package com.example.simplecustomlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simplecustomlauncher.ui.theme.SimpleCustomLauncherTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.simplecustomlauncher.PermissionManager.CALENDAR_PERMISSIONS
import com.example.simplecustomlauncher.data.ShortcutRepository
import com.example.simplecustomlauncher.data.ShortcutItem
import com.example.simplecustomlauncher.data.ShortcutPlacement
import com.example.simplecustomlauncher.data.ShortcutType
import com.example.simplecustomlauncher.data.HomeLayoutConfig
import android.graphics.drawable.Drawable
import android.util.Log
import android.content.Intent
import android.content.pm.LauncherApps
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.ui.input.pointer.pointerInput
import java.util.UUID
import com.example.simplecustomlauncher.data.SettingsRepository
import com.example.simplecustomlauncher.data.RowConfig
import com.example.simplecustomlauncher.ui.components.ShortcutConfirmDialog
import com.example.simplecustomlauncher.ui.components.EditModeConfirmDialog
import com.example.simplecustomlauncher.ui.components.AddRowDialog
import com.example.simplecustomlauncher.ui.screens.ShortcutAddScreen
import com.example.simplecustomlauncher.ui.screens.SlotEditScreen
import com.example.simplecustomlauncher.ui.screens.InternalFeature
import com.example.simplecustomlauncher.ui.screens.CalendarFullScreen
import com.example.simplecustomlauncher.ui.screens.AppSettingsScreen
import com.example.simplecustomlauncher.ui.screens.MemoScreen

class MainActivity : ComponentActivity() {

    private lateinit var shortcutRepository: ShortcutRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shortcutRepository = ShortcutRepository(this)

        // 起動時のIntentを処理
        handleIntent(intent)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainLauncherScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        Log.d("MainActivity", "handleIntent: action=${intent.action}, extras=${intent.extras?.keySet()}")

        // PinShortcutリクエストの処理
        if (intent.hasExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST)) {
            handlePinShortcut(intent)
        }
    }

    private fun handlePinShortcut(intent: Intent) {
        val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
        val request = launcherApps.getPinItemRequest(intent)

        if (request == null) {
            Log.e("MainActivity", "PinItemRequest is null")
            return
        }

        if (request.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            val shortcutInfo = request.shortcutInfo
            if (shortcutInfo != null) {
                Log.d("MainActivity", "Pin shortcut: ${shortcutInfo.shortLabel}, package: ${shortcutInfo.`package`}")

                val item = ShortcutItem(
                    id = UUID.randomUUID().toString(),
                    type = ShortcutType.INTENT,
                    label = shortcutInfo.shortLabel?.toString() ?: "ショートカット",
                    packageName = shortcutInfo.`package`
                )

                // ショートカット情報を保存（後で起動時に使う）
                savePinShortcutInfo(item.id, shortcutInfo.id, shortcutInfo.`package`)

                // 一時保管として保存（自動配置しない）
                shortcutRepository.saveShortcut(item)
                request.accept()
                Toast.makeText(this, "「${item.label}」を一時保管に追加しました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePinShortcutInfo(itemId: String, shortcutId: String, packageName: String) {
        val prefs = getSharedPreferences("pin_shortcuts", MODE_PRIVATE)
        prefs.edit()
            .putString("${itemId}_shortcut_id", shortcutId)
            .putString("${itemId}_package", packageName)
            .apply()
    }
}

/**
 * メイン画面の状態
 */
sealed class MainScreenState {
    object Home : MainScreenState()
    object ShortcutAdd : MainScreenState()  // 通常モードで＋タップ
    data class SlotEdit(                     // 編集モードでスロットタップ
        val row: Int,
        val column: Int,
        val currentShortcut: ShortcutItem?
    ) : MainScreenState()
    object Calendar : MainScreenState()     // カレンダー全画面
    object Memo : MainScreenState()         // メモ帳全画面
    object AppSettings : MainScreenState()  // アプリ設定画面
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainLauncherScreen() {
    val context = LocalContext.current

    // 権限状態の管理
    var hasPermission by remember {
        mutableStateOf(PermissionManager.checkPermissions(context, CALENDAR_PERMISSIONS))
    }

    // 画面状態
    var screenState by remember { mutableStateOf<MainScreenState>(MainScreenState.Home) }

    // ShortcutHelper & Repository
    val shortcutHelper = remember { ShortcutHelper(context) }
    val shortcutRepository = remember { ShortcutRepository(context) }
    val settingsRepository = remember { SettingsRepository(context) }

    // ショートカットデータの再読み込みトリガー
    var shortcutRefreshKey by remember { mutableStateOf(0) }

    // 編集モード
    var isEditMode by remember { mutableStateOf(false) }

    // 確認ダイアログの状態
    var showEditModeDialog by remember { mutableStateOf(false) }
    var shortcutToConfirm by remember { mutableStateOf<ShortcutItem?>(null) }
    var showAddRowDialog by remember { mutableStateOf(false) }

    // 追加時のターゲットスロット（通常モードで＋タップ後にアプリ選択した時用）
    var targetSlot by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // 起動時に権限を要求
    RequestPermissions(
        context = context,
        permissions = CALENDAR_PERMISSIONS,
        onResult = { isGranted -> hasPermission = isGranted }
    )

    // 祝日データの取得（権限が許可されたら再実行）
    val calendarRepository = remember { CalendarRepository(context) }
    val holidayMap = remember(hasPermission) {
        if (hasPermission) {
            val now = LocalDate.now()
            calendarRepository.getHolidaysForMonth(now.year, now.monthValue)
        } else {
            emptyMap()
        }
    }

    // ショートカットデータ
    val allShortcuts = remember(shortcutRefreshKey) {
        shortcutRepository.getAllShortcuts().values.toList()
    }
    val placements = remember(shortcutRefreshKey) {
        shortcutRepository.getAllPlacements()
    }
    val placedIds = placements.map { it.shortcutId }.toSet()
    val unplacedShortcuts = allShortcuts.filter {
        it.id !in placedIds && it.type != ShortcutType.EMPTY
    }
    val placedShortcuts = allShortcuts.filter {
        it.id in placedIds && it.type != ShortcutType.EMPTY
    }

    // 画面状態による表示切り替え
    when (val state = screenState) {
        is MainScreenState.Home -> {
            // メインホーム画面
            val pageCount = 1 // TODO: 設定から取得

            Scaffold { paddingValues ->
                Column(modifier = Modifier.padding(paddingValues)) {
                    HomeHeader(
                        context = context,
                        isEditMode = isEditMode,
                        onEditDone = {
                            isEditMode = false
                            shortcutRefreshKey++
                        },
                        onAddRow = {
                            showAddRowDialog = true
                        },
                        onAppSettings = {
                            screenState = MainScreenState.AppSettings
                        }
                    )

                    // ホームコンテンツ（ページャーは将来的に複数ページ対応時に使う）
                    Box(modifier = Modifier.weight(1f)) {
                        HomeContent(
                            repository = shortcutRepository,
                            refreshKey = shortcutRefreshKey,
                            isEditMode = isEditMode,
                            showConfirmDialog = settingsRepository.showConfirmDialog,
                            onAddShortcutClick = { row, column ->
                                targetSlot = row to column
                                screenState = MainScreenState.ShortcutAdd
                            },
                            onShortcutClick = { item ->
                                // アプリ内機能は画面遷移
                                when (item.type) {
                                    ShortcutType.CALENDAR -> {
                                        screenState = MainScreenState.Calendar
                                    }
                                    ShortcutType.MEMO -> {
                                        screenState = MainScreenState.Memo
                                    }
                                    ShortcutType.SETTINGS -> {
                                        screenState = MainScreenState.AppSettings
                                    }
                                    else -> {
                                        if (settingsRepository.showConfirmDialog) {
                                            shortcutToConfirm = item
                                        } else {
                                            launchShortcut(context, item, shortcutHelper)
                                        }
                                    }
                                }
                            },
                            onShortcutLongClick = { _ ->
                                showEditModeDialog = true
                            },
                            onSlotClickInEditMode = { row, column, currentShortcut ->
                                screenState = MainScreenState.SlotEdit(row, column, currentShortcut)
                            }
                        )
                    }

                    // ページインジケーター（複数ページの場合のみ表示）
                    if (pageCount > 1) {
                        PageIndicator(
                            pageCount = pageCount,
                            currentPage = 0, // TODO: 現在のページ
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            // 編集モード確認ダイアログ
            if (showEditModeDialog) {
                EditModeConfirmDialog(
                    onConfirm = {
                        isEditMode = true
                        showEditModeDialog = false
                    },
                    onDismiss = { showEditModeDialog = false }
                )
            }

            // ショートカット実行確認ダイアログ
            shortcutToConfirm?.let { item ->
                ShortcutConfirmDialog(
                    label = item.label,
                    onConfirm = {
                        launchShortcut(context, item, shortcutHelper)
                        shortcutToConfirm = null
                    },
                    onDismiss = { shortcutToConfirm = null }
                )
            }

            // 行追加ダイアログ
            if (showAddRowDialog) {
                AddRowDialog(
                    onAddRow = { columns ->
                        // 新しい行を追加
                        val currentConfig = shortcutRepository.getLayoutConfig()
                        val newRowIndex = currentConfig.rows.maxOfOrNull { it.rowIndex }?.plus(1) ?: 0
                        val newRows = currentConfig.rows + RowConfig(rowIndex = newRowIndex, columns = columns)
                        shortcutRepository.saveLayoutConfig(HomeLayoutConfig(rows = newRows))
                        shortcutRefreshKey++
                        showAddRowDialog = false
                    },
                    onDismiss = { showAddRowDialog = false }
                )
            }
        }

        is MainScreenState.ShortcutAdd -> {
            // ショートカット追加画面（通常モードで＋タップ）
            ShortcutAddScreen(
                unplacedShortcuts = unplacedShortcuts,
                onSelectUnplaced = { shortcut ->
                    // 未配置ショートカットを選択 → ターゲットスロットに配置
                    targetSlot?.let { (row, col) ->
                        shortcutRepository.savePlacement(
                            ShortcutPlacement(shortcutId = shortcut.id, row = row, column = col)
                        )
                    }
                    shortcutRefreshKey++
                    targetSlot = null
                    screenState = MainScreenState.Home
                },
                onSelectInternal = { feature ->
                    // アプリ内機能を選択 → 既存があれば再利用、なければ新規作成
                    val item = findOrCreateInternalShortcut(
                        allShortcuts, shortcutRepository, feature.type, feature.label
                    )
                    targetSlot?.let { (row, col) ->
                        shortcutRepository.savePlacement(
                            ShortcutPlacement(shortcutId = item.id, row = row, column = col)
                        )
                    }
                    shortcutRefreshKey++
                    targetSlot = null
                    screenState = MainScreenState.Home
                },
                onSelectApp = { app ->
                    // アプリを選択 → 既存があれば再利用、なければ新規作成
                    val item = findOrCreateAppShortcut(
                        allShortcuts, shortcutRepository, app.packageName, app.label
                    )
                    targetSlot?.let { (row, col) ->
                        shortcutRepository.savePlacement(
                            ShortcutPlacement(shortcutId = item.id, row = row, column = col)
                        )
                    }
                    shortcutRefreshKey++
                    targetSlot = null
                    screenState = MainScreenState.Home
                },
                onSelectShortcut = { shortcut ->
                    // ショートカットを選択 → 新規作成してターゲットスロットに配置
                    val item = ShortcutItem(
                        id = UUID.randomUUID().toString(),
                        type = ShortcutType.INTENT,
                        label = shortcut.shortLabel,
                        packageName = shortcut.packageName
                    )
                    shortcutRepository.saveShortcut(item)
                    // ショートカット起動に必要な情報を保存
                    saveShortcutInfo(context, item.id, shortcut.id, shortcut.packageName)
                    targetSlot?.let { (row, col) ->
                        shortcutRepository.savePlacement(
                            ShortcutPlacement(shortcutId = item.id, row = row, column = col)
                        )
                    }
                    shortcutRefreshKey++
                    targetSlot = null
                    screenState = MainScreenState.Home
                },
                onBack = {
                    targetSlot = null
                    screenState = MainScreenState.Home
                }
            )
        }

        is MainScreenState.SlotEdit -> {
            // スロット編集画面（編集モードでスロットタップ）
            val otherPlacedShortcuts = placedShortcuts.filter { it.id != state.currentShortcut?.id }

            SlotEditScreen(
                currentShortcut = state.currentShortcut,
                unplacedShortcuts = unplacedShortcuts,
                placedShortcuts = otherPlacedShortcuts,
                onSelectUnplaced = { shortcut ->
                    // 未配置ショートカットを選択 → このスロットに配置
                    shortcutRepository.savePlacement(
                        ShortcutPlacement(shortcutId = shortcut.id, row = state.row, column = state.column)
                    )
                    shortcutRefreshKey++
                    screenState = MainScreenState.Home
                },
                onSelectPlaced = { shortcut ->
                    // 配置済みショートカットを選択 → 入れ替え
                    val existingPlacement = placements.find { it.shortcutId == shortcut.id }
                    if (existingPlacement != null) {
                        if (state.currentShortcut != null) {
                            // 現在のショートカットを相手の位置に移動
                            shortcutRepository.savePlacement(
                                ShortcutPlacement(
                                    shortcutId = state.currentShortcut.id,
                                    row = existingPlacement.row,
                                    column = existingPlacement.column
                                )
                            )
                        } else {
                            // 現在のスロットが空の場合、相手の配置を削除
                            shortcutRepository.removePlacement(shortcut.id)
                        }
                    }
                    // 選択したショートカットをこのスロットに配置
                    shortcutRepository.savePlacement(
                        ShortcutPlacement(shortcutId = shortcut.id, row = state.row, column = state.column)
                    )
                    shortcutRefreshKey++
                    screenState = MainScreenState.Home
                },
                onSelectApp = { app ->
                    // アプリを選択 → 既存があれば再利用、なければ新規作成
                    val item = findOrCreateAppShortcut(
                        allShortcuts, shortcutRepository, app.packageName, app.label
                    )
                    shortcutRepository.savePlacement(
                        ShortcutPlacement(shortcutId = item.id, row = state.row, column = state.column)
                    )
                    shortcutRefreshKey++
                    screenState = MainScreenState.Home
                },
                onSelectShortcut = { shortcut ->
                    // ショートカットを選択 → 新規作成してこのスロットに配置
                    // INTENTタイプは同じ連絡先でもshortcutIdが異なる可能性があるので常に新規作成
                    val item = ShortcutItem(
                        id = UUID.randomUUID().toString(),
                        type = ShortcutType.INTENT,
                        label = shortcut.shortLabel,
                        packageName = shortcut.packageName
                    )
                    shortcutRepository.saveShortcut(item)
                    // ショートカット起動に必要な情報を保存
                    saveShortcutInfo(context, item.id, shortcut.id, shortcut.packageName)
                    shortcutRepository.savePlacement(
                        ShortcutPlacement(shortcutId = item.id, row = state.row, column = state.column)
                    )
                    shortcutRefreshKey++
                    screenState = MainScreenState.Home
                },
                onSelectInternal = { feature ->
                    // アプリ内機能を選択 → 既存があれば再利用、なければ新規作成
                    val item = findOrCreateInternalShortcut(
                        allShortcuts, shortcutRepository, feature.type, feature.label
                    )
                    shortcutRepository.savePlacement(
                        ShortcutPlacement(shortcutId = item.id, row = state.row, column = state.column)
                    )
                    shortcutRefreshKey++
                    screenState = MainScreenState.Home
                },
                onClear = {
                    // このスロットを空にする
                    state.currentShortcut?.let { current ->
                        shortcutRepository.removePlacement(current.id)
                    }
                    shortcutRefreshKey++
                    screenState = MainScreenState.Home
                },
                onDeleteRow = {
                    // この行全体を削除する
                    val rowToDelete = state.row
                    // この行の配置を全て削除
                    val currentPlacements = shortcutRepository.getAllPlacements()
                    currentPlacements.filter { it.row == rowToDelete }.forEach {
                        shortcutRepository.removePlacement(it.shortcutId)
                    }
                    // レイアウトから行を削除
                    val currentConfig = shortcutRepository.getLayoutConfig()
                    val newRows = currentConfig.rows.filter { it.rowIndex != rowToDelete }
                    shortcutRepository.saveLayoutConfig(HomeLayoutConfig(rows = newRows))
                    shortcutRefreshKey++
                    screenState = MainScreenState.Home
                },
                onBack = {
                    screenState = MainScreenState.Home
                }
            )
        }

        is MainScreenState.Calendar -> {
            // カレンダー全画面
            CalendarFullScreen(
                hasPermission = hasPermission,
                holidayMap = holidayMap,
                onBack = { screenState = MainScreenState.Home }
            )
        }

        is MainScreenState.Memo -> {
            // メモ帳全画面
            MemoScreen(
                onBack = { screenState = MainScreenState.Home }
            )
        }

        is MainScreenState.AppSettings -> {
            // アプリ設定画面（TODO: 実装）
            AppSettingsScreen(
                onBack = { screenState = MainScreenState.Home }
            )
        }
    }
}

/**
 * ショートカットを起動
 */
private fun launchShortcut(
    context: android.content.Context,
    item: ShortcutItem,
    shortcutHelper: ShortcutHelper
) {
    when (item.type) {
        ShortcutType.APP -> {
            item.packageName?.let { shortcutHelper.startApp(it) }
        }
        ShortcutType.INTENT -> {
            // まずintentUriがあればそれを使う
            val intent = item.toIntent()
            if (intent != null) {
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "起動できませんでした", Toast.LENGTH_SHORT).show()
                }
            } else {
                // PinShortcutの場合はLauncherAppsで起動
                launchPinShortcut(context, item)
            }
        }
        ShortcutType.PHONE -> {
            item.toIntent()?.let { intent ->
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "電話を起動できませんでした", Toast.LENGTH_SHORT).show()
                }
            }
        }
        ShortcutType.SMS -> {
            item.toIntent()?.let { intent ->
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "SMSを起動できませんでした", Toast.LENGTH_SHORT).show()
                }
            }
        }
        ShortcutType.CALENDAR -> { /* 画面遷移で処理するのでここでは何もしない */ }
        ShortcutType.MEMO -> { /* 画面遷移で処理するのでここでは何もしない */ }
        ShortcutType.SETTINGS -> { /* 画面遷移で処理するのでここでは何もしない */ }
        ShortcutType.EMPTY -> { /* 何もしない */ }
    }
}

private fun launchPinShortcut(context: android.content.Context, item: ShortcutItem) {
    val prefs = context.getSharedPreferences("pin_shortcuts", android.content.Context.MODE_PRIVATE)
    val shortcutId = prefs.getString("${item.id}_shortcut_id", null)
    val packageName = prefs.getString("${item.id}_package", null)

    if (shortcutId != null && packageName != null) {
        try {
            val launcherApps = context.getSystemService(android.content.Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            launcherApps.startShortcut(
                packageName,
                shortcutId,
                null,
                null,
                android.os.Process.myUserHandle()
            )
        } catch (e: Exception) {
            Log.e("launchPinShortcut", "Failed to launch shortcut", e)
            Toast.makeText(context, "起動できませんでした", Toast.LENGTH_SHORT).show()
        }
    } else {
        Log.e("launchPinShortcut", "Shortcut info not found for ${item.id}")
        Toast.makeText(context, "ショートカット情報が見つかりません", Toast.LENGTH_SHORT).show()
    }
}

/**
 * ショートカット起動に必要な情報を保存
 */
private fun saveShortcutInfo(context: android.content.Context, itemId: String, shortcutId: String, packageName: String) {
    val prefs = context.getSharedPreferences("pin_shortcuts", android.content.Context.MODE_PRIVATE)
    prefs.edit()
        .putString("${itemId}_shortcut_id", shortcutId)
        .putString("${itemId}_package", packageName)
        .apply()
}

/**
 * 既存のショートカットを検索、なければ新規作成
 */
private fun findOrCreateAppShortcut(
    allShortcuts: List<ShortcutItem>,
    repository: ShortcutRepository,
    packageName: String,
    label: String
): ShortcutItem {
    // 同じパッケージ名のAPPタイプがあれば再利用
    val existing = allShortcuts.find {
        it.type == ShortcutType.APP && it.packageName == packageName
    }
    if (existing != null) return existing

    // なければ新規作成
    val item = ShortcutItem(
        id = UUID.randomUUID().toString(),
        type = ShortcutType.APP,
        label = label,
        packageName = packageName
    )
    repository.saveShortcut(item)
    return item
}

/**
 * 既存の内部機能ショートカットを検索、なければ新規作成
 */
private fun findOrCreateInternalShortcut(
    allShortcuts: List<ShortcutItem>,
    repository: ShortcutRepository,
    type: ShortcutType,
    label: String
): ShortcutItem {
    // 同じタイプがあれば再利用
    val existing = allShortcuts.find { it.type == type }
    if (existing != null) return existing

    // なければ新規作成
    val item = ShortcutItem(
        id = UUID.randomUUID().toString(),
        type = type,
        label = label
    )
    repository.saveShortcut(item)
    return item
}

@Composable
fun HomeContent(
    repository: ShortcutRepository,
    refreshKey: Int,
    isEditMode: Boolean,
    showConfirmDialog: Boolean,
    onAddShortcutClick: (row: Int, column: Int) -> Unit,
    onShortcutClick: (ShortcutItem) -> Unit,
    onShortcutLongClick: (ShortcutItem?) -> Unit,
    onSlotClickInEditMode: (row: Int, column: Int, currentShortcut: ShortcutItem?) -> Unit
) {
    // データを読み込み
    val layoutConfig = remember(refreshKey) { repository.getLayoutConfig() }
    val shortcuts = remember(refreshKey) { repository.getAllShortcuts() }
    val placements = remember(refreshKey) { repository.getAllPlacements() }

    // 行ごとにグループ化
    val placementsByRow = placements.groupBy { it.row }

    // スクロールなし（1画面に収まるようにする）
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
                isEditMode = isEditMode,
                onShortcutClick = onShortcutClick,
                onShortcutLongClick = onShortcutLongClick,
                onEmptyClick = { column -> onAddShortcutClick(rowConfig.rowIndex, column) },
                onSlotClickInEditMode = onSlotClickInEditMode,
                modifier = Modifier.weight(1f)  // 均等に高さを分配
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeRow(
    rowConfig: RowConfig,
    placements: List<ShortcutPlacement>,
    shortcuts: Map<String, ShortcutItem>,
    isEditMode: Boolean,
    onShortcutClick: (ShortcutItem) -> Unit,
    onShortcutLongClick: (ShortcutItem?) -> Unit,
    onEmptyClick: (column: Int) -> Unit,
    onSlotClickInEditMode: (row: Int, column: Int, currentShortcut: ShortcutItem?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // この行の各スロット
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
                        },
                        onLongClick = { onShortcutLongClick(shortcut) }
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
                        },
                        onLongClick = { onShortcutLongClick(null) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortcutButton(
    item: ShortcutItem,
    columns: Int,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val shortcutHelper = remember { ShortcutHelper(context) }

    // アプリアイコンを取得（APP, INTENTタイプの場合）
    val appIcon: Drawable? = remember(item.packageName) {
        item.packageName?.let { shortcutHelper.getAppIcon(it) }
    }

    // サイズ設定（columns数に応じて調整）
    val iconSize = when (columns) {
        1 -> 56.dp   // 横長は大きめ
        2 -> 40.dp
        else -> 36.dp
    }
    val labelSize = when (columns) {
        1 -> 24.sp   // 横長は大きめ
        2 -> 14.sp
        else -> 12.sp
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (!isEditMode) onLongClick()
                }
            )
            .then(
                if (isEditMode) Modifier.border(
                    width = 3.dp,
                    color = Color(0xFFFF9800),
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            when (columns) {
                1 -> {
                    // 1分割（横長）: 横並びレイアウト
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // アイコンを固定幅のBoxに入れて位置を統一
                        Box(
                            modifier = Modifier.width(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ShortcutIcon(
                                item = item,
                                appIcon = appIcon,
                                size = iconSize
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // テキスト部分も固定幅にして全体の幅を統一
                        Box(
                            modifier = Modifier.width(160.dp),
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
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // 2分割: 縦並びレイアウト
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ShortcutIcon(
                            item = item,
                            appIcon = appIcon,
                            size = iconSize
                        )
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
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                else -> {
                    // 3分割以上: アイコンのみ
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ShortcutIcon(
                            item = item,
                            appIcon = appIcon,
                            size = iconSize
                        )
                        if (isEditMode) {
                            Text(
                                text = "編集",
                                color = Color(0xFFFF9800),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ショートカットのアイコンを表示
 */
@Composable
private fun ShortcutIcon(
    item: ShortcutItem,
    appIcon: Drawable?,
    size: androidx.compose.ui.unit.Dp
) {
    when (item.type) {
        ShortcutType.APP, ShortcutType.INTENT -> {
            // アプリアイコン
            if (appIcon != null) {
                val bitmap = remember(appIcon) {
                    appIcon.toBitmap(128, 128)
                }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = item.label,
                    modifier = Modifier.size(size)
                )
            } else {
                // アイコンがない場合はデフォルト
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = item.label,
                    modifier = Modifier.size(size),
                    tint = Color(0xFF1976D2)
                )
            }
        }
        ShortcutType.CALENDAR -> {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "カレンダー",
                modifier = Modifier.size(size),
                tint = Color(0xFF4CAF50)
            )
        }
        ShortcutType.MEMO -> {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "メモ帳",
                modifier = Modifier.size(size),
                tint = Color(0xFFFF9800)
            )
        }
        ShortcutType.SETTINGS -> {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "設定",
                modifier = Modifier.size(size),
                tint = Color(0xFF757575)
            )
        }
        ShortcutType.PHONE -> {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = item.label,
                modifier = Modifier.size(size),
                tint = Color(0xFF4CAF50)
            )
        }
        ShortcutType.SMS -> {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = item.label,
                modifier = Modifier.size(size),
                tint = Color(0xFF2196F3)
            )
        }
        ShortcutType.EMPTY -> {
            // 空の場合は何も表示しない
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmptySlotButton(
    isEditMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (!isEditMode) onLongClick()
                }
            )
            .then(
                if (isEditMode) Modifier.border(
                    width = 2.dp,
                    color = Color(0xFFFF9800),
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
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
 * 編集モード時のボトムバー
 */
@Composable
fun EditModeBottomBar(onDone: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = Color(0xFFFF9800)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDone() }
        ) {
            Text(
                text = "編集完了",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * ページインジケーター（ドット）
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

@Composable
fun BottomNavigationBar(pagerState: PagerState) {
    val scope = rememberCoroutineScope()

    NavigationBar(
        modifier = Modifier.height(80.dp), // 少し高くして押しやすく
        containerColor = Color.White
    ) {
        // --- ホームボタン ---
        NavigationBarItem(
            selected = pagerState.currentPage == 0,
            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
            icon = { /* アイコンは空にする */ },
            label = {
                Text(
                    text = "ホーム",
                    fontSize = 24.sp, // 特大文字
                    fontWeight = FontWeight.Bold,
                    color = if (pagerState.currentPage == 0) Color.Black else Color.Gray
                )
            },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color(0xFFE0E0E0) // 選択時の背景（薄いグレー）
            )
        )

        // --- カレンダーボタン ---
        NavigationBarItem(
            selected = pagerState.currentPage == 1,
            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
            icon = { /* アイコンは空にする */ },
            label = {
                Text(
                    text = "カレンダー",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (pagerState.currentPage == 1) Color.Black else Color.Gray
                )
            },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color(0xFFE0E0E0)
            )
        )
    }
}

@Composable
fun NavButtonItem(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() }
            .background(if (isSelected) Color(0xFF444444) else Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
@Preview(showBackground = true, device = "spec:width=1080px,height=2340px,dpi=440")
@Composable
fun MainLauncherPreview() {
    MaterialTheme {
        MainLauncherScreen()
    }
}