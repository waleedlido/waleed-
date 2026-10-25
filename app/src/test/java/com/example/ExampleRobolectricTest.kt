package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.MessageVaultDatabase
import com.example.data.repository.MessageVaultRepository
import com.example.data.repository.SaveResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var database: MessageVaultDatabase
  private lateinit var repository: MessageVaultRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
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
  fun testAppNameString() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("الرسائل المحذوفة", appName)
  }

  @Test
  fun testNotificationSavingAndDeletion() = runBlocking {
    val time = System.currentTimeMillis()
    val res1 = repository.saveIncomingNotification(
      packageName = "com.whatsapp",
      appDisplayName = "واتساب",
      chatTitle = "وليد الخطيب",
      senderName = "وليد الخطيب",
      messageText = "رسالة تجريبية أولى",
      timestamp = time,
      isClonedApp = false,
      userHandleId = 0
    )
    assertTrue(res1 is SaveResult.Saved)

    // Send deletion notification
    val res2 = repository.saveIncomingNotification(
      packageName = "com.whatsapp",
      appDisplayName = "واتساب",
      chatTitle = "وليد الخطيب",
      senderName = "وليد الخطيب",
      messageText = "تم حذف هذه الرسالة",
      timestamp = time + 1000,
      isClonedApp = false,
      userHandleId = 0
    )
    assertTrue(res2 is SaveResult.MarkedDeleted)

    val stats = repository.getStats()
    assertEquals(1, stats.conversationsCount)
    assertEquals(1, stats.messagesCount)
    assertEquals(1, stats.deletedMessagesCount)
  }
}
