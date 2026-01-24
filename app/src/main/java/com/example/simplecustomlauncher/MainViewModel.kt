package com.example.simplecustomlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Process
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.simplecustomlauncher.data.HomeLayoutConfig
import com.example.simplecustomlauncher.data.RowConfig
import com.example.simplecustomlauncher.data.SettingsRepository
import com.example.simplecustomlauncher.data.ShortcutItem
import com.example.simplecustomlauncher.data.ShortcutPlacement
import com.example.simplecustomlauncher.data.ShortcutRepository
import com.example.simplecustomlauncher.data.ShortcutType
import java.util.UUID

/**
 * メイン画面の状態
 */
sealed class MainScreenState {
    object Home : MainScreenState()
    object ShortcutAdd : MainScreenState()
    data class SlotEdit(
        val row: Int,
        val column: Int,
        val currentShortcut: ShortcutItem?
    ) : MainScreenState()
    object Calendar : MainScreenState()
    object Memo : MainScreenState()
    object AppSettings : MainScreenState()
}

/**
 * エラーイベント
 */
data class ErrorEvent(
    val message: String,
    val id: Long = System.currentTimeMillis()
)

/**
 * メイン画面の ViewModel
 */
class MainViewModel(
    private val shortcutRepository: ShortcutRepository,
    private val settingsRepository: SettingsRepository,
    private val calendarRepository: CalendarRepository
) : ViewModel() {

    // 画面状態
    var screenState by mutableStateOf<MainScreenState>(MainScreenState.Home)
        private set

    // 編集モード
    var isEditMode by mutableStateOf(false)
        private set

    // ダイアログ状態
    var showEditModeDialog by mutableStateOf(false)
        private set
    var showAddRowDialog by mutableStateOf(false)
        private set
    var shortcutToConfirm by mutableStateOf<ShortcutItem?>(null)
        private set

    // ターゲットスロット（ショートカット追加時）
    var targetSlot by mutableStateOf<Pair<Int, Int>?>(null)
        private set

    // エラーイベント
    var errorEvent by mutableStateOf<ErrorEvent?>(null)
        private set

    // データ更新トリガー
    var refreshKey by mutableStateOf(0)
        private set

    // 設定
    val showConfirmDialog: Boolean
        get() = settingsRepository.showConfirmDialog

    // ショートカットデータ（refreshKey で再計算）
    fun getAllShortcuts(): List<ShortcutItem> {
        return shortcutRepository.getAllShortcuts().values.toList()
    }

    fun getAllPlacements(): List<ShortcutPlacement> {
        return shortcutRepository.getAllPlacements()
    }

    fun getLayoutConfig(): HomeLayoutConfig {
        return shortcutRepository.getLayoutConfig()
    }

    fun getShortcutsMap(): Map<String, ShortcutItem> {
        return shortcutRepository.getAllShortcuts()
    }

    fun getUnplacedShortcuts(): List<ShortcutItem> {
        val placedIds = getAllPlacements().map { it.shortcutId }.toSet()
        return getAllShortcuts().filter {
            it.id !in placedIds && it.type != ShortcutType.EMPTY
        }
    }

    fun getPlacedShortcuts(): List<ShortcutItem> {
        val placedIds = getAllPlacements().map { it.shortcutId }.toSet()
        return getAllShortcuts().filter {
            it.id in placedIds && it.type != ShortcutType.EMPTY
        }
    }

    // 祝日データ
    fun getHolidaysForMonth(year: Int, month: Int, hasPermission: Boolean): Map<Int, String> {
        return if (hasPermission) {
            calendarRepository.getHolidaysForMonth(year, month)
        } else {
            emptyMap()
        }
    }

    // === 画面遷移 ===

    fun navigateTo(state: MainScreenState) {
        screenState = state
    }

    fun navigateToHome() {
        screenState = MainScreenState.Home
        targetSlot = null
    }

    fun navigateToShortcutAdd(row: Int, column: Int) {
        targetSlot = row to column
        screenState = MainScreenState.ShortcutAdd
    }

    fun navigateToSlotEdit(row: Int, column: Int, currentShortcut: ShortcutItem?) {
        screenState = MainScreenState.SlotEdit(row, column, currentShortcut)
    }

    // === 編集モード ===

    fun showEditModeConfirmDialog() {
        showEditModeDialog = true
    }

    fun dismissEditModeDialog() {
        showEditModeDialog = false
    }

    fun enterEditMode() {
        isEditMode = true
        showEditModeDialog = false
    }

    fun exitEditMode() {
        isEditMode = false
        refresh()
    }

    // === 行追加ダイアログ ===

    fun showAddRowDialogAction() {
        showAddRowDialog = true
    }

    fun dismissAddRowDialog() {
        showAddRowDialog = false
    }

    fun addRow(columns: Int) {
        val currentConfig = shortcutRepository.getLayoutConfig()
        val newRowIndex = currentConfig.rows.maxOfOrNull { it.rowIndex }?.plus(1) ?: 0
        val newRows = currentConfig.rows + RowConfig(rowIndex = newRowIndex, columns = columns)
        shortcutRepository.saveLayoutConfig(HomeLayoutConfig(rows = newRows))
        refresh()
        showAddRowDialog = false
    }

    // === ショートカット確認ダイアログ ===

    fun showShortcutConfirmDialog(item: ShortcutItem) {
        shortcutToConfirm = item
    }

    fun dismissShortcutConfirmDialog() {
        shortcutToConfirm = null
    }

    // === ショートカット操作 ===

    fun placeShortcut(shortcut: ShortcutItem, row: Int, column: Int) {
        shortcutRepository.savePlacement(
            ShortcutPlacement(shortcutId = shortcut.id, row = row, column = column)
        )
        refresh()
    }

    fun placeInternalFeature(type: ShortcutType, label: String, row: Int, column: Int) {
        val allShortcuts = getAllShortcuts()
        val item = findOrCreateInternalShortcut(allShortcuts, type, label)
        placeShortcut(item, row, column)
    }

    fun placeApp(packageName: String, label: String, row: Int, column: Int) {
        val allShortcuts = getAllShortcuts()
        val item = findOrCreateAppShortcut(allShortcuts, packageName, label)
        placeShortcut(item, row, column)
    }

    fun placeIntent(shortLabel: String, packageName: String, shortcutId: String, context: Context, row: Int, column: Int) {
        val item = ShortcutItem(
            id = UUID.randomUUID().toString(),
            type = ShortcutType.INTENT,
            label = shortLabel,
            packageName = packageName
        )
        shortcutRepository.saveShortcut(item)
        saveShortcutInfo(context, item.id, shortcutId, packageName)
        placeShortcut(item, row, column)
    }

    fun placeContact(name: String, phoneNumber: String, type: ShortcutType, row: Int, column: Int) {
        val item = ShortcutItem(
            id = UUID.randomUUID().toString(),
            type = type,
            label = name,
            phoneNumber = phoneNumber
        )
        shortcutRepository.saveShortcut(item)
        placeShortcut(item, row, column)
    }

    fun swapShortcuts(currentShortcut: ShortcutItem?, targetShortcut: ShortcutItem, row: Int, column: Int) {
        val placements = getAllPlacements()
        val existingPlacement = placements.find { it.shortcutId == targetShortcut.id }

        if (existingPlacement != null) {
            if (currentShortcut != null) {
                // 現在のショートカットを相手の位置に移動
                shortcutRepository.savePlacement(
                    ShortcutPlacement(
                        shortcutId = currentShortcut.id,
                        row = existingPlacement.row,
                        column = existingPlacement.column
                    )
                )
            } else {
                // 現在のスロットが空の場合、相手の配置を削除
                shortcutRepository.removePlacement(targetShortcut.id)
            }
        }
        // 選択したショートカットをこのスロットに配置
        shortcutRepository.savePlacement(
            ShortcutPlacement(shortcutId = targetShortcut.id, row = row, column = column)
        )
        refresh()
    }

    fun clearSlot(shortcut: ShortcutItem) {
        shortcutRepository.removePlacement(shortcut.id)
        refresh()
    }

    fun deleteRow(rowIndex: Int) {
        // この行の配置を全て削除
        val currentPlacements = shortcutRepository.getAllPlacements()
        currentPlacements.filter { it.row == rowIndex }.forEach {
            shortcutRepository.removePlacement(it.shortcutId)
        }
        // レイアウトから行を削除
        val currentConfig = shortcutRepository.getLayoutConfig()
        val newRows = currentConfig.rows.filter { it.rowIndex != rowIndex }
        shortcutRepository.saveLayoutConfig(HomeLayoutConfig(rows = newRows))
        refresh()
    }

    // === ショートカット起動 ===

    fun launchShortcut(context: Context, item: ShortcutItem, shortcutHelper: ShortcutHelper) {
        Log.d("MainViewModel", "launchShortcut: type=${item.type}, label=${item.label}, id=${item.id}")
        try {
            when (item.type) {
                ShortcutType.APP -> {
                    Log.d("MainViewModel", "Launching APP: ${item.packageName}")
                    item.packageName?.let { shortcutHelper.startApp(it) }
                }
                ShortcutType.INTENT -> {
                    val intent = item.toIntent()
                    Log.d("MainViewModel", "INTENT type: intentUri=${item.intentUri}, generatedIntent=$intent")
                    if (intent != null) {
                        context.startActivity(intent)
                    } else {
                        launchPinShortcut(context, item)
                    }
                }
                ShortcutType.PHONE -> {
                    item.toIntent()?.let { intent ->
                        // 電話アプリを明示的に指定（Zoomなどに奪われないように）
                        val dialerPackages = listOf(
                            "com.google.android.dialer",
                            "com.android.dialer"
                        )
                        val installedDialer = dialerPackages.firstOrNull { pkg ->
                            shortcutHelper.getAppIcon(pkg) != null
                        }
                        if (installedDialer != null) {
                            intent.setPackage(installedDialer)
                        }
                        context.startActivity(intent)
                    }
                }
                ShortcutType.SMS -> {
                    item.toIntent()?.let { intent ->
                        context.startActivity(intent)
                    }
                }
                ShortcutType.DIALER -> {
                    item.toIntent()?.let { intent ->
                        context.startActivity(intent)
                    }
                }
                ShortcutType.CALENDAR -> navigateTo(MainScreenState.Calendar)
                ShortcutType.MEMO -> navigateTo(MainScreenState.Memo)
                ShortcutType.SETTINGS -> navigateTo(MainScreenState.AppSettings)
                ShortcutType.EMPTY -> { /* 何もしない */ }
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to launch shortcut", e)
            showError("起動できませんでした")
        }
    }

    private fun launchPinShortcut(context: Context, item: ShortcutItem) {
        val prefs = context.getSharedPreferences("pin_shortcuts", Context.MODE_PRIVATE)
        val shortcutId = prefs.getString("${item.id}_shortcut_id", null)
        val packageName = prefs.getString("${item.id}_package", null)

        Log.d("MainViewModel", "launchPinShortcut: itemId=${item.id}, shortcutId=$shortcutId, package=$packageName")

        if (shortcutId != null && packageName != null) {
            try {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                Log.d("MainViewModel", "Starting shortcut via LauncherApps...")
                launcherApps.startShortcut(packageName, shortcutId, null, null, Process.myUserHandle())
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to launch pin shortcut: ${e.message}", e)
                showError("起動できませんでした: ${e.message}")
            }
        } else {
            Log.e("MainViewModel", "Shortcut info not found for item: ${item.id}, label: ${item.label}")
            showError("ショートカット情報が見つかりません")
        }
    }

    // === エラー処理 ===

    fun showError(message: String) {
        errorEvent = ErrorEvent(message)
    }

    fun clearError() {
        errorEvent = null
    }

    // === ヘルパー関数 ===

    private fun refresh() {
        refreshKey++
    }

    private fun findOrCreateAppShortcut(
        allShortcuts: List<ShortcutItem>,
        packageName: String,
        label: String
    ): ShortcutItem {
        val existing = allShortcuts.find {
            it.type == ShortcutType.APP && it.packageName == packageName
        }
        if (existing != null) return existing

        val item = ShortcutItem(
            id = UUID.randomUUID().toString(),
            type = ShortcutType.APP,
            label = label,
            packageName = packageName
        )
        shortcutRepository.saveShortcut(item)
        return item
    }

    private fun findOrCreateInternalShortcut(
        allShortcuts: List<ShortcutItem>,
        type: ShortcutType,
        label: String
    ): ShortcutItem {
        val existing = allShortcuts.find { it.type == type }
        if (existing != null) return existing

        val item = ShortcutItem(
            id = UUID.randomUUID().toString(),
            type = type,
            label = label
        )
        shortcutRepository.saveShortcut(item)
        return item
    }

    private fun saveShortcutInfo(context: Context, itemId: String, shortcutId: String, packageName: String) {
        val prefs = context.getSharedPreferences("pin_shortcuts", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("${itemId}_shortcut_id", shortcutId)
            .putString("${itemId}_package", packageName)
            .apply()
    }
}
