package com.example.simplecustomlauncher.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.simplecustomlauncher.R
import com.example.simplecustomlauncher.HomeHeader
import com.example.simplecustomlauncher.MainScreenState
import com.example.simplecustomlauncher.MainViewModel
import com.example.simplecustomlauncher.ShortcutHelper
import com.example.simplecustomlauncher.data.RowConfig
import com.example.simplecustomlauncher.data.ShortcutItem
import com.example.simplecustomlauncher.data.ShortcutPlacement
import com.example.simplecustomlauncher.data.ShortcutType
import com.example.simplecustomlauncher.ui.components.AddRowDialog
import com.example.simplecustomlauncher.ui.components.EditModeConfirmDialog
import com.example.simplecustomlauncher.ui.components.ShortcutConfirmDialog

/**
 * ホーム画面
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val shortcutHelper = remember { ShortcutHelper(context) }

    // エラー表示
    LaunchedEffect(viewModel.errorEvent) {
        viewModel.errorEvent?.let { error ->
            snackbarHostState.showSnackbar(error.message)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            HomeHeader(
                context = context,
                isEditMode = viewModel.isEditMode,
                onEditDone = { viewModel.exitEditMode() },
                onAddRow = { viewModel.showAddRowDialogAction() },
                onAppSettings = { viewModel.navigateTo(MainScreenState.AppSettings) }
            )

            // ホームコンテンツ
            Box(modifier = Modifier.weight(1f)) {
                HomeContent(
                    viewModel = viewModel,
                    shortcutHelper = shortcutHelper
                )
            }
        }
    }

    // 編集モード確認ダイアログ
    if (viewModel.showEditModeDialog) {
        EditModeConfirmDialog(
            onConfirm = { viewModel.enterEditMode() },
            onDismiss = { viewModel.dismissEditModeDialog() }
        )
    }

    // ショートカット実行確認ダイアログ
    viewModel.shortcutToConfirm?.let { item ->
        ShortcutConfirmDialog(
            label = item.label,
            onConfirm = {
                viewModel.launchShortcut(context, item, shortcutHelper)
                viewModel.dismissShortcutConfirmDialog()
            },
            onDismiss = { viewModel.dismissShortcutConfirmDialog() }
        )
    }

    // 行追加ダイアログ
    if (viewModel.showAddRowDialog) {
        AddRowDialog(
            onAddRow = { columns -> viewModel.addRow(columns) },
            onDismiss = { viewModel.dismissAddRowDialog() }
        )
    }
}

@Composable
private fun HomeContent(
    viewModel: MainViewModel,
    shortcutHelper: ShortcutHelper
) {
    val context = LocalContext.current

    // データを読み込み
    val layoutConfig = remember(viewModel.refreshKey) { viewModel.getLayoutConfig() }
    val shortcuts = remember(viewModel.refreshKey) { viewModel.getShortcutsMap() }
    val placements = remember(viewModel.refreshKey) { viewModel.getAllPlacements() }

    // 行ごとにグループ化
    val placementsByRow = placements.groupBy { it.row }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        layoutConfig.rows.forEach { rowConfig ->
            HomeRow(
                rowConfig = rowConfig,
                placements = placementsByRow[rowConfig.rowIndex] ?: emptyList(),
                shortcuts = shortcuts,
                isEditMode = viewModel.isEditMode,
                onShortcutClick = { item ->
                    when (item.type) {
                        ShortcutType.CALENDAR -> viewModel.navigateTo(MainScreenState.Calendar)
                        ShortcutType.MEMO -> viewModel.navigateTo(MainScreenState.Memo)
                        ShortcutType.SETTINGS -> viewModel.navigateTo(MainScreenState.AppSettings)
                        else -> {
                            if (viewModel.showConfirmDialog) {
                                viewModel.showShortcutConfirmDialog(item)
                            } else {
                                viewModel.launchShortcut(context, item, shortcutHelper)
                            }
                        }
                    }
                },
                onEmptyClick = { column ->
                    viewModel.navigateToShortcutAdd(rowConfig.rowIndex, column)
                },
                onSlotClickInEditMode = { row, column, currentShortcut ->
                    viewModel.navigateToSlotEdit(row, column, currentShortcut)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HomeRow(
    rowConfig: RowConfig,
    placements: List<ShortcutPlacement>,
    shortcuts: Map<String, ShortcutItem>,
    isEditMode: Boolean,
    onShortcutClick: (ShortcutItem) -> Unit,
    onEmptyClick: (column: Int) -> Unit,
    onSlotClickInEditMode: (row: Int, column: Int, currentShortcut: ShortcutItem?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (colIndex in 0 until rowConfig.columns) {
            val placement = placements.find { it.column == colIndex }
            val shortcut = placement?.let { shortcuts[it.shortcutId] }

            Box(modifier = Modifier.weight(1f)) {
                if (shortcut != null && shortcut.type != ShortcutType.EMPTY) {
                    ShortcutButton(
                        item = shortcut,
                        columns = rowConfig.columns,
                        isEditMode = isEditMode,
                        onClick = {
                            if (isEditMode) {
                                onSlotClickInEditMode(rowConfig.rowIndex, colIndex, shortcut)
                            } else {
                                onShortcutClick(shortcut)
                            }
                        }
                    )
                } else {
                    EmptySlotButton(
                        isEditMode = isEditMode,
                        onClick = {
                            if (isEditMode) {
                                onSlotClickInEditMode(rowConfig.rowIndex, colIndex, null)
                            } else {
                                onEmptyClick(colIndex)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortcutButton(
    item: ShortcutItem,
    columns: Int,
    isEditMode: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val shortcutHelper = remember { ShortcutHelper(context) }

    // アプリアイコンを取得
    val appIcon = remember(item.packageName) {
        item.packageName?.let { shortcutHelper.getAppIcon(it) }
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .then(
                if (isEditMode) Modifier.border(
                    width = 3.dp,
                    color = Color(0xFFFF9800),
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // ボタンサイズから動的にアイコン・文字サイズを計算
            val buttonHeight = maxHeight
            val buttonWidth = maxWidth

            when (columns) {
                1 -> {
                    // 横並びレイアウト（1列）
                    val iconSize = buttonHeight * 0.55f
                    val labelSize = (buttonHeight.value * 0.22f).sp

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier.weight(0.3f),
                            contentAlignment = Alignment.Center
                        ) {
                            ShortcutIcon(item = item, appIcon = appIcon, size = iconSize)
                        }
                        Box(
                            modifier = Modifier.weight(0.7f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text(
                                    text = item.label,
                                    color = Color(0xFF333333),
                                    fontSize = labelSize,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isEditMode) {
                                    Text(
                                        text = "タップで編集",
                                        color = Color(0xFFFF9800),
                                        fontSize = (buttonHeight.value * 0.12f).sp
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // 縦並びレイアウト（2列）
                    val iconSize = minOf(buttonHeight * 0.5f, buttonWidth * 0.6f)
                    val labelSize = (buttonHeight.value * 0.14f).sp

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ShortcutIcon(item = item, appIcon = appIcon, size = iconSize)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.label,
                            color = Color(0xFF333333),
                            fontSize = labelSize,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isEditMode) {
                            Text(
                                text = "編集",
                                color = Color(0xFFFF9800),
                                fontSize = (buttonHeight.value * 0.10f).sp
                            )
                        }
                    }
                }
                else -> {
                    // アイコンのみ（3列以上）
                    val iconSize = minOf(buttonHeight * 0.6f, buttonWidth * 0.7f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ShortcutIcon(item = item, appIcon = appIcon, size = iconSize)
                        if (isEditMode) {
                            Text(
                                text = "編集",
                                color = Color(0xFFFF9800),
                                fontSize = (buttonHeight.value * 0.10f).sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutIcon(
    item: ShortcutItem,
    appIcon: android.graphics.drawable.Drawable?,
    size: Dp
) {
    val context = LocalContext.current
    val shortcutHelper = remember { ShortcutHelper(context) }

    when (item.type) {
        ShortcutType.APP, ShortcutType.INTENT -> {
            if (appIcon != null) {
                val bitmap = remember(appIcon) { appIcon.toBitmap(128, 128) }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = item.label,
                    modifier = Modifier.size(size)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = item.label,
                    modifier = Modifier.size(size),
                    tint = Color(0xFF1976D2)
                )
            }
        }
        ShortcutType.CALENDAR -> {
            Icon(Icons.Default.DateRange, "カレンダー", Modifier.size(size), Color(0xFF4CAF50))
        }
        ShortcutType.MEMO -> {
            Icon(Icons.Default.Edit, "メモ帳", Modifier.size(size), Color(0xFFFF9800))
        }
        ShortcutType.SETTINGS -> {
            Icon(Icons.Default.Settings, "設定", Modifier.size(size), Color(0xFF757575))
        }
        ShortcutType.PHONE -> {
            // 電話アプリのアイコンを取得
            val phoneIcon = remember {
                shortcutHelper.getAppIcon("com.google.android.dialer")
                    ?: shortcutHelper.getAppIcon("com.android.dialer")
            }
            if (phoneIcon != null) {
                val bitmap = remember(phoneIcon) { phoneIcon.toBitmap(128, 128) }
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = item.label, modifier = Modifier.size(size))
            } else {
                Icon(Icons.Default.Phone, item.label, Modifier.size(size), Color(0xFF4CAF50))
            }
        }
        ShortcutType.SMS -> {
            // SMSアプリのアイコンを取得
            val smsIcon = remember {
                shortcutHelper.getAppIcon("com.google.android.apps.messaging")
                    ?: shortcutHelper.getAppIcon("com.android.mms")
            }
            if (smsIcon != null) {
                val bitmap = remember(smsIcon) { smsIcon.toBitmap(128, 128) }
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = item.label, modifier = Modifier.size(size))
            } else {
                Icon(Icons.Default.Email, item.label, Modifier.size(size), Color(0xFF2196F3))
            }
        }
        ShortcutType.DIALER -> {
            // カスタムキーパッドアイコン
            val dialerIcon = remember {
                ContextCompat.getDrawable(context, R.drawable.ic_phone_keypad)
            }
            if (dialerIcon != null) {
                val bitmap = remember(dialerIcon) { dialerIcon.toBitmap(128, 128) }
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = item.label, modifier = Modifier.size(size))
            } else {
                Icon(Icons.Default.Phone, item.label, Modifier.size(size), Color(0xFF4CAF50))
            }
        }
        ShortcutType.EMPTY -> { }
    }
}

@Composable
private fun EmptySlotButton(
    isEditMode: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .then(
                if (isEditMode) Modifier.border(
                    width = 2.dp,
                    color = Color(0xFFFF9800),
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "追加",
                modifier = Modifier.size(36.dp),
                tint = Color(0xFFBDBDBD)
            )
        }
    }
}

/**
 * ページインジケーター
 */
@Composable
fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (index == currentPage) 12.dp else 8.dp)
                    .background(
                        color = if (index == currentPage) Color(0xFF1976D2) else Color.LightGray,
                        shape = CircleShape
                    )
            )
        }
    }
}
