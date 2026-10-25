package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.MessageVaultDatabase
import com.example.data.repository.MessageVaultRepository
import com.example.util.AppNotificationManager
import com.example.util.LockScreenPrivacy
import com.example.util.NotificationConfig
import com.example.util.NotificationPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationSystemRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: MessageVaultDatabase
    private lateinit var repository: MessageVaultRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MessageVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MessageVaultRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testDefaultNotificationPreferencesMasterSwitchIsOff() {
        val defaultConfig = NotificationConfig()
        assertFalse("Master notifications switch MUST be OFF by default", defaultConfig.areNotificationsEnabled)
        assertTrue(defaultConfig.notifyOnNewMessages)
        assertTrue(defaultConfig.notifyOnDeletedMessages)
        assertTrue(defaultConfig.isSoundEnabled)
        assertTrue(defaultConfig.isVibrationEnabled)
        assertEquals(LockScreenPrivacy.SENDER_ONLY, defaultConfig.lockScreenPrivacy)
    }

    @Test
    fun testAppFilterLogic() {
        val config = NotificationConfig(
            areNotificationsEnabled = true,
            notifyWhatsApp = true,
            notifyWhatsAppTwin = false,
            notifyTelegram = true,
            notifyMessenger = false
        )

        assertTrue(config.isAppAllowed("com.whatsapp", isClonedApp = false))
        assertFalse(config.isAppAllowed("com.whatsapp", isClonedApp = true))
        assertTrue(config.isAppAllowed("org.telegram.messenger", isClonedApp = false))
        assertFalse(config.isAppAllowed("com.facebook.orca", isClonedApp = false))
    }

    @Test
    fun testNotificationChannelsInitialization() {
        AppNotificationManager.initChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val newMsgChannel = nm.getNotificationChannel(AppNotificationManager.CHANNEL_NEW_MESSAGES)
        assertNotNull("New messages channel must be created", newMsgChannel)
        assertEquals("رسائل جديدة", newMsgChannel.name)

        val delMsgChannel = nm.getNotificationChannel(AppNotificationManager.CHANNEL_DELETED_MESSAGES)
        assertNotNull("Deleted messages channel must be created", delMsgChannel)
        assertEquals("رسائل محذوفة", delMsgChannel.name)
    }

    @Test
    fun testLockScreenPrivacyEnum() {
        assertEquals(LockScreenPrivacy.FULL_CONTENT, LockScreenPrivacy.fromId("FULL_CONTENT"))
        assertEquals(LockScreenPrivacy.SENDER_ONLY, LockScreenPrivacy.fromId("SENDER_ONLY"))
        assertEquals(LockScreenPrivacy.HIDE_DETAILS, LockScreenPrivacy.fromId("HIDE_DETAILS"))
        // Fallback for unknown id
        assertEquals(LockScreenPrivacy.SENDER_ONLY, LockScreenPrivacy.fromId("UNKNOWN"))
    }

    @Test
    fun testMessageSavingIsCompletelyIndependentOfNotifications() = runBlocking {
        // Even if notifications are disabled
        val prefs = NotificationPreferences.get(context)
        prefs.setNotificationsEnabled(false)

        val time = System.currentTimeMillis()
        val result = repository.saveIncomingNotification(
            packageName = "com.whatsapp",
            appDisplayName = "واتساب",
            chatTitle = "عمر فاروق",
            senderName = "عمر فاروق",
            messageText = "رسالة تجريبية للتأكد من استقلالية الحفظ",
            timestamp = time,
            isClonedApp = false,
            userHandleId = 0
        )

        val convs = repository.allConversations.first()
        assertEquals(1, convs.size)
        assertEquals("عمر فاروق", convs[0].chatTitle)

        val msgs = repository.getMessagesForConversation(convs[0].id).first()
        assertEquals(1, msgs.size)
        assertEquals("رسالة تجريبية للتأكد من استقلالية الحفظ", msgs[0].messageText)
    }
}
