package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChartDataPoint
import com.example.data.model.DeletedMessagesChartStats
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedContainer
import com.example.ui.theme.VaultTheme
import kotlin.math.max

enum class ChartTimeframe(val title: String) {
    DAILY("يومياً (7 أيام)"),
    WEEKLY("أسبوعياً (4 أسابيع)")
}

/**
 * Modern Native Compose Chart inspired by Recharts (ComposedChart with animated bars,
 * spline trend line, grid reference lines, interactive tooltip, and daily/weekly bucketing).
 */
@Composable
fun RechartsDeletedMessagesChart(
    stats: DeletedMessagesChartStats,
    modifier: Modifier = Modifier
) {
    val themeColors = VaultTheme.colors
    var selectedTimeframe by remember { mutableStateOf(ChartTimeframe.DAILY) }

    val currentPoints = if (selectedTimeframe == ChartTimeframe.DAILY) {
        stats.dailyPoints
    } else {
        stats.weeklyPoints
    }

    var selectedPointIndex by remember(selectedTimeframe) {
        val lastWithCount = currentPoints.indexOfLast { it.count > 0 }
        mutableStateOf(if (lastWithCount >= 0) lastWithCount else currentPoints.lastIndex)
    }

    // Animation progress triggered on timeframe change or data load
    var animationTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(selectedTimeframe, stats) {
        animationTrigger = true
    }
    val animProgress by animateFloatAsState(
        targetValue = if (animationTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "chartAnim"
    )

    val maxCount = max(currentPoints.maxOfOrNull { it.count } ?: 0, 4)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_deleted_messages_card")
            .border(1.dp, themeColors.border, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header: Title & Timeframe Selector Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(themeColors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = themeColors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "إحصائيات الرسائل المحذوفة المسترجعة",
                            color = themeColors.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تحليل مرئي بياني مستوحى من Recharts",
                            color = themeColors.textMuted,
                            fontSize = 10.5.sp
                        )
                    }
                }

                // Timeframe switch pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(themeColors.surfaceElevated)
                        .border(1.dp, themeColors.border, RoundedCornerShape(20.dp))
                        .padding(2.dp)
                ) {
                    ChartTimeframe.entries.forEach { timeframe ->
                        val isSelected = timeframe == selectedTimeframe
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) themeColors.primary else Color.Transparent)
                                .clickable { selectedTimeframe = timeframe }
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (timeframe == ChartTimeframe.DAILY) "يومي" else "أسبوعي",
                                color = if (isSelected) Color.Black else themeColors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stat Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total
                MetricChip(
                    label = "الإجمالي",
                    value = "${stats.totalDeletedCount}",
                    color = DangerRed,
                    containerColor = DangerRedContainer.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                )

                // Average
                val avgText = if (selectedTimeframe == ChartTimeframe.DAILY) {
                    String.format(java.util.Locale.US, "%.1f", stats.dailyAverage)
                } else {
                    String.format(java.util.Locale.US, "%.1f", stats.weeklyAverage)
                }
                MetricChip(
                    label = if (selectedTimeframe == ChartTimeframe.DAILY) "معدل يومي" else "معدل أسبوعي",
                    value = avgText,
                    color = themeColors.secondary,
                    containerColor = themeColors.secondary.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f)
                )

                // Peak
                val peakText = if (selectedTimeframe == ChartTimeframe.DAILY) {
                    "${stats.peakDayCount} (${stats.peakDayLabel})"
                } else {
                    "${stats.peakWeekCount} (${stats.peakWeekLabel})"
                }
                MetricChip(
                    label = "الذروة",
                    value = peakText,
                    color = CyanAccent,
                    containerColor = CyanAccent.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1.3f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Tooltip Card for Selected Bar
            val activePoint = currentPoints.getOrNull(selectedPointIndex)
            AnimatedVisibility(
                visible = activePoint != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (activePoint != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(themeColors.surfaceElevated)
                            .border(1.dp, themeColors.border, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(themeColors.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${activePoint.label} (${activePoint.subLabel}):",
                                    color = themeColors.textSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = if (activePoint.count == 0) "لا توجد رسائل محذوفة" else "${activePoint.count} رسائل محذوفة مسترجعة 🔒",
                                color = if (activePoint.count > 0) DangerRed else themeColors.textMuted,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Canvas: Dotted Grid + Gradient Bars + Trend Line + Data Point Dots
            val barCount = currentPoints.size
            val primaryColor = themeColors.primary
            val secondaryColor = themeColors.secondary
            val gridColor = themeColors.border.copy(alpha = 0.4f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .pointerInput(currentPoints, maxCount) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val step = width / barCount
                            val index = (offset.x / step).toInt().coerceIn(0, barCount - 1)
                            selectedPointIndex = index
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(145.dp)) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val bottomY = canvasHeight - 12f
                    val topY = 16f
                    val usableHeight = bottomY - topY

                    // 1. Draw Dotted Reference Grid Lines (Y-Axis levels: 0%, 50%, 100%)
                    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    val levels = listOf(0.0f, 0.5f, 1.0f)
                    levels.forEach { level ->
                        val y = bottomY - (usableHeight * level)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1f,
                            pathEffect = dashedEffect
                        )
                    }

                    // 2. Draw Recharts Gradient Bars
                    val step = canvasWidth / barCount
                    val barWidth = step * 0.44f

                    val trendPoints = mutableListOf<Offset>()

                    currentPoints.forEachIndexed { index, point ->
                        val centerX = (index * step) + (step / 2f)
                        val left = centerX - (barWidth / 2f)

                        val rawHeight = if (maxCount > 0) {
                            (point.count.toFloat() / maxCount.toFloat()) * usableHeight
                        } else 0f

                        val animatedHeight = (rawHeight * animProgress).coerceAtLeast(if (point.count > 0) 6f else 2f)
                        val top = bottomY - animatedHeight

                        val isSelected = index == selectedPointIndex

                        // Gradient fill for bar
                        val barBrush = Brush.verticalGradient(
                            colors = if (point.count > 0) {
                                listOf(
                                    primaryColor,
                                    primaryColor.copy(alpha = if (isSelected) 0.65f else 0.35f)
                                )
                            } else {
                                listOf(
                                    gridColor.copy(alpha = 0.5f),
                                    gridColor.copy(alpha = 0.15f)
                                )
                            },
                            startY = top,
                            endY = bottomY
                        )

                        // Draw Rounded Bar
                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, animatedHeight),
                            cornerRadius = CornerRadius(8f, 8f)
                        )

                        // Highlight border if selected
                        if (isSelected) {
                            drawRoundRect(
                                color = if (point.count > 0) DangerRed else primaryColor,
                                topLeft = Offset(left - 2f, top - 2f),
                                size = Size(barWidth + 4f, animatedHeight + 2f),
                                cornerRadius = CornerRadius(10f, 10f),
                                style = Stroke(width = 2f)
                            )
                        }

                        // Record point for Trend Line
                        trendPoints.add(Offset(centerX, top))
                    }

                    // 3. Draw Trend Line across top of bars (Recharts ComposedChart Line)
                    if (trendPoints.size > 1 && animProgress > 0.2f) {
                        val linePath = Path()
                        linePath.moveTo(trendPoints.first().x, trendPoints.first().y)

                        for (i in 1 until trendPoints.size) {
                            val prev = trendPoints[i - 1]
                            val curr = trendPoints[i]
                            val cX = (prev.x + curr.x) / 2f
                            linePath.cubicTo(cX, prev.y, cX, curr.y, curr.x, curr.y)
                        }

                        drawPath(
                            path = linePath,
                            color = secondaryColor.copy(alpha = 0.85f),
                            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                        )

                        // Draw Recharts Data Dots
                        trendPoints.forEachIndexed { i, pt ->
                            val ptCount = currentPoints[i].count
                            val isSel = i == selectedPointIndex

                            // Outer white halo
                            drawCircle(
                                color = Color.White,
                                radius = if (isSel) 5.5f else 3.5f,
                                center = pt
                            )
                            // Inner colored dot
                            drawCircle(
                                color = if (ptCount > 0) DangerRed else secondaryColor,
                                radius = if (isSel) 4f else 2.5f,
                                center = pt
                            )
                        }
                    }
                }

                // X-Axis Labels Row below Canvas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    currentPoints.forEachIndexed { index, point ->
                        val isSelected = index == selectedPointIndex
                        Text(
                            text = point.label,
                            color = if (isSelected) primaryColor else themeColors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPointIndex = index }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chart Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(themeColors.primary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "أعمدة الرسائل المحذوفة",
                        color = themeColors.textMuted,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(2.5.dp)
                            .background(themeColors.secondary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "خط الاتجاه (Trend Line)",
                        color = themeColors.textMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    val themeColors = VaultTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(
                text = label,
                color = themeColors.textMuted,
                fontSize = 9.5.sp
            )
            Text(
                text = value,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
