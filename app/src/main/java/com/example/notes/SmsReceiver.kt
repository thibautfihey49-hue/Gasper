package com.example.notes
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlin.random.Random

class SmsReceiver : BroadcastReceiver() {
    private val quotes = listOf(
        "La vie est belle.",
        "Tout vient a point a qui sait attendre.",
        "L'espoir fait vivre.",
        "Le temps est un grand maitre.",
        "A coeur vaillant rien d impossible.",
        "La patience est la cle du bonheur.",
        "Qui ne tente rien n a rien.",
        "Chaque jour est une nouvelle chance.",
        "La simplicite est la sophistication.",
        "Rever c est le bonheur, esperer c est la vie."
    )
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.DATA_SMS_RECEIVED") return
        val bundle = intent.extras ?: return
        val format = bundle.getString("format")
        val pdus = bundle.get("pdus") as? Array<*> ?: return
        var fullMessage = ""
        var sender = ""
        for (pdu in pdus) {
            val msg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                SmsMessage.createFromPdu(pdu as ByteArray, format)
            } else {
                SmsMessage.createFromPdu(pdu as ByteArray)
            }
            sender = msg.originatingAddress ?: sender
            val data = msg.userData
            if (data != null) {
                fullMessage += String(data, Charsets.UTF_8)
            }
        }
        if (fullMessage.isEmpty()) return
        try { abortBroadcast() } catch (_: Exception) {}
        val local = Intent("NEW_DATA_SMS")
        local.putExtra("from", sender)
        local.putExtra("body", fullMessage)
        LocalBroadcastManager.getInstance(context).sendBroadcast(local)
        val prefs = context.getSharedPreferences("notes_prefs", Context.MODE_PRIVATE)
        val old = prefs.getString("conversation_history", "") ?: ""
        val newHist = if (old.isEmpty()) "[$sender]: $fullMessage" else "$old\n\n[$sender]: $fullMessage"
        prefs.edit().putString("conversation_history", newHist).apply()
        val channelId = "notes_quotes"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "Notes", NotificationManager.IMPORTANCE_LOW)
            ch.setSound(null, null)
            ch.enableVibration(false)
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
        val quote = quotes[Random.nextInt(quotes.size)]
        val openIntent = Intent(context, MainActivity::class.java)
        openIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pending = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Notes")
            .setContentText(quote)
            .setStyle(NotificationCompat.BigTextStyle().bigText(quote))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setSound(null)
            .setVibrate(null)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        nm.notify(Random.nextInt(99999), notif)
    }
}
