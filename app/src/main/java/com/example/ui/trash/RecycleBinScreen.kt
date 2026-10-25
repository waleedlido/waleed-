package com.example.ui.trash

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConversationEntity
import com.example.data.repository.MessageVaultRepository
import com.example.ui.components.AppAvatarIcon
import com.example.ui.components.AppSourceBadge
import com.example.ui.components.AppSourceIcon
import com.example.ui.home.formatTime
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedContainer
import com.example.ui.theme.DarkEmerald
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    repository: MessageVaultRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val trashedConversations by repository.trashConversations.collectAsState(initial = emptyList())
    val selectedIds = remember { mutableStateListOf<Long>() }
    val isSelectionMode = selectedIds.isNotEmpty()

    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var conversationToDeleteId by remember { mutableStateOf<Long?>(null) }

    BackHandler {
        if (showEmptyTrashDialog || showDeleteConfirmDialog) {
            showEmptyTrashDialog = false
            showDeleteConfirmDialog = false
        } else if (isSelectionMode) {
            selectedIds.clear()
        } else {
            onBack()
        }
    }

    Scaffold(
        containerColor = DeepObsidian,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = TextPrimaryDark
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSelectionMode) selectedIds.clear() else onBack()
                        },
                        modifier = Modifier.testTag("trash_back_button")
                    ) {
                        Icon(
                            imageVector = if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isSelectionMode) "إلغاء التحديد" else "رجوع",
                            tint = TextPrimaryDark
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = if (isSelectionMode) "تم تحديد ${selectedIds.size}" else "سلة المحذوفات",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        if (!isSelectionMode) {
                            Text(
                                text = "${trashedConversations.size} محادثات محذوفة مؤقتًا",
                                fontSize = 11.sp,
                                color = TextMutedDark
                            )
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // Select All / Deselect All
                        IconButton(onClick = {
                            if (selectedIds.size == trashedConversations.size) {
                                selectedIds.clear()
                            } else {
                                selectedIds.clear()
                                selectedIds.addAll(trashedConversations.map { it.id })
                            }
                        }) {
                            Icon(imageVector = Icons.Default.SelectAll, contentDescription = "تحديد الكل", tint = CyanAccent)
                        }

                        // Restore Selected
                        IconButton(onClick = {
                            val idsToRestore = selectedIds.toList()
                            selectedIds.clear()
                            coroutineScope.launch {
                                repository.restoreMultipleFromTrash(idsToRestore)
                                Toast.makeText(context, "تم استعادة ${idsToRestore.size} محادثة بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = "استعادة المحدد", tint = NeonGreen)
                        }

                        // Delete Selected Permanently
                        IconButton(onClick = {
                            showDeleteConfirmDialog = true
                        }) {
                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "حذف نهائي", tint = DangerRed)
                        }
                    } else if (trashedConversations.isNotEmpty()) {
                        TextButton(onClick = { showEmptyTrashDialog = true }) {
                            Text("إفراغ السلة", color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Explanatory Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "سلة المحذوفات الخاصة بالمحادثات",
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "المحادثات المنقولة هنا لا تظهر في الرئيسية ولا تُحذف نهائيًا إلا بطلبك. يمكنك استعادتها بأي وقت.",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            if (trashedConversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = TextMutedDark,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "سلة المحذوفات فارغة",
                            color = TextPrimaryDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "عندما تقوم بحذف أي محادثة من الرئيسية ستنتقل إلى هنا لحمايتها من الفقدان العرضي.",
                            color = TextMutedDark,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("trash_conversations_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(trashedConversations, key = { it.id }) { conv ->
                        val isSelected = selectedIds.contains(conv.id)
                        TrashConversationCard(
                            conversation = conv,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onToggleSelect = {
                                if (isSelected) selectedIds.remove(conv.id) else selectedIds.add(conv.id)
                            },
                            onRestore = {
                                coroutineScope.launch {
                                    repository.restoreConversationFromTrash(conv.id)
                                    val result = snackbarHostState.showSnackbar(
                                        message = "تمت استعادة محادثة \"${conv.chatTitle}\"",
                                        actionLabel = "تراجع",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        repository.moveConversationToTrash(conv.id)
                                    }
                                }
                            },
                            onPermanentDelete = {
                                conversationToDeleteId = conv.id
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Confirm Empty Trash Dialog
    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            containerColor = SurfaceDark,
            title = {
                Text("إفراغ سلة المحذوفات؟", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "سيتم حذف جميع المحادثات الموجودة في السلة (${trashedConversations.size} محادثة) نهائيًا مع كافة رسائلها، ولا يمكن التراجع بعد ذلك.",
                    color = TextSecondaryDark,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyTrashDialog = false
                        coroutineScope.launch {
                            repository.emptyTrash()
                            Toast.makeText(context, "تم إفراغ سلة المحذوفات بالكامل", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White)
                ) {
                    Text("إفراغ نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text("إلغاء", color = TextMutedDark)
                }
            }
        )
    }

    // Confirm Delete Dialog (single or multiple)
    if (showDeleteConfirmDialog) {
        val count = if (conversationToDeleteId != null) 1 else selectedIds.size
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                conversationToDeleteId = null
            },
            containerColor = SurfaceDark,
            title = {
                Text("تأكيد الحذف النهائي؟", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "هل أنت متأكد من حذف $count محادثة نهائيًا؟ لن تتمكن من استرجاعها بعد ذلك.",
                    color = TextSecondaryDark,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        coroutineScope.launch {
                            if (conversationToDeleteId != null) {
                                repository.permanentlyDeleteConversation(conversationToDeleteId!!)
                                conversationToDeleteId = null
                            } else {
                                val ids = selectedIds.toList()
                                selectedIds.clear()
                                repository.permanentlyDeleteMultiple(ids)
                            }
                            Toast.makeText(context, "تم الحذف النهائي بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White)
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    conversationToDeleteId = null
                }) {
                    Text("إلغاء", color = TextMutedDark)
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TrashConversationCard(
    conversation: ConversationEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) NeonGreen else BorderDark,
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelect()
                },
                onLongClick = {
                    onToggleSelect()
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SurfaceElevatedDark else SurfaceDark
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) NeonGreen else SurfaceElevatedDark)
                            .border(1.5.dp, if (isSelected) NeonGreen else BorderDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                AppAvatarIcon(
                    packageName = conversation.packageName,
                    chatTitle = conversation.chatTitle,
                    isClonedApp = conversation.isClonedApp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            AppSourceIcon(
                                packageName = conversation.packageName,
                                isClonedApp = conversation.isClonedApp,
                                size = 22.dp,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = conversation.chatTitle,
                                color = TextPrimaryDark,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "حُذفت ${formatTime(conversation.trashedAt ?: conversation.lastMessageTimestamp)}",
                            color = TextMutedDark,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = conversation.lastMessageText,
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppSourceBadge(
                            packageName = conversation.packageName,
                            isClonedApp = conversation.isClonedApp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceElevatedDark)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${conversation.messageCount} رسائل",
                                color = TextMutedDark,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            if (!isSelectionMode) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Restore Button
                    OutlinedButton(
                        onClick = onRestore,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NeonGreen))
                    ) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("استعادة المحادثة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Permanent Delete Button
                    Button(
                        onClick = onPermanentDelete,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRedContainer, contentColor = DangerRed)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف نهائي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
