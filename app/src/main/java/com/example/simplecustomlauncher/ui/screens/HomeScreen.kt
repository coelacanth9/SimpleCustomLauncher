package com.example.simplecustomlauncher.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.simplecustomlauncher.R
import com.example.simplecustomlauncher.ErrorMessage
import com.example.simplecustomlauncher.HomeHeader
import com.example.simplecustomlauncher.MainScreenState
import com.example.simplecustomlauncher.MainViewModel
import com.example.simplecustomlauncher.ShortcutHelper
import com.example.simplecustomlauncher.data.RowConfig
import com.example.simplecustomlauncher.data.SettingsRepository
import com.example.simplecustomlauncher.data.ShortcutItem
import com.example.simplecustomlauncher.data.ShortcutPlacement
import com.example.simplecustomlauncher.data.ShortcutType
import com.example.simplecustomlauncher.data.TapMode
import com.example.simplecustomlauncher.ui.components.AddPageConfirmDialog
import com.example.simplecustomlauncher.ui.components.AddRowDialog
import com.example.simplecustomlauncher.ui.components.EditModeConfirmDialog
import com.example.simplecustomlauncher.ui.components.PageIndicator
import com.example.simplecustomlauncher.ui.components.PageDeleteConfirmDialog
import com.example.simplecustomlauncher.ui.components.PageResetConfirmDialog
import com.example.simplecustomlauncher.ui.components.PremiumLockOverlay
import com.example.simplecustomlauncher.ui.components.PremiumRequiredForPageDialog
import com.example.simplecustomlauncher.ui.components.ShortcutConfirmDialog
import com.example.simplecustomlauncher.ui.theme.AppTheme

/**
 * 内部機能のラベルをローカライズして取得
 * アプリの場合はPackageManagerから現在のロケールでラベルを取得
 */
@Composable
fun getLocalizedLabel(item: ShortcutItem): String {
    val context = LocalContext.current
    return when (item.type) {
        ShortcutType.CALENDAR -> stringResource(R.string.shortcut_type_calendar)
        ShortcutType.MEMO -> stringResource(R.string.shortcut_type_memo)
        ShortcutType.SETTINGS -> stringResource(R.string.settings)
        ShortcutType.DIALER -> stringResource(R.string.shortcut_type_phone)
        ShortcutType.ALL_APPS -> stringResource(R.string.shortcut_type_all_apps)
        ShortcutType.DATE_DISPLAY -> stringResource(R.string.shortcut_type_date)
        ShortcutType.TIME_DISPLAY -> stringResource(R.string.shortcut_type_time)
        ShortcutType.APP -> {
            // パッケージ名から現在のロケールでアプリ名を取得
            item.packageName?.let { pkg ->
                try {
                    val pm = context.packageManager
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    item.label
                }
            } ?: item.label
        }
        else -> item.label
    }
}

/**
 * タップモードに応じたクリック処理のModifier
 */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.tapModeClickable(
    isEditMode: Boolean,
    tapMode: TapMode,
    onClick: () -> Unit
): Modifier = if (isEditMode) {
    this.clickable(onClick = onClick)
} else {
    when (tapMode) {
        TapMode.SINGLE_TAP -> this.clickable(onClick = onClick)
        TapMode.LONG_TAP -> this.combinedClickable(
            onClick = { /* タップでは何もしない */ },
            onLongClick = onClick
        )
    }
}

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
            snackbarHostState.showSnackbar(error.errorMessage.toDisplayString(context))
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
                onResetPage = { viewModel.showPageResetDialogAction() },
                onDeletePage = if (viewModel.getTotalPageCount() > 1) {
                    { viewModel.showPageDeleteDialogAction() }
                } else null,
                onLayoutEdit = { viewModel.showEditModeConfirmDialog() },
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
            label = getLocalizedLabel(item),
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

    // ページ追加確認ダイアログ
    if (viewModel.showAddPageConfirmDialog) {
        AddPageConfirmDialog(
            onConfirm = { viewModel.confirmAddPageWithRow() },
            onDismiss = { viewModel.dismissAddPageConfirmDialog() }
        )
    }

    // プレミアム誘導ダイアログ（ページ追加時）
    if (viewModel.showPremiumRequiredForPageDialog) {
        val activity = context as? android.app.Activity
        PremiumRequiredForPageDialog(
            onWatchAd = { activity?.let { viewModel.watchAdAndAddPage(it) } },
            onPurchase = { activity?.let { viewModel.purchaseAndAddPage(it) } },
            onDismiss = { viewModel.dismissPremiumRequiredForPageDialog() }
        )
    }

    // ページリセット確認ダイアログ
    if (viewModel.showPageResetDialog) {
        PageResetConfirmDialog(
            onConfirm = { viewModel.confirmPageReset() },
            onDismiss = { viewModel.dismissPageResetDialog() }
        )
    }

    // ページ削除確認ダイアログ
    if (viewModel.showPageDeleteDialog) {
        PageDeleteConfirmDialog(
            onConfirm = { viewModel.confirmPageDelete() },
            onDismiss = { viewModel.dismissPageDeleteDialog() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeContent(
    viewModel: MainViewModel,
    shortcutHelper: ShortcutHelper
) {
    val context = LocalContext.current
    val view = LocalView.current
    val settingsRepository = remember { SettingsRepository(context) }
    val tapMode = settingsRepository.tapMode
    val showConfirmDialog = settingsRepository.showConfirmDialog
    val tapFeedback = settingsRepository.tapFeedback

    // データを読み込み
    val layoutConfig = remember(viewModel.refreshKey) { viewModel.getLayoutConfig() }
    val shortcuts = remember(viewModel.refreshKey) { viewModel.getShortcutsMap() }

    // ページング関連
    val totalPageCount = remember(viewModel.refreshKey) { viewModel.getTotalPageCount() }
    val isPremium = remember(viewModel.refreshKey) { viewModel.isPremiumActive() }
    val loopEnabled = remember(viewModel.refreshKey) { viewModel.isLoopPagingEnabled() }

    // ループページング用のページ数計算
    // 安全な範囲でループを実現（1000周分）
    val loopMultiplier = 1000
    val effectivePageCount = if (loopEnabled && totalPageCount > 1) {
        totalPageCount * loopMultiplier
    } else {
        totalPageCount
    }
    val initialPage = if (loopEnabled && totalPageCount > 1) {
        // 中央付近から開始（500周目のページ0）+ 現在のページ
        totalPageCount * (loopMultiplier / 2) + viewModel.currentPageIndex
    } else {
        viewModel.currentPageIndex.coerceIn(0, maxOf(0, totalPageCount - 1))
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { effectivePageCount }
    )

    // 現在のページをViewModelに同期
    LaunchedEffect(pagerState.currentPage, totalPageCount, loopEnabled) {
        val actualPage = if (loopEnabled && totalPageCount > 1) {
            pagerState.currentPage % totalPageCount
        } else {
            pagerState.currentPage.coerceIn(0, maxOf(0, totalPageCount - 1))
        }
        viewModel.setCurrentPage(actualPage)
    }

    // ページ遷移リクエストを処理
    LaunchedEffect(viewModel.navigateToPageRequest) {
        viewModel.navigateToPageRequest?.let { targetPage ->
            val targetPagerPage = if (loopEnabled && totalPageCount > 1) {
                // ループ時は現在位置からの相対位置で計算
                val currentActual = pagerState.currentPage % totalPageCount
                pagerState.currentPage + (targetPage - currentActual)
            } else {
                targetPage
            }
            pagerState.animateScrollToPage(targetPagerPage)
            viewModel.clearNavigateToPageRequest()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        // ページインジケーター（2ページ以上の場合のみ表示）
        if (totalPageCount > 1) {
            val currentActualPage = if (loopEnabled && totalPageCount > 1) {
                pagerState.currentPage % totalPageCount
            } else {
                pagerState.currentPage
            }
            PageIndicator(
                pageCount = totalPageCount,
                currentPage = currentActualPage,
                lockedFromPage = if (isPremium) -1 else 1  // 非プレミアム時は2ページ目以降ロック表示
            )
        }

        // HorizontalPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val actualPage = if (loopEnabled && totalPageCount > 1) {
                page % totalPageCount
            } else {
                page
            }

            Box(modifier = Modifier.fillMaxSize()) {
                HomePageContent(
                    viewModel = viewModel,
                    pageIndex = actualPage,
                    layoutConfig = layoutConfig,
                    shortcuts = shortcuts,
                    shortcutHelper = shortcutHelper,
                    tapMode = tapMode,
                    showConfirmDialog = showConfirmDialog,
                    tapFeedback = tapFeedback
                )

                // 非プレミアム時、2ページ目以降は半透明カバー
                if (!isPremium && actualPage > 0) {
                    val activity = context as? android.app.Activity
                    PremiumLockOverlay(
                        onWatchAd = { activity?.let { viewModel.showRewardedAd(it) } },
                        onPurchase = { activity?.let { viewModel.launchPurchase(it) } },
                        formattedPrice = viewModel.getFormattedPrice(),
                        isAdReady = viewModel.isAdReady()
                    )
                }
            }
        }
    }
}

@Composable
private fun HomePageContent(
    viewModel: MainViewModel,
    pageIndex: Int,
    layoutConfig: com.example.simplecustomlauncher.data.HomeLayoutConfig,
    shortcuts: Map<String, ShortcutItem>,
    shortcutHelper: ShortcutHelper,
    tapMode: TapMode,
    showConfirmDialog: Boolean,
    tapFeedback: Boolean
) {
    val context = LocalContext.current
    val view = LocalView.current

    // タップフィードバック処理
    val performFeedback: () -> Unit = {
        if (tapFeedback) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    // このページの配置とレイアウト
    val pageRows = remember(viewModel.refreshKey, pageIndex) {
        layoutConfig.getRowsForPage(pageIndex)
    }
    val placements = remember(viewModel.refreshKey, pageIndex) {
        viewModel.getPlacementsForPage(pageIndex)
    }
    val placementsByRow = placements.groupBy { it.row }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        pageRows.forEach { rowConfig ->
            val rowPlacements = placementsByRow[rowConfig.rowIndex] ?: emptyList()

            // 行内のショートカットタイプから固定高さを計算
            val dynamicFixedHeight = rowConfig.fixedHeightDp ?: run {
                val rowShortcuts = rowPlacements.mapNotNull { shortcuts[it.shortcutId] }
                when {
                    rowShortcuts.any { it.type == ShortcutType.TIME_DISPLAY } -> 80
                    rowShortcuts.any { it.type == ShortcutType.DATE_DISPLAY } -> 56
                    else -> null
                }
            }

            HomeRow(
                rowConfig = rowConfig,
                placements = rowPlacements,
                shortcuts = shortcuts,
                isEditMode = viewModel.isEditMode,
                tapMode = tapMode,
                textOnly = rowConfig.textOnly,
                onShortcutClick = { item ->
                    performFeedback()
                    if (showConfirmDialog) {
                        viewModel.showShortcutConfirmDialog(item)
                    } else {
                        viewModel.launchShortcut(context, item, shortcutHelper)
                    }
                },
                onEmptyClick = { column ->
                    performFeedback()
                    viewModel.navigateToShortcutAdd(pageIndex, rowConfig.rowIndex, column)
                },
                onSlotClickInEditMode = { row, column, currentShortcut ->
                    performFeedback()
                    viewModel.navigateToSlotEdit(pageIndex, row, column, currentShortcut)
                },
                modifier = if (dynamicFixedHeight != null) {
                    Modifier.height(dynamicFixedHeight.dp)
                } else {
                    Modifier.weight(1f)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeRow(
    rowConfig: RowConfig,
    placements: List<ShortcutPlacement>,
    shortcuts: Map<String, ShortcutItem>,
    isEditMode: Boolean,
    tapMode: TapMode,
    textOnly: Boolean = false,
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
                    when (shortcut.type) {
                        ShortcutType.DATE_DISPLAY -> {
                            DateDisplayButton(
                                isEditMode = isEditMode,
                                tapMode = tapMode,
                                onClick = {
                                    if (isEditMode) {
                                        onSlotClickInEditMode(rowConfig.rowIndex, colIndex, shortcut)
                                    }
                                }
                            )
                        }
                        ShortcutType.TIME_DISPLAY -> {
                            TimeDisplayButton(
                                isEditMode = isEditMode,
                                tapMode = tapMode,
                                onClick = {
                                    if (isEditMode) {
                                        onSlotClickInEditMode(rowConfig.rowIndex, colIndex, shortcut)
                                    }
                                }
                            )
                        }
                        else -> {
                            ShortcutButton(
                                item = shortcut,
                                columns = rowConfig.columns,
                                isEditMode = isEditMode,
                                tapMode = tapMode,
                                textOnly = textOnly,
                                backgroundColor = placement?.backgroundColor,
                                textColor = placement?.textColor,
                                onClick = {
                                    if (isEditMode) {
                                        onSlotClickInEditMode(rowConfig.rowIndex, colIndex, shortcut)
                                    } else {
                                        onShortcutClick(shortcut)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    EmptySlotButton(
                        isEditMode = isEditMode,
                        tapMode = tapMode,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutButton(
    item: ShortcutItem,
    columns: Int,
    isEditMode: Boolean,
    tapMode: TapMode,
    textOnly: Boolean = false,
    backgroundColor: String? = null,
    textColor: String? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val shortcutHelper = remember { ShortcutHelper(context) }

    // アプリアイコンを取得
    val appIcon = remember(item.packageName) {
        item.packageName?.let { shortcutHelper.getAppIcon(it) }
    }

    // 背景色を計算
    val cardBackgroundColor = if (backgroundColor != null) {
        try {
            Color(android.graphics.Color.parseColor(backgroundColor))
        } catch (e: Exception) {
            AppTheme.extendedColors.cardBackground
        }
    } else {
        AppTheme.extendedColors.cardBackground
    }

    // 文字色を計算
    val labelColor = if (textColor != null) {
        try {
            Color(android.graphics.Color.parseColor(textColor))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.onSurface
        }
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .tapModeClickable(isEditMode, tapMode, onClick)
            .then(
                if (isEditMode) Modifier.border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
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

            if (textOnly) {
                // 文字のみモード
                val labelSize = when (columns) {
                    1 -> minOf(buttonHeight.value * 0.35f, 40f).sp
                    2 -> minOf(buttonHeight.value * 0.28f, 32f).sp
                    else -> minOf(buttonHeight.value * 0.22f, 24f).sp
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = getLocalizedLabel(item),
                        color = labelColor,
                        fontSize = labelSize,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isEditMode) {
                        Text(
                            text = stringResource(R.string.tap_to_edit),
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = minOf(buttonHeight.value * 0.12f, 14f).sp
                        )
                    }
                }
            } else {
                // 通常モード（アイコン+文字）
                when (columns) {
                    1 -> {
                        // 横並びレイアウト（1列）- アイコンと文字を中央にグループ化
                        val iconSize = minOf(buttonHeight * 0.65f, 80.dp)  // 最大80dp
                        val labelSize = minOf(buttonHeight.value * 0.26f, 32f).sp  // 最大32sp

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            ShortcutIcon(item = item, appIcon = appIcon, size = iconSize)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = getLocalizedLabel(item),
                                    color = labelColor,
                                    fontSize = labelSize,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isEditMode) {
                                    Text(
                                        text = stringResource(R.string.tap_to_edit),
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = minOf(buttonHeight.value * 0.14f, 16f).sp
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // 縦並びレイアウト（2列）
                        val iconSize = minOf(buttonHeight * 0.55f, buttonWidth * 0.65f, 64.dp)  // 最大64dp
                        val labelSize = minOf(buttonHeight.value * 0.18f, 24f).sp  // 最大24sp

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            ShortcutIcon(item = item, appIcon = appIcon, size = iconSize)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = getLocalizedLabel(item),
                                color = labelColor,
                                fontSize = labelSize,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isEditMode) {
                                Text(
                                    text = stringResource(R.string.tap_to_edit),
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = minOf(buttonHeight.value * 0.12f, 14f).sp
                                )
                            }
                        }
                    }
                    else -> {
                        // アイコンのみ（3列以上）
                        val iconSize = minOf(buttonHeight * 0.7f, buttonWidth * 0.8f, 56.dp)  // 最大56dp

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            ShortcutIcon(item = item, appIcon = appIcon, size = iconSize)
                            if (isEditMode) {
                                Text(
                                    text = stringResource(R.string.tap_to_edit),
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
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
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        ShortcutType.CALENDAR -> {
            val calendarIcon = painterResource(id = R.drawable.ic_calendar)
            Icon(calendarIcon, stringResource(R.string.shortcut_type_calendar), Modifier.size(size), AppTheme.extendedColors.iconCalendar)
        }
        ShortcutType.MEMO -> {
            val memoIcon = painterResource(id = R.drawable.ic_memo)
            Icon(memoIcon, stringResource(R.string.shortcut_type_memo), Modifier.size(size), AppTheme.extendedColors.iconMemo)
        }
        ShortcutType.SETTINGS -> {
            Icon(Icons.Default.Settings, stringResource(R.string.settings), Modifier.size(size), MaterialTheme.colorScheme.onSurfaceVariant)
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
                Icon(Icons.Default.Phone, item.label, Modifier.size(size), AppTheme.extendedColors.iconCalendar)
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
                Icon(Icons.Default.Email, item.label, Modifier.size(size), AppTheme.extendedColors.iconMemo)
            }
        }
        ShortcutType.DIALER -> {
            // カスタムキーパッドアイコン
            val iconColor = AppTheme.extendedColors.iconDialer
            val dialerIcon = remember {
                ContextCompat.getDrawable(context, R.drawable.ic_phone_keypad)
            }
            if (dialerIcon != null) {
                val bitmap = remember(dialerIcon) { dialerIcon.toBitmap(128, 128) }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = item.label,
                    modifier = Modifier.size(size),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(iconColor)
                )
            } else {
                Icon(Icons.Default.Phone, item.label, Modifier.size(size), iconColor)
            }
        }
        ShortcutType.ALL_APPS -> {
            Icon(Icons.Default.Apps, stringResource(R.string.shortcut_type_all_apps), Modifier.size(size), AppTheme.extendedColors.iconAllApps)
        }
        ShortcutType.DATE_DISPLAY -> { /* 専用コンポーネントで表示 */ }
        ShortcutType.TIME_DISPLAY -> { /* 専用コンポーネントで表示 */ }
        ShortcutType.EMPTY -> { }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmptySlotButton(
    isEditMode: Boolean,
    tapMode: TapMode,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .tapModeClickable(isEditMode, tapMode, onClick)
            .then(
                if (isEditMode) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add),
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * 日付表示ボタン
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DateDisplayButton(
    isEditMode: Boolean,
    tapMode: TapMode,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { com.example.simplecustomlauncher.CalendarRepository(context) }

    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }

    // 1分ごとに更新（日付なので秒単位は不要）
    LaunchedEffect(Unit) {
        while (true) {
            currentDateTime = LocalDateTime.now()
            delay(60_000)
        }
    }

    val datePattern = stringResource(R.string.date_format)
    val dateFormatter = DateTimeFormatter.ofPattern(datePattern, Locale.getDefault())

    // 祝日名を取得
    val holidayName = remember(currentDateTime.toLocalDate()) {
        repository.getHolidayName(currentDateTime.toLocalDate())
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isEditMode) Modifier
                    .tapModeClickable(true, tapMode, onClick)
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(16.dp)
                    )
                else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = AppTheme.extendedColors.cardBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentDateTime.format(dateFormatter),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (holidayName != null) {
                    Text(
                        text = holidayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (isEditMode) {
                    Text(
                        text = stringResource(R.string.tap_to_edit),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

/**
 * 時計表示ボタン
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimeDisplayButton(
    isEditMode: Boolean,
    tapMode: TapMode,
    onClick: () -> Unit
) {
    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }

    // 1秒ごとに更新
    LaunchedEffect(Unit) {
        while (true) {
            currentDateTime = LocalDateTime.now()
            delay(1000)
        }
    }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Card(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isEditMode) Modifier
                    .tapModeClickable(true, tapMode, onClick)
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(16.dp)
                    )
                else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = AppTheme.extendedColors.cardBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentDateTime.format(timeFormatter),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isEditMode) {
                    Text(
                        text = stringResource(R.string.tap_to_edit),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
