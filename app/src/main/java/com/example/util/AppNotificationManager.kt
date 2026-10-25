package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.util.Collections
import java.util.LinkedHashMap

object AppNotificationManager {

    const val EXTRA_CONVERSATION_ID = "extra_target_conversation_id"

    const val CHANNEL_NEW_MESSAGES = "channel_vault_new_messages"
    const val CHANNEL_DELETED_MESSAGES = "channel_vault_deleted_messages"

    // Deduplication cache to prevent re-notifying for the same message (keeps last 200 keys)
    private val recentlyNotifiedKeys = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(200, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > 200
            }
        }
    )

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // Channel 1: New Messages
            val newMessagesChannel = NotificationChannel(
                CHANNEL_NEW_MESSAGES,
                "رسائل جديدة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات وصول رسائل جديدة وتوثيقها فورياً"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // Channel 2: Deleted Messages
            val deletedMessagesChannel = NotificationChannel(
                CHANNEL_DELETED_MESSAGES,
                "رسائل محذوفة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات رصد قيام الطرف الآخر بحذف الرسائل"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(newMessagesChannel)
            notificationManager.createNotificationChannel(deletedMessagesChannel)
        }
    }

    fun showNewMessageNotification(
        context: Context,
        conversationId: Long,
        senderName: String,
        messageText: String,
        packageName: String,
        isClonedApp: Boolean,
        timestamp: Long
    ) {
        val prefs = NotificationPreferences.get(context)
        val config = prefs.stateFlow.value

        // Check master switch and specific feature switch
        if (!config.areNotificationsEnabled || !config.notifyOnNewMessages) {
            return
        }

        // Check app filter
        if (!config.isAppAllowed(packageName, isClonedApp)) {
            return
        }

        // Deduplication check: prevent notifying for same message or past messages on startup
        val dedupeKey = "$conversationId|$messageText|$timestamp"
        if (recentlyNotifiedKeys.containsKey(dedupeKey)) {
            return
        }
        // Don't notify for messages older than 2 minutes
        if (System.currentTimeMillis() - timestamp > 120_000) {
            return
        }
        recentlyNotifiedKeys[dedupeKey] = System.currentTimeMillis()

        initChannels(context)

        val appLabel = AppIconProvider.getAppLabel(context, packageName, isClonedApp)
        val notificationId = (conversationId.hashCode() xor 0x1234).toInt()

        // PendingIntent to launch directly into the conversation
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_CONVERSATION_ID, conversationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeFormatted = DateTimeUtils.formatShortTime(timestamp)
        val title = "$senderName ($appLabel)"

        // Public lock screen version depending on privacy setting
        val publicNotification = when (config.lockScreenPrivacy) {
            LockScreenPrivacy.FULL_CONTENT -> {
                NotificationCompat.Builder(context, CHANNEL_NEW_MESSAGES)
                    .setSmallIcon(R.drawable.ic_brand_generic)
                    .setContentTitle(title)
                    .setContentText(messageText)
                    .setSubText("الرسائل المحذوفة")
                    .build()
            }
            LockScreenPrivacy.SENDER_ONLY -> {
                NotificationCompat.Builder(context, CHANNEL_NEW_MESSAGES)
                    .setSmallIcon(R.drawable.ic_brand_generic)
                    .setContentTitle(title)
                    .setContentText("رسالة جديدة محفوظة")
                    .setSubText("الرسائل المحذوفة")
                    .build()
            }
            LockScreenPrivacy.HIDE_DETAILS -> {
                NotificationCompat.Builder(context, CHANNEL_NEW_MESSAGES)
                    .setSmallIcon(R.drawable.ic_brand_generic)
                    .setContentTitle("الرسائل المحذوفة")
                    .setContentText("لديك رسالة محفوظة جديدة")
                    .build()
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_NEW_MESSAGES)
            .setSmallIcon(R.drawable.ic_brand_generic)
            .setContentTitle(title)
            .setContentText(messageText)
            .setSubText("الرسائل المحذوفة • $timeFormatted")
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setWhen(timestamp)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setPublicVersion(publicNotification)
            .setVisibility(
                if (config.lockScreenPrivacy == LockScreenPrivacy.FULL_CONTENT)
                    NotificationCompat.VISIBILITY_PUBLIC
                else
                    NotificationCompat.VISIBILITY_PRIVATE
            )

        if (config.isSoundEnabled) {
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(defaultSound)
        } else {
            builder.setSilent(true)
        }

        if (config.isVibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 250, 150, 250))
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted
        } catch (e: Exception) {
            // Ignore other exceptions
        }
    }

    fun showDeletedMessageNotification(
        context: Context,
        conversationId: Long,
        senderName: String,
        messageText: String,
        packageName: String,
        isClonedApp: Boolean,
        deletedDetectedAt: Long
    ) {
        val prefs = NotificationPreferences.get(context)
        val config = prefs.stateFlow.value

        if (!config.areNotificationsEnabled || !config.notifyOnDeletedMessages) {
            return
        }

        if (!config.isAppAllowed(packageName, isClonedApp)) {
            return
        }

        val dedupeKey = "del|$conversationId|$messageText"
        if (recentlyNotifiedKeys.containsKey(dedupeKey)) {
            return
        }
        recentlyNotifiedKeys[dedupeKey] = System.currentTimeMillis()

        initChannels(context)

        val appLabel = AppIconProvider.getAppLabel(context, packageName, isClonedApp)
        val notificationId = (conversationId.hashCode() xor 0x5678).toInt()

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_CONVERSATION_ID, conversationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "⚠️ حُذفت رسالة من $senderName ($appLabel)"
        val content = "تم حذف رسالة: \"${messageText.take(50)}\" (النص الأصلي محفوظ ومحمي 🔒)"

        val publicNotification = NotificationCompat.Builder(context, CHANNEL_DELETED_MESSAGES)
            .setSmallIcon(R.drawable.ic_brand_generic)
            .setContentTitle(if (config.lockScreenPrivacy == LockScreenPrivacy.HIDE_DETAILS) "الرسائل المحذوفة" else "⚠️ رصد حذف رسالة من $senderName")
            .setContentText(if (config.lockScreenPrivacy == LockScreenPrivacy.HIDE_DETAILS) "تم رصد حذف رسالة وتوثيقها" else "قام الطرف الآخر بحذف رسالة (محفوظة)")
            .build()

        val builder = NotificationCompat.Builder(context, CHANNEL_DELETED_MESSAGES)
            .setSmallIcon(R.drawable.ic_brand_generic)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText("قام الطرف الآخر بحذف الرسالة التالية، وتم حمايتها في السجل:\n\n\"$messageText\""))
            .setSubText("رصد حذف رسالة 🔒")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setWhen(deletedDetectedAt)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setPublicVersion(publicNotification)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)

        if (config.isSoundEnabled) {
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(defaultSound)
        } else {
            builder.setSilent(true)
        }

        if (config.isVibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 300, 200, 300))
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(notificationId, builder.build())
            }
        } catch (e: Exception) {
            // Handled
        }
    }

    fun sendTestNotification(context: Context) {
        val prefs = NotificationPreferences.get(context)
        val config = prefs.stateFlow.value

        initChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val publicNotification = NotificationCompat.Builder(context, CHANNEL_NEW_MESSAGES)
            .setSmallIcon(R.drawable.ic_brand_generic)
            .setContentTitle("الرسائل المحذوفة")
            .setContentText(
                when (config.lockScreenPrivacy) {
                    LockScreenPrivacy.FULL_CONTENT -> "إشعار تجريبي: تم حفظ رسالة تجريبية بنجاح"
                    LockScreenPrivacy.SENDER_ONLY -> "إشعار تجريبي من أحمد خالد"
                    LockScreenPrivacy.HIDE_DETAILS -> "لديك رسالة محفوظة جديدة"
                }
            )
            .build()

        val builder = NotificationCompat.Builder(context, CHANNEL_NEW_MESSAGES)
            .setSmallIcon(R.drawable.ic_brand_generic)
            .setContentTitle("أحمد خالد (واتساب)")
            .setContentText("إشعار تجريبي: هذا مثال لتنبيه وصول رسالة جديدة وتوثيقها!")
            .setSubText("الرسائل المحذوفة • تجربة")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setPublicVersion(publicNotification)
            .setVisibility(
                if (config.lockScreenPrivacy == LockScreenPrivacy.FULL_CONTENT)
                    NotificationCompat.VISIBILITY_PUBLIC
                else
                    NotificationCompat.VISIBILITY_PRIVATE
            )

        if (config.isSoundEnabled) {
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(defaultSound)
        } else {
            builder.setSilent(true)
        }

        if (config.isVibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 250, 150, 250))
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(9999, builder.build())
        } catch (e: Exception) {
            // Handled
        }
    }

    fun areSystemNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun openSystemNotificationSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent("android.settings.APP_NOTIFICATION_SETTINGS").apply {
                    putExtra("app_package", context.packageName)
                    putExtra("app_uid", context.applicationInfo.uid)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to app details
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }
}
