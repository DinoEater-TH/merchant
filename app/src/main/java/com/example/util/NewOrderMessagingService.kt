package com.example.util

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives Firebase Cloud Messaging pushes (sent by the Cloud Function when a
 * new order is written to Firebase). Because the payload is delivered as a
 * data message, this service runs even when the app is in the background or
 * has been closed (as long as the OS has not force-stopped it), which is what
 * makes the alert work "even when the app is closed".
 *
 * The device token is registered under
 * shops/dinoeater-merchant/fcmTokens/<token> so the Cloud Function can target
 * only admin devices.
 */
class NewOrderMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        registerToken(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val orderId = message.data["orderId"] ?: message.notification?.title ?: "ออเดอร์ใหม่"
        val total = message.data["totalAmount"]?.toDoubleOrNull()
        NotificationHelper.showNewOrderNotification(this, orderId, total)
        SoundHelper.playNewOrderAlert(this)
    }

    companion object {
        private const val DATABASE_URL =
            "https://dinoeater-merchant-default-rtdb.asia-southeast1.firebasedatabase.app"
        private const val ROOT_PATH = "shops/dinoeater-merchant"

        fun registerToken(context: Context, token: String) {
            val prefs = context.getSharedPreferences("fcm_tokens", Context.MODE_PRIVATE)
            prefs.edit().putString("admin_fcm_token", token).apply()

            val auth = FirebaseAuth.getInstance()
            val write = {
                val ref = FirebaseDatabase.getInstance(DATABASE_URL)
                    .getReference("$ROOT_PATH/fcmTokens/$token")
                ref.setValue(
                    mapOf(
                        "token" to token,
                        "platform" to "android",
                        "admin" to true,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
            }
            if (auth.currentUser != null) {
                write()
            } else {
                auth.signInAnonymously().addOnCompleteListener { write() }
            }
        }
    }
}
