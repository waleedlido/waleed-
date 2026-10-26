package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.MessageVaultDatabase
import com.example.data.repository.ExtractedNotificationMessage
import com.example.data.repository.MessageVaultRepository
import com.example.data.repository.SaveResult
import com.example.util.NotificationTextFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WhatsAppExtractionAndBatteryTest {

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
    fun testArabicDualAndEnglishSummariesAreRejected() {
        // Arabic dual forms
        assertTrue(NotificationTextFilter.isSummaryOrCounterText("رسالتان جديدتان"))
        assertTrue(NotificationTextFilter.isSummaryOrCounterText("رسالتين جديدتين"))
        assertTrue(NotificationTextFilter.isSummaryOrCounterText("رسالتان"))
        assertTrue(NotificationTextFilter.isSummaryOrCounterText("رسالتين"))
        assertTrue(NotificationTextFilter.isSummaryOrCounterText("رسالة جديدة"))
        assertTrue(NotificationTextFilter.isSummaryOrCounterText("رسائل جديدة"))
        assertTrue(NotificationTextFilter.isSummaryOrCounterText("3 رسائل جديدة"))
        assertTrue(NotificationTextFilter.isSummaryOrCounterText("5 رسائل من محادثتين"))
        assertTrue(NotificationTextFilter.isSummaryOrCounterText("رسالتان من أحمد"))
        assertTrue(NotificationTextFilter.isSummaryOrCounterText("محتوى الإشعار مخفي"))

        // English counter forms
        assertTrue(NotificationTextFilter.isSummaryOrCounterText("2 new messages"))
        assertTrue(NotificationTextFilter.isSummaryOrCounterText("1 new message"))
        assertTrue(NotificationTextFilter.isSummaryOrCounterText("New messages"))
        assertTrue(NotificationTextFilter.isSummaryOrCounterText("5 messages from 2 chats"))
        assertTrue(NotificationTextFilter.isSummaryOrCounterText("4 unread messages"))

        // Genuine messages must NOT be rejected
        assertFalse(NotificationTextFilter.isSummaryOrCounterText("السلام عليكم يا م. وليد"))
        assertFalse(NotificationTextFilter.isSummaryOrCounterText("وصلتني رسالتان مهمتان بالبريد"))
        assertFalse(NotificationTextFilter.isSummaryOrCounterText("Hello, how are you?"))
        assertFalse(NotificationTextFilter.isSummaryOrCounterText("تمام، سأكون في الموعد"))
    }

    @Test
    fun testSingleMessageExtractionAndPersistence() = runBlocking {
        val timestamp = 1700000000000L
        val result = repository.saveIncomingNotification(
            packageName = "com.whatsapp",
            appDisplayName = "واتساب",
            chatTitle = "م. وليد الخطيب",
            senderName = "م. وليد الخطيب",
            messageText = "مرحبا، هل تم حل مشكلة الإشعارات؟",
            timestamp = timestamp,
            isClonedApp = false,
            userHandleId = 0
        )

        assertTrue("Result must be Saved", result is SaveResult.Saved)

        val convs = repository.allConversations.first()
        assertEquals(1, convs.size)
        assertEquals("م. وليد الخطيب", convs[0].chatTitle)
        assertEquals("مرحبا، هل تم حل مشكلة الإشعارات؟", convs[0].lastMessageText)

        val msgs = repository.getMessagesForConversation(convs[0].id).first()
        assertEquals(1, msgs.size)
        assertEquals("مرحبا، هل تم حل مشكلة الإشعارات؟", msgs[0].messageText)
    }

    @Test
    fun testMultiMessageExtractionFromSingleBundle() = runBlocking {
        // Simulates receiving 2 messages from WhatsApp in a single notification
        val t1 = 1700000001000L
        val t2 = 1700000005000L

        val batch = listOf(
            ExtractedNotificationMessage(
                senderName = "أحمد خالد",
                text = "الرسالة الأولى: السلام عليكم",
                timestamp = t1
            ),
            ExtractedNotificationMessage(
                senderName = "أحمد خالد",
                text = "الرسالة الثانية: الملف جاهز للمراجعة",
                timestamp = t2
            )
        )

        val result = repository.saveIncomingBatchNotification(
            packageName = "com.whatsapp",
            appDisplayName = "واتساب",
            chatTitle = "أحمد خالد",
            messages = batch,
            isClonedApp = false,
            userHandleId = 0
        )

        assertTrue(result is SaveResult.Saved)

        val convs = repository.allConversations.first()
        assertEquals(1, convs.size)
        assertEquals(2, convs[0].messageCount)
        assertEquals("الرسالة الثانية: الملف جاهز للمراجعة", convs[0].lastMessageText)
        assertEquals(t2, convs[0].lastMessageTimestamp)

        val msgs = repository.getMessagesForConversation(convs[0].id).first()
        assertEquals(2, msgs.size)
        assertEquals("الرسالة الأولى: السلام عليكم", msgs[0].messageText)
        assertEquals("الرسالة الثانية: الملف جاهز للمراجعة", msgs[1].messageText)
    }

    @Test
    fun testSummaryOnlyNotificationDoesNotOverwriteRealMessages() = runBlocking {
        // Step 1: Save real message
        repository.saveIncomingNotification(
            packageName = "com.whatsapp",
            appDisplayName = "واتساب",
            chatTitle = "سارة علي",
            senderName = "سارة علي",
            messageText = "نص رسالة حقيقية ومهمة",
            timestamp = 1700000010000L,
            isClonedApp = false,
            userHandleId = 0
        )

        // Step 2: System receives a summary notification ("رسالتان جديدتان")
        val summaryResult = repository.saveIncomingNotification(
            packageName = "com.whatsapp",
            appDisplayName = "واتساب",
            chatTitle = "سارة علي",
            senderName = "سارة علي",
            messageText = "رسالتان جديدتان",
            timestamp = 1700000020000L,
            isClonedApp = false,
            userHandleId = 0
        )

        assertTrue("Summary notification should be ignored as message body", summaryResult is SaveResult.Ignored)

        // Step 3: Verify real message was not replaced
        val convs = repository.allConversations.first()
        assertEquals(1, convs.size)
        assertEquals("نص رسالة حقيقية ومهمة", convs[0].lastMessageText)

        val msgs = repository.getMessagesForConversation(convs[0].id).first()
        assertEquals(1, msgs.size)
        assertEquals("نص رسالة حقيقية ومهمة", msgs[0].messageText)
    }

    @Test
    fun testIdenticalTextAtDifferentTimestampsAreBothPreserved() = runBlocking {
        val t1 = 1700000100000L
        val t2 = 1700000200000L

        // User sends "تمام" first
        repository.saveIncomingNotification(
            packageName = "com.whatsapp",
            appDisplayName = "واتساب",
            chatTitle = "خالد",
            senderName = "خالد",
            messageText = "تمام",
            timestamp = t1,
            isClonedApp = false,
            userHandleId = 0
        )

        // User sends "تمام" again later (genuine distinct message)
        repository.saveIncomingNotification(
            packageName = "com.whatsapp",
            appDisplayName = "واتساب",
            chatTitle = "خالد",
            senderName = "خالد",
            messageText = "تمام",
            timestamp = t2,
            isClonedApp = false,
            userHandleId = 0
        )

        val msgs = repository.getMessagesForConversation(1L).first()
        assertEquals(2, msgs.size)
        assertEquals("تمام", msgs[0].messageText)
        assertEquals("تمام", msgs[1].messageText)
        assertEquals(t1, msgs[0].timestamp)
        assertEquals(t2, msgs[1].timestamp)
    }

    @Test
    fun testClonedAppSupportAndSeparation() = runBlocking {
        // Original WhatsApp
        repository.saveIncomingNotification(
            packageName = "com.whatsapp",
            appDisplayName = "واتساب",
            chatTitle = "العمل",
            senderName = "المدير",
            messageText = "اجتماع الساعة العاشرة",
            timestamp = 1700000300000L,
            isClonedApp = false,
            userHandleId = 0
        )

        // Cloned WhatsApp (App Twin)
        repository.saveIncomingNotification(
            packageName = "com.whatsapp",
            appDisplayName = "واتساب (مستنسخ / App Twin)",
            chatTitle = "العمل",
            senderName = "المدير",
            messageText = "اجتماع الحساب الثاني",
            timestamp = 1700000300000L,
            isClonedApp = true,
            userHandleId = 10
        )

        val allConvs = repository.allConversations.first()
        assertEquals(2, allConvs.size)

        val original = allConvs.find { !it.isClonedApp }
        val cloned = allConvs.find { it.isClonedApp }

        assertTrue(original != null)
        assertTrue(cloned != null)
        assertEquals("اجتماع الساعة العاشرة", original?.lastMessageText)
        assertEquals("اجتماع الحساب الثاني", cloned?.lastMessageText)
    }
}
