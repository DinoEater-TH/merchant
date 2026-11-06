package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

/**
 * Local notifications for new orders. These work while the app process is alive
 * (including when the UI is in the background). For delivery when the app is
 * fully closed, FirebaseMessagingService below receives the push from the
 * Cloud Function trigger and shows the same style of notification.
 */
object NotificationHelper {

    const val CHANNEL_ID = "dinoeater_new_orders"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "ออเดอร์ใหม่",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "แจ้งเตือนเมื่อมีออเดอร์ใหม่เข้ามาในร้าน"
                    enableVibration(true)
                    setSound(sound, android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .build())
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showNewOrderNotification(context: Context, orderId: String, totalAmount: Double?) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("order_id", orderId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val amountText = if (totalAmount != null) " ยอด ฿${totalAmount.toLong()}" else ""
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔔 มีออเดอร์ใหม่ $orderId")
            .setContentText("ลูกค้าสั่งอาหารเข้ามา$amountText กดเพื่อเปิดดูออเดอร์")
            .setStyle(NotificationCompat.BigTextStyle().bigText("มีออเดอร์ใหม่ $orderId$amountText กดเพื่อเปิดดูและจัดการออเดอร์"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted yet; the in-app sound still alerts the admin.
        }
    }
}
