package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ChartDataPoint(
    val id: String,
    val label: String,
    val subLabel: String,
    val count: Int,
    val dateMillis: Long
)

data class DeletedMessagesChartStats(
    val dailyPoints: List<ChartDataPoint>,
    val weeklyPoints: List<ChartDataPoint>,
    val totalDeletedCount: Int,
    val dailyAverage: Float,
    val weeklyAverage: Float,
    val peakDayLabel: String,
    val peakDayCount: Int,
    val peakWeekLabel: String,
    val peakWeekCount: Int
) {
    companion object {
        private val arabicLocale = Locale.forLanguageTag("ar")

        fun calculate(
            deletedMessages: List<MessageEntity>,
            nowMillis: Long = System.currentTimeMillis()
        ): DeletedMessagesChartStats {
            val totalCount = deletedMessages.size

            // 1. Calculate Daily Points (Last 7 Days)
            val dailyList = mutableListOf<ChartDataPoint>()
            val dayFormat = SimpleDateFormat("EEEE", arabicLocale)
            val dayMonthFormat = SimpleDateFormat("d MMMM", arabicLocale)

            for (i in 6 downTo 0) {
                val dayStartCal = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    add(Calendar.DAY_OF_YEAR, -i)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = dayStartCal.timeInMillis

                val dayEndCal = Calendar.getInstance().apply {
                    timeInMillis = startOfDay
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val endOfDay = dayEndCal.timeInMillis

                val countForDay = deletedMessages.count { msg ->
                    val eventTime = if (msg.deletedDetectedAt != null && msg.deletedDetectedAt > 0) {
                        msg.deletedDetectedAt
                    } else {
                        msg.timestamp
                    }
                    eventTime in startOfDay..endOfDay
                }

                val label = when (i) {
                    0 -> "اليوم"
                    1 -> "أمس"
                    else -> dayFormat.format(Date(startOfDay))
                }
                val subLabel = dayMonthFormat.format(Date(startOfDay))

                dailyList.add(
                    ChartDataPoint(
                        id = "day_$i",
                        label = label,
                        subLabel = subLabel,
                        count = countForDay,
                        dateMillis = startOfDay
                    )
                )
            }

            // 2. Calculate Weekly Points (Last 4 Weeks)
            val weeklyList = mutableListOf<ChartDataPoint>()
            val shortDayFormat = SimpleDateFormat("d MMM", arabicLocale)

            for (w in 3 downTo 0) {
                val weekEndCal = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    add(Calendar.DAY_OF_YEAR, -w * 7)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val endOfWeek = weekEndCal.timeInMillis

                val weekStartCal = Calendar.getInstance().apply {
                    timeInMillis = endOfWeek
                    add(Calendar.DAY_OF_YEAR, -6)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfWeek = weekStartCal.timeInMillis

                val countForWeek = deletedMessages.count { msg ->
                    val eventTime = if (msg.deletedDetectedAt != null && msg.deletedDetectedAt > 0) {
                        msg.deletedDetectedAt
                    } else {
                        msg.timestamp
                    }
                    eventTime in startOfWeek..endOfWeek
                }

                val label = when (w) {
                    0 -> "هذا الأسبوع"
                    1 -> "الأسبوع الماضي"
                    2 -> "منذ أسبوعين"
                    else -> "منذ 3 أسابيع"
                }
                val subLabel = "${shortDayFormat.format(Date(startOfWeek))} - ${shortDayFormat.format(Date(endOfWeek))}"

                weeklyList.add(
                    ChartDataPoint(
                        id = "week_$w",
                        label = label,
                        subLabel = subLabel,
                        count = countForWeek,
                        dateMillis = startOfWeek
                    )
                )
            }

            val peakDay = if (dailyList.any { it.count > 0 }) {
                dailyList.filter { it.count > 0 }.maxWithOrNull(compareBy({ it.count }, { it.dateMillis }))
            } else {
                null
            }

            val peakWeek = if (weeklyList.any { it.count > 0 }) {
                weeklyList.filter { it.count > 0 }.maxWithOrNull(compareBy({ it.count }, { it.dateMillis }))
            } else {
                null
            }

            val dailySum = dailyList.sumOf { it.count }
            val dailyAvg = if (dailyList.isNotEmpty()) dailySum.toFloat() / dailyList.size else 0f

            val weeklySum = weeklyList.sumOf { it.count }
            val weeklyAvg = if (weeklyList.isNotEmpty()) weeklySum.toFloat() / weeklyList.size else 0f

            return DeletedMessagesChartStats(
                dailyPoints = dailyList,
                weeklyPoints = weeklyList,
                totalDeletedCount = totalCount,
                dailyAverage = dailyAvg,
                weeklyAverage = weeklyAvg,
                peakDayLabel = peakDay?.label ?: "—",
                peakDayCount = peakDay?.count ?: 0,
                peakWeekLabel = peakWeek?.label ?: "—",
                peakWeekCount = peakWeek?.count ?: 0
            )
        }
    }
}
