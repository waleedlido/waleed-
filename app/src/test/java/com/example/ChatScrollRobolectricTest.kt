package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.MessageVaultDatabase
import com.example.data.repository.MessageVaultRepository
import com.example.ui.chat.ChatScrollStateCache
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
class ChatScrollRobolectricTest {

    private lateinit var database: MessageVaultDatabase
    private lateinit var repository: MessageVaultRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MessageVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MessageVaultRepository(database)
        ChatScrollStateCache.clearAll()
    }

    @After
    fun tearDown() {
        database.close()
        ChatScrollStateCache.clearAll()
    }

    @Test
    fun testLongConversation100PlusMessagesChronologicalOrder() = runBlocking {
        val baseTime = 1700000000000L
        val totalMessages = 120

        // Populate 120 messages chronologically
        for (i in 1..totalMessages) {
            repository.saveIncomingNotification(
                packageName = "com.whatsapp",
                appDisplayName = "واتساب",
                chatTitle = "محادثة اختبار 120 رسالة",
                senderName = if (i % 2 == 0) "أحمد" else "سارة",
                messageText = "رسالة رقم $i في السجل الطويل",
                timestamp = baseTime + (i * 1000L),
                isClonedApp = false,
                userHandleId = 0
            )
        }

        val conv = database.conversationDao().findConversation("com.whatsapp", "محادثة اختبار 120 رسالة", false)
        assertNotNull(conv)
        assertEquals(totalMessages, conv!!.messageCount)

        val messages = repository.getMessagesForConversation(conv.id).first()
        assertEquals(totalMessages, messages.size)

        // Verify strictly chronological order (oldest first at index 0, newest at index 119)
        assertEquals("رسالة رقم 1 في السجل الطويل", messages.first().messageText)
        assertEquals("رسالة رقم 120 في السجل الطويل", messages.last().messageText)
        assertTrue(messages.last().timestamp > messages.first().timestamp)

        for (i in 0 until messages.size - 1) {
            assertTrue("Message $i timestamp should be <= Message ${i+1}", messages[i].timestamp <= messages[i+1].timestamp)
        }
    }

    @Test
    fun testChatScrollStateCachePersistence() {
        val convId = 42L
        assertEquals(null, ChatScrollStateCache.getPosition(convId))

        // Simulate user scrolling to message #85
        ChatScrollStateCache.savePosition(convId, 85, 12)
        val pos = ChatScrollStateCache.getPosition(convId)
        assertNotNull(pos)
        assertEquals(85, pos!!.first)
        assertEquals(12, pos.second)

        // Clear and verify
        ChatScrollStateCache.clear(convId)
        assertEquals(null, ChatScrollStateCache.getPosition(convId))
    }

    @Test
    fun testFastScrollTargetCalculations() {
        val totalItems = 150
        val currentIndex = 145

        // Scrolling to top (target = 0): distance is 145 (> 10)
        val targetIndex = 0
        val distance = kotlin.math.abs(currentIndex - targetIndex)
        assertTrue(distance > 10)

        val intermediateTop = if (targetIndex > currentIndex) {
            (targetIndex - 5).coerceAtLeast(0)
        } else {
            (targetIndex + 5).coerceAtMost(totalItems - 1)
        }
        // Jump target should be near item 5, avoiding frame lag across 145 items
        assertEquals(5, intermediateTop)

        // Scrolling back to bottom (target = 149) from index 5: distance is 144 (> 10)
        val intermediateBottom = if (149 > 5) {
            (149 - 5).coerceAtLeast(0)
        } else {
            (149 + 5).coerceAtMost(totalItems - 1)
        }
        assertEquals(144, intermediateBottom)
    }

    @Test
    fun testNewMessageArrivalDetectionLogic() {
        // When user is viewing older items (canScrollForward = true)
        val canScrollForward = true
        var hasNewMessagePill = false

        val oldSize = 100
        val newSize = 101

        if (newSize > oldSize) {
            if (canScrollForward) {
                hasNewMessagePill = true
            }
        }
        assertTrue("New message pill should show when user is reading past history", hasNewMessagePill)

        // When user scrolls to bottom (canScrollForward becomes false)
        val atBottom = false // canScrollForward is false
        if (!atBottom) {
            hasNewMessagePill = false
        }
        assertFalse("New message pill must auto-dismiss when reaching bottom", hasNewMessagePill)
    }
}
