package com.example.simplecustomlauncher.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simplecustomlauncher.data.MemoItem
import com.example.simplecustomlauncher.data.MemoRepository
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * メモ帳画面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { MemoRepository(context) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    var memos by remember { mutableStateOf(repository.getMemos()) }
    var newMemoText by remember { mutableStateOf("") }
    var fontSize by remember { mutableIntStateOf(repository.getFontSize()) }
    var showSettings by remember { mutableStateOf(false) }
    var editingMemo by remember { mutableStateOf<MemoItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "メモ帳",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "設定")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 新規メモ入力
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newMemoText,
                    onValueChange = { newMemoText = it },
                    modifier = Modifier.weight(1f)
                                        .focusRequester(focusRequester),
                    placeholder = { Text("新しいメモを入力", fontSize = fontSize.sp) },

                    textStyle = LocalTextStyle.current.copy(fontSize = fontSize.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newMemoText.isNotBlank()) {
                                repository.addMemo(newMemoText.trim())
                                memos = repository.getMemos()
                                newMemoText = ""
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newMemoText.isNotBlank()) {
                            repository.addMemo(newMemoText.trim())
                            memos = repository.getMemos()
                            newMemoText = ""
                            focusManager.clearFocus()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("登録", fontSize = 16.sp)
                }
            }

            // メモ一覧
            if (memos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "メモがありません\n上の入力欄から追加してください",
                        fontSize = fontSize.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = (fontSize * 1.5).sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(memos, key = { it.id }) { memo ->
                        MemoItemCard(
                            memo = memo,
                            fontSize = fontSize,
                            onToggleCheck = {
                                repository.toggleCheck(memo.id)
                                memos = repository.getMemos()
                            },
                            onEdit = { editingMemo = memo },
                            onDelete = {
                                repository.deleteMemo(memo.id)
                                memos = repository.getMemos()
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    // 設定ダイアログ
    if (showSettings) {
        FontSizeSettingsDialog(
            currentSize = fontSize,
            onSizeChange = { newSize ->
                fontSize = newSize
                repository.setFontSize(newSize)
            },
            onDismiss = { showSettings = false }
        )
    }

    // 編集ダイアログ
    editingMemo?.let { memo ->
        EditMemoDialog(
            memo = memo,
            fontSize = fontSize,
            onSave = { updatedMemo ->
                repository.updateMemo(updatedMemo)
                memos = repository.getMemos()
                editingMemo = null
            },
            onDismiss = { editingMemo = null }
        )
    }
}

@Composable
private fun MemoItemCard(
    memo: MemoItem,
    fontSize: Int,
    onToggleCheck: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleCheck() },
        colors = CardDefaults.cardColors(
            containerColor = if (memo.isChecked) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = memo.isChecked,
                onCheckedChange = { onToggleCheck() },
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = memo.text,
                fontSize = fontSize.sp,
                modifier = Modifier.weight(1f),
                textDecoration = if (memo.isChecked) TextDecoration.LineThrough else null,
                color = if (memo.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "メニュー",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("編集", fontSize = 16.sp) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("削除", fontSize = 16.sp, color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FontSizeSettingsDialog(
    currentSize: Int,
    onSizeChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sizeOptions = listOf(16, 20, 24, 28, 32)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("文字サイズ設定", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                sizeOptions.forEach { size ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSizeChange(size) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSize == size,
                            onClick = { onSizeChange(size) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (size) {
                                16 -> "小 (16)"
                                20 -> "標準 (20)"
                                24 -> "大 (24)"
                                28 -> "特大 (28)"
                                32 -> "最大 (32)"
                                else -> "$size"
                            },
                            fontSize = size.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

@Composable
private fun EditMemoDialog(
    memo: MemoItem,
    fontSize: Int,
    onSave: (MemoItem) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(memo.text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("メモを編集", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = fontSize.sp),
                shape = RoundedCornerShape(8.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSave(memo.copy(text = text.trim()))
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
