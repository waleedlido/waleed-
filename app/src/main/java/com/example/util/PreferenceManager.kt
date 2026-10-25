package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

class PreferenceManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isSetupCompleted(): Boolean {
        return prefs.getBoolean(KEY_SETUP_COMPLETED, false)
    }

    fun setSetupCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETED, completed).apply()
    }

    fun isNotificationAccessGranted(): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        if (enabledPackages.contains(context.packageName)) {
            return true
        }

        // Alternative check via secure settings
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (flat != null && flat.contains(context.packageName)) {
            return true
        }

        return false
    }

    fun isAutoRestoreOnNewMessage(): Boolean {
        return prefs.getBoolean(KEY_AUTO_RESTORE_ON_NEW_MESSAGE, false)
    }

    fun setAutoRestoreOnNewMessage(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RESTORE_ON_NEW_MESSAGE, enabled).apply()
    }

    companion object {
        private const val PREF_NAME = "message_vault_app_prefs"
        private const val KEY_SETUP_COMPLETED = "key_setup_completed"
        private const val KEY_AUTO_RESTORE_ON_NEW_MESSAGE = "key_auto_restore_on_new_message"
    }
}
