package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    private val arabicLocale = Locale("ar")

    /**
     * Formats conversation timestamp for HomeScreen, SearchTab, DeletedTab, RecycleBin.
     * Replaces vague "الآن" with the actual Arabic time/date:
     * - Today: "10:35 ص" / "08:15 م"
     * - Yesterday: "أمس 10:35 ص"
     * - This year: "25 أكتوبر 10:35 ص"
     * - Older: "25/10/2025"
     */
    fun formatConversationTimestamp(timestamp: Long): String {
        if (timestamp <= 0) return ""

        val now = System.currentTimeMillis()
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val isSameDay = targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                targetCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) {
            val timeFormat = SimpleDateFormat("h:mm a", arabicLocale)
            return timeFormat.format(Date(timestamp)).cleanArabicAmPm()
        }

        // Check if yesterday
        nowCal.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                targetCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) {
            val timeFormat = SimpleDateFormat("h:mm a", arabicLocale)
            return "أمس " + timeFormat.format(Date(timestamp)).cleanArabicAmPm()
        }

        // Check if same year
        nowCal.timeInMillis = now
        val isSameYear = targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
        if (isSameYear) {
            val dateFormat = SimpleDateFormat("d MMMM h:mm a", arabicLocale)
            return dateFormat.format(Date(timestamp)).cleanArabicAmPm()
        }

        val fullDateFormat = SimpleDateFormat("dd/MM/yyyy", arabicLocale)
        return fullDateFormat.format(Date(timestamp))
    }

    /**
     * Formats time displayed below each message bubble:
     * e.g., "10:35 ص" or "25/10/2026 — 10:35 ص"
     */
    fun formatMessageBubbleTime(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val sdf = SimpleDateFormat("dd/MM/yyyy — h:mm a", arabicLocale)
        return sdf.format(Date(timestamp)).cleanArabicAmPm()
    }

    /**
     * Short time only for compact message badges (e.g. "10:35 ص")
     */
    fun formatShortTime(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val sdf = SimpleDateFormat("h:mm a", arabicLocale)
        return sdf.format(Date(timestamp)).cleanArabicAmPm()
    }

    /**
     * Daily Section Header label between messages:
     * - "اليوم"
     * - "أمس"
     * - "25 أكتوبر 2026"
     */
    fun formatDateHeader(timestamp: Long): String {
        if (timestamp <= 0) return ""

        val now = System.currentTimeMillis()
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val isSameDay = targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                targetCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) return "اليوم"

        nowCal.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                targetCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) return "أمس"

        val sdf = SimpleDateFormat("d MMMM yyyy", arabicLocale)
        return sdf.format(Date(timestamp))
    }

    /**
     * Full detailed Arabic date & time with day of the week, date, time and seconds:
     * e.g. "الأحد 25 أكتوبر 2026 — 10:35:42 ص"
     */
    fun formatFullDetailedDateTime(timestamp: Long): String {
        if (timestamp <= 0) return "غير متوفر"
        val sdf = SimpleDateFormat("EEEE d MMMM yyyy — hh:mm:ss a", arabicLocale)
        return sdf.format(Date(timestamp)).cleanArabicAmPm()
    }

    /**
     * Checks if two timestamps fall on the same calendar day.
     */
    fun isSameDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun String.cleanArabicAmPm(): String {
        return this.replace("AM", "ص")
            .replace("PM", "م")
            .replace("am", "ص")
            .replace("pm", "م")
    }
}
