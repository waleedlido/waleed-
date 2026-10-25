package com.example.util

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

val Context.privacyDataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_preferences")

data class PrivacyBlurConfig(
    val isMasterBlurEnabled: Boolean = false, // OFF by default
    val blurHomePreviews: Boolean = true,
    val blurChatMessages: Boolean = true,
    val blurSenderNames: Boolean = false,
    val isScreenSecurityEnabled: Boolean = false // FLAG_SECURE for Recent Apps & Screenshots
) {
    val isMasterBlurActive: Boolean get() = isMasterBlurEnabled
    val preventScreenshots: Boolean get() = isScreenSecurityEnabled
}

class PrivacyBlurManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val configFlow: Flow<PrivacyBlurConfig> = context.privacyDataStore.data.map { prefs ->
        PrivacyBlurConfig(
            isMasterBlurEnabled = prefs[KEY_MASTER_BLUR] ?: false,
            blurHomePreviews = prefs[KEY_BLUR_HOME] ?: true,
            blurChatMessages = prefs[KEY_BLUR_CHAT] ?: true,
            blurSenderNames = prefs[KEY_BLUR_SENDER] ?: false,
            isScreenSecurityEnabled = prefs[KEY_SCREEN_SECURITY] ?: false
        )
    }

    val stateFlow: StateFlow<PrivacyBlurConfig> = configFlow.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = PrivacyBlurConfig()
    )

    fun setMasterBlurEnabled(enabled: Boolean) {
        scope.launch {
            context.privacyDataStore.edit { it[KEY_MASTER_BLUR] = enabled }
        }
    }

    fun setMasterBlur(enabled: Boolean) = setMasterBlurEnabled(enabled)

    fun setBlurHomePreviews(enabled: Boolean) {
        scope.launch {
            context.privacyDataStore.edit { it[KEY_BLUR_HOME] = enabled }
        }
    }

    fun setBlurChatMessages(enabled: Boolean) {
        scope.launch {
            context.privacyDataStore.edit { it[KEY_BLUR_CHAT] = enabled }
        }
    }

    fun setBlurSenderNames(enabled: Boolean) {
        scope.launch {
            context.privacyDataStore.edit { it[KEY_BLUR_SENDER] = enabled }
        }
    }

    fun setScreenSecurityEnabled(enabled: Boolean) {
        scope.launch {
            context.privacyDataStore.edit { it[KEY_SCREEN_SECURITY] = enabled }
        }
    }

    fun setPreventScreenshots(enabled: Boolean) = setScreenSecurityEnabled(enabled)

    fun applyScreenSecurityToActivity(activity: Activity?, enabled: Boolean) {
        if (activity == null || activity.isFinishing) return
        activity.runOnUiThread {
            if (enabled) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    companion object {
        private val KEY_MASTER_BLUR = booleanPreferencesKey("privacy_master_blur")
        private val KEY_BLUR_HOME = booleanPreferencesKey("privacy_blur_home")
        private val KEY_BLUR_CHAT = booleanPreferencesKey("privacy_blur_chat")
        private val KEY_BLUR_SENDER = booleanPreferencesKey("privacy_blur_sender")
        private val KEY_SCREEN_SECURITY = booleanPreferencesKey("privacy_screen_security")

        @Volatile
        private var instance: PrivacyBlurManager? = null

        fun get(context: Context): PrivacyBlurManager {
            return instance ?: synchronized(this) {
                instance ?: PrivacyBlurManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
