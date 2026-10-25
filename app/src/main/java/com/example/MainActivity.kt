package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.fragment.app.FragmentActivity
import com.example.ml.TranslationManager
import com.example.security.SecurityManager
import com.example.ui.auth.AuthScreen
import com.example.ui.chat.ChatDetailScreen
import com.example.ui.home.HomeScreen
import com.example.ui.permissions.PermissionsScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.MessageVaultTheme
import com.example.ui.trash.RecycleBinScreen
import com.example.util.PreferenceManager

sealed class Screen {
    object Splash : Screen()
    object Auth : Screen()
    object Permissions : Screen()
    object Home : Screen()
    object Trash : Screen()
    data class ChatDetail(val conversationId: Long) : Screen()
}

class MainActivity : FragmentActivity() {

    private lateinit var translationManager: TranslationManager
    private lateinit var securityManager: SecurityManager
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        translationManager = TranslationManager(this)
        securityManager = SecurityManager(this)
        preferenceManager = PreferenceManager(this)

        setContent {
            MessageVaultTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppNavigation(
                        translationManager = translationManager,
                        securityManager = securityManager,
                        preferenceManager = preferenceManager
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        translationManager.close()
    }
}

@Composable
fun AppNavigation(
    translationManager: TranslationManager,
    securityManager: SecurityManager,
    preferenceManager: PreferenceManager
) {
    val repository = MessageVaultApp.get().repository
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    val isAppUnlocked by securityManager.isAppUnlocked.collectAsState()

    Crossfade(
        targetState = currentScreen,
        modifier = Modifier.fillMaxSize(),
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            is Screen.Splash -> {
                SplashScreen(
                    translationManager = translationManager,
                    preferenceManager = preferenceManager,
                    onSplashFinished = {
                        val isLocked = securityManager.isLockEnabled()
                        if (isLocked && !isAppUnlocked) {
                            currentScreen = Screen.Auth
                        } else if (!preferenceManager.isSetupCompleted() || !preferenceManager.isNotificationAccessGranted()) {
                            currentScreen = Screen.Permissions
                        } else {
                            currentScreen = Screen.Home
                        }
                    }
                )
            }

            is Screen.Auth -> {
                AuthScreen(
                    securityManager = securityManager,
                    onUnlocked = {
                        if (!preferenceManager.isSetupCompleted() || !preferenceManager.isNotificationAccessGranted()) {
                            currentScreen = Screen.Permissions
                        } else {
                            currentScreen = Screen.Home
                        }
                    }
                )
            }

            is Screen.Permissions -> {
                PermissionsScreen(
                    preferenceManager = preferenceManager,
                    onSetupCompleted = {
                        currentScreen = Screen.Home
                    }
                )
            }

            is Screen.Home -> {
                HomeScreen(
                    repository = repository,
                    translationManager = translationManager,
                    securityManager = securityManager,
                    preferenceManager = preferenceManager,
                    onConversationClick = { conversationId ->
                        currentScreen = Screen.ChatDetail(conversationId)
                    },
                    onNavigateToTrash = {
                        currentScreen = Screen.Trash
                    }
                )
            }

            is Screen.Trash -> {
                RecycleBinScreen(
                    repository = repository,
                    onBack = {
                        currentScreen = Screen.Home
                    }
                )
            }

            is Screen.ChatDetail -> {
                ChatDetailScreen(
                    conversationId = screen.conversationId,
                    repository = repository,
                    translationManager = translationManager,
                    onBack = {
                        currentScreen = Screen.Home
                    }
                )
            }
        }
    }
}
