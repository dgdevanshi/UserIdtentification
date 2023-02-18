package com.example.useridten

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class NotificationService : NotificationListenerService() {

    private val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    class ApplicationPackageNames {
        companion object {
            const val FACEBOOK_PACK_NAME = "com.facebook.katana"
            const val FACEBOOK_MESSENGER_PACK_NAME = "com.facebook.orca"
            const val WHATSAPP_PACK_NAME = "com.whatsapp"
            const val INSTAGRAM_PACK_NAME = "com.instagram.android"
        }
    }

    class InterceptedNotificationCode {
        companion object {
            const val FACEBOOK_CODE: Int = 1
            const val WHATSAPP_CODE: Int = 2
            const val INSTAGRAM_CODE: Int = 3
            const val OTHER_NOTIFICATION_CODE: Int = 4
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

//    override fun onNotificationPosted(sbn: StatusBarNotification?) {
//        val notificationCode = sbn?.let { matchNotificationCode(it) }
//
//        if (notificationCode != InterceptedNotificationCode.OTHER_NOTIFICATION_CODE) {
//            val intent = Intent("com.example.userIdten")
//            intent.putExtra("Notification Code", notificationCode)
//            sendBroadcast(intent)
//        }
//    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notificationCode: Int = matchNotificationCode(sbn)
        val pack = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title")
        val text = extras.getCharSequence("android.text").toString()
        var subtext = ""

        if (notificationCode != InterceptedNotificationCode.OTHER_NOTIFICATION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val b = extras[Notification.EXTRA_MESSAGES] as Array<Parcelable>?

                if (b != null) {
                    for (tmp in b) {
                        val msgBundle = tmp as Bundle
                        subtext = msgBundle.getString("text")!!
                    }
                    Log.d("Detail Error1: ", subtext)
                }
                if (subtext.isEmpty()) {
                    subtext = text;
                }
                Log.d("Detail Error2: ", subtext)

                val intent = Intent("com.example.useridten")
                intent.putExtra("Notification Code", notificationCode)
                intent.putExtra("package", pack)
                intent.putExtra("title", title)
                intent.putExtra("text", subtext)
                intent.putExtra("id", sbn.id)

                sendBroadcast(intent)

                if (text != null) {
                    if (!text.contains("new messages") &&
                        !text.contains("WhatsApp Web is currently active") &&
                        !text.contains("WhatsApp Web login")) {

                        val androidId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
                        val deviceModel = android.os.Build.MANUFACTURER + android.os.Build.MODEL + android.os.Build.BRAND + android.os.Build.SERIAL

                        val df: DateFormat = SimpleDateFormat("ddMMyyyyHHmmssSSS")
                        val date: String = df.format(Calendar.getInstance().time)

                        val intentPending = Intent(applicationContext, MainActivity::class.java)
                        val pendingIntent = PendingIntent.getActivity(this, 0, intentPending, 0)

                        val builder = NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_launcher_background)
                            .setContentTitle("UserIdten")
                            .setContentText("This is an incoming message: $text")
                        builder.setWhen(System.currentTimeMillis())
                        builder.setSmallIcon(R.mipmap.ic_launcher)
                        val largeIconBitmap : Bitmap = BitmapFactory.decodeResource(resources, R.drawable.notification_logo)
                        builder.setLargeIcon(largeIconBitmap)
                        builder.setPriority(Notification.PRIORITY_MAX)
                        builder.setFullScreenIntent(pendingIntent, true)

                        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(1, builder.build())
                    }
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val notificationCode = sbn?.let { matchNotificationCode(it) }
        if (notificationCode != InterceptedNotificationCode.OTHER_NOTIFICATION_CODE) {
            val activeNotifications: Array<StatusBarNotification> = notificationManager!!.activeNotifications
            if (activeNotifications != null && activeNotifications.isNotEmpty()) {
                for (i in activeNotifications.indices) {
                    if (notificationCode == matchNotificationCode(activeNotifications[i])) {
                        val intent = Intent("com.example.useridten")
                        intent.putExtra("Notification Code", notificationCode)
                        sendBroadcast(intent)
                        break
                    }
                }
            }
        }
    }

    private fun matchNotificationCode(sbn: StatusBarNotification): Int {
        val packageName = sbn.packageName

        if (packageName.equals(ApplicationPackageNames.FACEBOOK_PACK_NAME)
            || packageName.equals(ApplicationPackageNames.FACEBOOK_MESSENGER_PACK_NAME)) {
            return (InterceptedNotificationCode.FACEBOOK_CODE)
        }
        else if (packageName.equals(ApplicationPackageNames.INSTAGRAM_PACK_NAME)) {
            return (InterceptedNotificationCode.INSTAGRAM_CODE)
        }
        else if (packageName.equals(ApplicationPackageNames.WHATSAPP_PACK_NAME)) {
            return (InterceptedNotificationCode.WHATSAPP_CODE)
        }
        else {
            return (InterceptedNotificationCode.OTHER_NOTIFICATION_CODE)
        }
    }
}