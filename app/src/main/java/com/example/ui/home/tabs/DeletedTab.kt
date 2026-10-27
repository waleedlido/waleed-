package com.example.ui.home.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeletedMessagesChartStats
import com.example.data.repository.MessageVaultRepository
import com.example.ui.components.AppAvatarIcon
import com.example.ui.components.RechartsDeletedMessagesChart
import com.example.ui.home.formatTime
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedContainer
import com.example.ui.theme.VaultTheme
import com.example.util.PrivacyBlurConfig
import com.example.util.PrivacyBlurManager

@Composable
fun DeletedTab(
    repository: MessageVaultRepository,
    onConversationClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val privacyManager = remember { PrivacyBlurManager.get(context) }
    val privacyConfig by privacyManager.configFlow.collectAsState(initial = PrivacyBlurConfig())
    val colors = VaultTheme.colors

    val conversations by repository.allConversations.collectAsState(initial = emptyList())
    val deletedConversations = conversations.filter { it.deletedMessagesCount > 0 }
    val totalDeletedMessages = deletedConversations.sumOf { it.deletedMessagesCount }

    val chartStats by repository.deletedMessagesChartStatsFlow.collectAsState(
        initial = DeletedMessagesChartStats.calculate(emptyList())
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("deleted_tab_content"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Stats Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DangerRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DangerRedContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DangerRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "سجل الرسائل المحذوفة الموثقة",
                            color = DangerRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalDeletedMessages رسالة قام أصحابها بحذفها ولكنها محفوظة لديك 🔒",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 2. New Recharts-Inspired Daily & Weekly Deleted Messages Chart Section
        item {
            RechartsDeletedMessagesChart(
                stats = chartStats,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 3. Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المحادثات المحتوية على رسائل محذوفة (${deletedConversations.size})",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4. Conversations List or Empty State
        if (deletedConversations.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(colors.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "لا توجد رسائل محذوفة حتى الآن",
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "عندما يرسل لك شخص رسالة على واتساب أو تيليجرام ثم يضغط \"حذف لدى الجميع\"، سيتم توثيقها والاحتفاظ بها هنا فورًا وتحديث الرسم البياني أعلاه.",
                            color = colors.textMuted,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        } else {
            items(deletedConversations, key = { it.id }) { conv ->
                val shouldBlurSender = privacyConfig.isMasterBlurActive && privacyConfig.blurSenderNames
                val shouldBlurPreview = privacyConfig.isMasterBlurActive && privacyConfig.blurHomePreviews

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                        .clickable { onConversationClick(conv.id) },
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Single dedicated app avatar (50dp) with clone badge if twin
                        AppAvatarIcon(
                            packageName = conv.packageName,
                            isClonedApp = conv.isClonedApp,
                            size = 50.dp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = conv.chatTitle,
                                    color = colors.textPrimary,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(if (shouldBlurSender) Modifier.blur(8.dp) else Modifier)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = formatTime(conv.lastMessageTimestamp),
                                    color = colors.textMuted,
                                    fontSize = 10.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = conv.lastMessageText,
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = if (shouldBlurPreview) Modifier.blur(10.dp) else Modifier
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // High contrast deleted badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DangerRedContainer)
                                .border(1.dp, DangerRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${conv.deletedMessagesCount} محذوفة",
                                color = DangerRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
