package com.example.simplecustomlauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simplecustomlauncher.data.ShortcutItem
import com.example.simplecustomlauncher.data.ShortcutType

/**
 * スロット編集時に表示するBottomSheet
 * 配置するショートカットを選択する
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotEditSheet(
    currentShortcut: ShortcutItem?,  // 現在このスロットに配置されているもの
    allShortcuts: List<ShortcutItem>, // 全ショートカット
    placedShortcutIds: Set<String>,   // 既に配置されているショートカットのID
    onSelectShortcut: (ShortcutItem) -> Unit,
    onAddNew: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    // 未配置のショートカット
    val unplacedShortcuts = allShortcuts.filter {
        it.id !in placedShortcutIds && it.type != ShortcutType.EMPTY
    }

    // 配置済みのショートカット（現在のスロット以外）
    val otherPlacedShortcuts = allShortcuts.filter {
        it.id in placedShortcutIds &&
        it.id != currentShortcut?.id &&
        it.type != ShortcutType.EMPTY
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(16.dp)
        ) {
            Text(
                text = "このスロットに配置",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. アプリ一覧から選ぶ（一番上）
                item {
                    NavigationButton(
                        text = "アプリ一覧から選ぶ",
                        icon = "📱",
                        onClick = onAddNew
                    )
                }

                // 2. 未配置のショートカット
                if (unplacedShortcuts.isNotEmpty()) {
                    item {
                        SectionHeader(text = "未配置ショートカット")
                    }
                    items(unplacedShortcuts) { shortcut ->
                        ShortcutListItem(
                            shortcut = shortcut,
                            subtitle = "タップで配置",
                            backgroundColor = Color(0xFFF5F5F5),
                            onClick = { onSelectShortcut(shortcut) }
                        )
                    }
                }

                // 3. 配置済みと入れ替え
                if (otherPlacedShortcuts.isNotEmpty()) {
                    item {
                        SectionHeader(text = "配置済みと入れ替え")
                    }
                    items(otherPlacedShortcuts) { shortcut ->
                        ShortcutListItem(
                            shortcut = shortcut,
                            subtitle = "タップで入れ替え",
                            backgroundColor = Color(0xFFFFF3E0),
                            onClick = { onSelectShortcut(shortcut) }
                        )
                    }
                }

                // 空にする（現在配置されている場合のみ）
                if (currentShortcut != null && currentShortcut.type != ShortcutType.EMPTY) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    item {
                        ActionButton(
                            text = "このスロットを空にする",
                            color = Color(0xFFE53935),
                            onClick = onClear
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Gray,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun NavigationButton(
    text: String,
    icon: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "→",
                fontSize = 20.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ShortcutListItem(
    shortcut: ShortcutItem,
    subtitle: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // アイコン表示（種類に応じて）
            Text(
                text = when (shortcut.type) {
                    ShortcutType.APP -> "📱"
                    ShortcutType.PHONE -> "📞"
                    ShortcutType.SMS -> "💬"
                    ShortcutType.INTENT -> "🔗"
                    ShortcutType.CALENDAR -> "📅"
                    ShortcutType.MEMO -> "📝"
                    ShortcutType.SETTINGS -> "⚙️"
                    ShortcutType.EMPTY -> ""
                },
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shortcut.label,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}
