package com.example.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.Order
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONObject

/**
 * Foreground service that keeps the app alive in the background so new customer
 * orders are detected and announced (sound + vibration + notification) even when
 * the main Activity is not in the foreground.
 */
class OrderAlertService : Service() {

    companion object {
        const val CHANNEL_ID = "dinoeater_order_alert_service"
        const val NOTIF_ID = 2001
        const val ACTION_START = "com.example.action.START_ORDER_ALERT"
        const val ACTION_STOP = "com.example.action.STOP_ORDER_ALERT"

        fun start(context: Context) {
            val intent = Intent(context, OrderAlertService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OrderAlertService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }

    private var knownOrderIds: Set<String>? = null
    private var listener: ValueEventListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIF_ID, buildNotification("กำลังฟังออเดอร์ใหม่..."))
                attachListener()
            }
        }
        return START_STICKY
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(CHANNEL_ID, "Order Alert Service", NotificationManager.IMPORTANCE_LOW).apply {
                    description = " keeps the app listening for new orders"
                    setShowBadge(false)
                }
                mgr.createNotificationChannel(ch)
            }
        }
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("DinoEater ร้านค้า")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun attachListener() {
        if (listener != null) return
        val ref = FirebaseDatabase.getInstance().reference.child("shops/dinoeater-merchant/state")
        val vl = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val raw = snapshot.value
                val parsed = parseState(raw) ?: return
                val orders = parsed.optJSONArray("orders") ?: return
                val currentIds = (0 until orders.length()).map { orders.getJSONObject(it).optString("id") }.toSet()
                val known = knownOrderIds
                if (known == null) {
                    knownOrderIds = currentIds
                    return
                }
                val incoming = (0 until orders.length()).map { orders.getJSONObject(it) }.filter { o ->
                    val id = o.optString("id")
                    id !in known && o.optString("status") == "PENDING" &&
                        !o.optString("comment").trim().uppercase().startsWith("POS")
                }
                knownOrderIds = currentIds
                if (incoming.isNotEmpty()) {
                    SoundHelper.playNewOrderAlert(applicationContext)
                    val latest = incoming.last()
                    NotificationHelper.showNewOrderNotification(
                        applicationContext,
                        latest.optString("id", "ออเดอร์ใหม่"),
                        latest.optDouble("totalAmount", Double.NaN).let { if (it.isNaN()) null else it }
                    )
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        listener = vl
        ref.addValueEventListener(vl)
    }

    private fun parseState(raw: Any?): JSONObject? {
        if (raw == null) return null
        return try {
            if (raw is String) JSONObject(raw) else JSONObject(raw.toString())
        } catch (_: Exception) {
            null
        }
    }

    override fun onDestroy() {
        listener?.let {
            try { FirebaseDatabase.getInstance().reference.child("shops/dinoeater-merchant/state").removeEventListener(it) } catch (_: Exception) {}
        }
        listener = null
        super.onDestroy()
    }
}