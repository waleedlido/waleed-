package com.example.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_settings")

enum class LockScreenPrivacy(val id: String, val title: String, val description: String) {
    FULL_CONTENT(
        id = "FULL_CONTENT",
        title = "إظهار اسم المرسل ومحتوى الرسالة",
        description = "عرض التفاصيل كاملة على شاشة القفل"
    ),
    SENDER_ONLY(
        id = "SENDER_ONLY",
        title = "إظهار اسم المرسل فقط",
        description = "عرض اسم المرسل مع إخفاء نص الرسالة"
    ),
    HIDE_DETAILS(
        id = "HIDE_DETAILS",
        title = "إخفاء التفاصيل الحساسة",
        description = "عرض «لديك رسالة محفوظة جديدة» فقط"
    );

    companion object {
        fun fromId(id: String): LockScreenPrivacy {
            return entries.find { it.id == id } ?: SENDER_ONLY
        }
    }
}

data class NotificationConfig(
    val areNotificationsEnabled: Boolean = false, // Master switch - OFF by default
    val notifyOnNewMessages: Boolean = true,
    val notifyOnDeletedMessages: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val lockScreenPrivacy: LockScreenPrivacy = LockScreenPrivacy.SENDER_ONLY,
    val notifyWhatsApp: Boolean = true,
    val notifyWhatsAppTwin: Boolean = true,
    val notifyTelegram: Boolean = true,
    val notifyMessenger: Boolean = true,
    val notifySignal: Boolean = true,
    val notifySms: Boolean = true
) {
    fun isAppAllowed(packageName: String, isClonedApp: Boolean): Boolean {
        val lower = packageName.lowercase()
        return when {
            lower.contains("whatsapp") && isClonedApp -> notifyWhatsAppTwin
            lower.contains("whatsapp") -> notifyWhatsApp
            lower.contains("telegram") -> notifyTelegram
            lower.contains("orca") || lower.contains("messenger") -> notifyMessenger
            lower.contains("signal") || lower.contains("securesms") -> notifySignal
            lower.contains("mms") || lower.contains("messaging") -> notifySms
            else -> true
        }
    }
}

class NotificationPreferences(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val configFlow: Flow<NotificationConfig> = context.notificationDataStore.data.map { prefs ->
        NotificationConfig(
            areNotificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: false,
            notifyOnNewMessages = prefs[KEY_NOTIFY_NEW_MESSAGES] ?: true,
            notifyOnDeletedMessages = prefs[KEY_NOTIFY_DELETED_MESSAGES] ?: true,
            isSoundEnabled = prefs[KEY_SOUND_ENABLED] ?: true,
            isVibrationEnabled = prefs[KEY_VIBRATION_ENABLED] ?: true,
            lockScreenPrivacy = LockScreenPrivacy.fromId(prefs[KEY_LOCK_SCREEN_PRIVACY] ?: LockScreenPrivacy.SENDER_ONLY.id),
            notifyWhatsApp = prefs[KEY_APP_WHATSAPP] ?: true,
            notifyWhatsAppTwin = prefs[KEY_APP_WHATSAPP_TWIN] ?: true,
            notifyTelegram = prefs[KEY_APP_TELEGRAM] ?: true,
            notifyMessenger = prefs[KEY_APP_MESSENGER] ?: true,
            notifySignal = prefs[KEY_APP_SIGNAL] ?: true,
            notifySms = prefs[KEY_APP_SMS] ?: true
        )
    }

    val stateFlow: StateFlow<NotificationConfig> = configFlow.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = NotificationConfig()
    )

    fun setNotificationsEnabled(enabled: Boolean) {
        scope.launch {
            context.notificationDataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
        }
    }

    fun setNotifyOnNewMessages(enabled: Boolean) {
        scope.launch {
            context.notificationDataStore.edit { it[KEY_NOTIFY_NEW_MESSAGES] = enabled }
        }
    }

    fun setNotifyOnDeletedMessages(enabled: Boolean) {
        scope.launch {
            context.notificationDataStore.edit { it[KEY_NOTIFY_DELETED_MESSAGES] = enabled }
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        scope.launch {
            context.notificationDataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        scope.launch {
            context.notificationDataStore.edit { it[KEY_VIBRATION_ENABLED] = enabled }
        }
    }

    fun setLockScreenPrivacy(privacy: LockScreenPrivacy) {
        scope.launch {
            context.notificationDataStore.edit { it[KEY_LOCK_SCREEN_PRIVACY] = privacy.id }
        }
    }

    fun setAppFilter(appKey: String, enabled: Boolean) {
        scope.launch {
            context.notificationDataStore.edit { prefs ->
                when (appKey) {
                    "whatsapp" -> prefs[KEY_APP_WHATSAPP] = enabled
                    "whatsapp_twin" -> prefs[KEY_APP_WHATSAPP_TWIN] = enabled
                    "telegram" -> prefs[KEY_APP_TELEGRAM] = enabled
                    "messenger" -> prefs[KEY_APP_MESSENGER] = enabled
                    "signal" -> prefs[KEY_APP_SIGNAL] = enabled
                    "sms" -> prefs[KEY_APP_SMS] = enabled
                }
            }
        }
    }

    companion object {
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notif_enabled")
        private val KEY_NOTIFY_NEW_MESSAGES = booleanPreferencesKey("notif_new_messages")
        private val KEY_NOTIFY_DELETED_MESSAGES = booleanPreferencesKey("notif_deleted_messages")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("notif_sound")
        private val KEY_VIBRATION_ENABLED = booleanPreferencesKey("notif_vibration")
        private val KEY_LOCK_SCREEN_PRIVACY = stringPreferencesKey("notif_lock_privacy")
        private val KEY_APP_WHATSAPP = booleanPreferencesKey("notif_app_whatsapp")
        private val KEY_APP_WHATSAPP_TWIN = booleanPreferencesKey("notif_app_whatsapp_twin")
        private val KEY_APP_TELEGRAM = booleanPreferencesKey("notif_app_telegram")
        private val KEY_APP_MESSENGER = booleanPreferencesKey("notif_app_messenger")
        private val KEY_APP_SIGNAL = booleanPreferencesKey("notif_app_signal")
        private val KEY_APP_SMS = booleanPreferencesKey("notif_app_sms")

        @Volatile
        private var instance: NotificationPreferences? = null

        fun get(context: Context): NotificationPreferences {
            return instance ?: synchronized(this) {
                instance ?: NotificationPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
