const { onValueWritten } = require("firebase-functions/v2/database");
const admin = require("firebase-admin");

admin.initializeApp();

const ROOT = "shops/dinoeater-merchant";

function isPosOrder(order) {
  return ((order && order.comment) || "").trim().toUpperCase().startsWith("POS");
}

/**
 * Sends a push notification to every registered admin device whenever a new
 * online (non-POS) order appears. The shop state is stored as one JSON blob at
 * ROOT/state, so this listens to that node and diffs the orders array.
 * Admin devices register their FCM token at ROOT/fcmTokens/<token> when they
 * enter admin mode, so alerts arrive even when the app / browser tab is closed.
 */
exports.onNewOrderPush = onValueWritten(
  { ref: `${ROOT}/state` },
  async (event) => {
    const parse = (raw) => {
      if (!raw) return null;
      try {
        return typeof raw === "string" ? JSON.parse(raw) : raw;
      } catch (_) {
        return null;
      }
    };
    const after = parse(event.data.after.val());
    const before = parse(event.data.before.val());
    if (!after || !Array.isArray(after.orders)) return;

    const beforeIds = new Set(((before && before.orders) || []).map((o) => o && o.id));
    const fresh = after.orders.filter(
      (o) => o && o.id && !beforeIds.has(o.id) && o.status === "PENDING" && !isPosOrder(o)
    );
    if (fresh.length === 0) return;

    const tokensSnap = await admin.database().ref(`${ROOT}/fcmTokens`).once("value");
    const tokensObj = tokensSnap.val() || {};
    const tokens = Object.keys(tokensObj).filter((t) => t && t.length > 20);
    if (tokens.length === 0) return;

    const latest = fresh[fresh.length - 1];
    const orderId = latest.id;
    const totalAmount = latest.totalAmount != null ? String(latest.totalAmount) : "";

    const message = {
      data: {
        orderId: String(orderId),
        totalAmount,
        type: "NEW_ORDER",
      },
      notification: {
        title: `🔔 มีออเดอร์ใหม่ ${orderId}`,
        body: totalAmount ? `ยอด ฿${totalAmount} กดเพื่อเปิดดูออเดอร์` : "กดเพื่อเปิดดูออเดอร์",
      },
      android: {
        priority: "high",
        notification: { channelId: "dinoeater_new_orders" },
      },
      webpush: {
        notification: { icon: "https://dinoeater-merchant.web.app/icon-192.png" },
      },
      tokens,
    };

    const response = await admin.messaging().sendEach(message);
    const stale = [];
    response.responses.forEach((result, index) => {
      if (!result.success) {
        const code = result.error && result.error.code;
        if (code === "messaging/invalid-registration-token" || code === "messaging/registration-token-not-registered") {
          stale.push(tokens[index]);
        }
      }
    });
    await Promise.all(stale.map((t) => admin.database().ref(`${ROOT}/fcmTokens/${t}`).remove()));
  }
);
