package com.example

import com.example.data.model.DeletedMessagesChartStats
import com.example.data.model.MessageEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DeletedMessagesChartStatsTest {

    @Test
    fun testEmptyDeletedMessagesListGenerates7DaysAnd4Weeks() {
        val stats = DeletedMessagesChartStats.calculate(emptyList())

        assertEquals(7, stats.dailyPoints.size)
        assertEquals(4, stats.weeklyPoints.size)
        assertEquals(0, stats.totalDeletedCount)
        assertEquals(0f, stats.dailyAverage, 0.001f)
        assertEquals(0f, stats.weeklyAverage, 0.001f)
        assertEquals(0, stats.peakDayCount)
        assertEquals(0, stats.peakWeekCount)

        assertEquals("اليوم", stats.dailyPoints.last().label)
        assertEquals("أمس", stats.dailyPoints[stats.dailyPoints.size - 2].label)
        assertEquals("هذا الأسبوع", stats.weeklyPoints.last().label)
    }

    @Test
    fun testDailyBucketingPlacesMessagesInCorrectDays() {
        val now = System.currentTimeMillis()

        // Today 00:30
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 1)
        }
        val todayMsgTime = cal.timeInMillis

        // Yesterday 14:00
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayMsgTime = cal.timeInMillis

        // 3 days ago
        cal.add(Calendar.DAY_OF_YEAR, -2)
        val threeDaysAgoMsgTime = cal.timeInMillis

        val messages = listOf(
            MessageEntity(
                id = 1,
                conversationId = 10,
                senderName = "أحمد",
                messageText = "رسالة اليوم 1",
                timestamp = todayMsgTime - 5000,
                isDeletedBySender = true,
                deletedDetectedAt = todayMsgTime
            ),
            MessageEntity(
                id = 2,
                conversationId = 10,
                senderName = "أحمد",
                messageText = "رسالة اليوم 2",
                timestamp = todayMsgTime - 10000,
                isDeletedBySender = true,
                deletedDetectedAt = todayMsgTime + 500
            ),
            MessageEntity(
                id = 3,
                conversationId = 10,
                senderName = "خالد",
                messageText = "رسالة أمس",
                timestamp = yesterdayMsgTime,
                isDeletedBySender = true,
                deletedDetectedAt = yesterdayMsgTime
            ),
            MessageEntity(
                id = 4,
                conversationId = 10,
                senderName = "سارة",
                messageText = "رسالة قبل 3 أيام",
                timestamp = threeDaysAgoMsgTime,
                isDeletedBySender = true,
                deletedDetectedAt = threeDaysAgoMsgTime
            )
        )

        val stats = DeletedMessagesChartStats.calculate(messages, nowMillis = now)

        assertEquals(4, stats.totalDeletedCount)

        // Today (last point) should have 2
        val todayPoint = stats.dailyPoints.last()
        assertEquals("اليوم", todayPoint.label)
        assertEquals(2, todayPoint.count)

        // Yesterday (second to last) should have 1
        val yesterdayPoint = stats.dailyPoints[stats.dailyPoints.size - 2]
        assertEquals("أمس", yesterdayPoint.label)
        assertEquals(1, yesterdayPoint.count)

        // Peak day should be "اليوم" with count 2
        assertEquals("اليوم", stats.peakDayLabel)
        assertEquals(2, stats.peakDayCount)

        // All 4 messages happened within the last 7 days, so "هذا الأسبوع" should have 4
        val thisWeekPoint = stats.weeklyPoints.last()
        assertEquals("هذا الأسبوع", thisWeekPoint.label)
        assertEquals(4, thisWeekPoint.count)
        assertEquals("هذا الأسبوع", stats.peakWeekLabel)
        assertEquals(4, stats.peakWeekCount)
    }

    @Test
    fun testWeeklyBucketingPlacesOlderMessagesCorrectly() {
        val now = System.currentTimeMillis()

        // 10 days ago (belongs to Week 1: "الأسبوع الماضي")
        val tenDaysAgo = now - 10L * 24 * 60 * 60 * 1000

        // 22 days ago (belongs to Week 3: "منذ 3 أسابيع")
        val twentyTwoDaysAgo = now - 22L * 24 * 60 * 60 * 1000

        val messages = listOf(
            MessageEntity(
                id = 1,
                conversationId = 20,
                senderName = "محمد",
                messageText = "رسالة قديمة 1",
                timestamp = tenDaysAgo,
                isDeletedBySender = true,
                deletedDetectedAt = tenDaysAgo
            ),
            MessageEntity(
                id = 2,
                conversationId = 20,
                senderName = "علي",
                messageText = "رسالة قديمة 2",
                timestamp = twentyTwoDaysAgo,
                isDeletedBySender = true,
                deletedDetectedAt = twentyTwoDaysAgo
            )
        )

        val stats = DeletedMessagesChartStats.calculate(messages, nowMillis = now)

        assertEquals(2, stats.totalDeletedCount)

        val lastWeekPoint = stats.weeklyPoints[stats.weeklyPoints.size - 2]
        assertEquals("الأسبوع الماضي", lastWeekPoint.label)
        assertEquals(1, lastWeekPoint.count)

        val threeWeeksAgoPoint = stats.weeklyPoints.first()
        assertEquals("منذ 3 أسابيع", threeWeeksAgoPoint.label)
        assertEquals(1, threeWeeksAgoPoint.count)
    }
}
