package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.MessageVaultDatabase
import com.example.data.repository.MessageVaultRepository
import com.example.util.AppIconProvider
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppIconAndDateTimeTest {

    private lateinit var database: MessageVaultDatabase
    private lateinit var repository: MessageVaultRepository
    private lateinit var context: Context

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
    fun testDateTimeUtilsNeverReturnsNow() {
        val now = System.currentTimeMillis()
        val formatted = DateTimeUtils.formatConversationTimestamp(now)
        assertFalse("Timestamp formatting must never return 'الآن'", formatted.contains("الآن"))
        assertTrue("Timestamp should contain Arabic AM/PM (ص or م)", formatted.contains("ص") || formatted.contains("م"))
    }

    @Test
    fun testDateTimeUtilsDateHeaders() {
        val now = System.currentTimeMillis()
        assertEquals("اليوم", DateTimeUtils.formatDateHeader(now))

        val calYesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        assertEquals("أمس", DateTimeUtils.formatDateHeader(calYesterday.timeInMillis))
    }

    @Test
    fun testAppIconProviderFallbackDrawables() {
        assertEquals(R.drawable.ic_brand_whatsapp, AppIconProvider.getFallbackIconRes("com.whatsapp"))
        assertEquals(R.drawable.ic_brand_whatsapp_business, AppIconProvider.getFallbackIconRes("com.whatsapp.w4b"))
        assertEquals(R.drawable.ic_brand_telegram, AppIconProvider.getFallbackIconRes("org.telegram.messenger"))
        assertEquals(R.drawable.ic_brand_messenger, AppIconProvider.getFallbackIconRes("com.facebook.orca"))
        assertEquals(R.drawable.ic_brand_signal, AppIconProvider.getFallbackIconRes("org.thoughtcrime.securesms"))
        assertEquals(R.drawable.ic_brand_sms, AppIconProvider.getFallbackIconRes("com.google.android.apps.messaging"))
        assertEquals(R.drawable.ic_brand_sms, AppIconProvider.getFallbackIconRes("com.android.mms"))
        assertEquals(R.drawable.ic_brand_generic, AppIconProvider.getFallbackIconRes("com.other.app"))
    }

    @Test
    fun testAppIconProviderLabels() {
        val waLabel = AppIconProvider.getAppLabel(context, "com.whatsapp", false)
        assertEquals("واتساب", waLabel)

        val waClonedLabel = AppIconProvider.getAppLabel(context, "com.whatsapp", true)
        assertTrue(waClonedLabel.contains("مستنسخ"))

        val tgLabel = AppIconProvider.getAppLabel(context, "org.telegram.messenger", false)
        assertEquals("تيليجرام", tgLabel)

        val signalLabel = AppIconProvider.getAppLabel(context, "org.thoughtcrime.securesms", false)
        assertEquals("سيجنال", signalLabel)
    }

    @Test
    fun testOriginalTimestampPreservedOnDeletion() = runBlocking {
        val originalTime = 1700000000000L
        repository.saveIncomingNotification(
            packageName = "com.whatsapp",
            appDisplayName = "واتساب",
            chatTitle = "محمد علي",
            senderName = "محمد علي",
            messageText = "رسالة سرية ستُحذف",
            timestamp = originalTime,
            isClonedApp = true,
            userHandleId = 95
        )

        val conv = database.conversationDao().findConversation("com.whatsapp", "محمد علي", true)
        assertNotNull(conv)
        assertTrue(conv!!.isClonedApp)

        val messagesBefore = repository.getMessagesForConversation(conv.id).first()
        assertEquals(1, messagesBefore.size)
        val originalMsg = messagesBefore.first()
        assertEquals(originalTime, originalMsg.timestamp)
        assertTrue(originalMsg.isFromClonedApp)

        // Deletion notification arrives 5 minutes later
        val deletionNoticeTime = originalTime + 300000L
        repository.saveIncomingNotification(
            packageName = "com.whatsapp",
            appDisplayName = "واتساب",
            chatTitle = "محمد علي",
            senderName = "محمد علي",
            messageText = "تم حذف هذه الرسالة",
            timestamp = deletionNoticeTime,
            isClonedApp = true,
            userHandleId = 95
        )

        val messagesAfter = repository.getMessagesForConversation(conv.id).first()
        assertEquals(1, messagesAfter.size)
        val deletedMsg = messagesAfter.first()

        // Critical: Original timestamp must NOT be overwritten!
        assertEquals(originalTime, deletedMsg.timestamp)
        assertTrue(deletedMsg.isDeletedBySender)
        assertEquals(deletionNoticeTime, deletedMsg.deletedDetectedAt)
    }
}
