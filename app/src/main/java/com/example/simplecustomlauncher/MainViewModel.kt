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
import com.example.simplecustomlauncher.data.PremiumManager
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
    data class ShortcutAdd(
        val pageIndex: Int = 0,
        val row: Int = 0,
        val column: Int = 0
    ) : MainScreenState()
    data class SlotEdit(
        val pageIndex: Int = 0,
        val row: Int,
        val column: Int,
        val currentShortcut: ShortcutItem?
    ) : MainScreenState()
    object Calendar : MainScreenState()
    object Memo : MainScreenState()
    object AppSettings : MainScreenState()
    object AllApps : MainScreenState()
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
    private val calendarRepository: CalendarRepository,
    private val premiumManager: PremiumManager
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
    var showAddPageConfirmDialog by mutableStateOf(false)
        private set
    var showPremiumRequiredForPageDialog by mutableStateOf(false)
        private set
    var pendingRowColumns by mutableStateOf(0)
        private set
    var shortcutToConfirm by mutableStateOf<ShortcutItem?>(null)
        private set

    // ターゲットスロット（ショートカット追加時）: Triple<pageIndex, row, column>
    var targetSlot by mutableStateOf<Triple<Int, Int, Int>?>(null)
        private set

    // 現在のページインデックス
    var currentPageIndex by mutableStateOf(0)
        private set

    // エラーイベント
    var errorEvent by mutableStateOf<ErrorEvent?>(null)
        private set

    // データ更新トリガー
    var refreshKey by mutableStateOf(0)
        private set

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

    /**
     * 指定ページの配置を取得
     */
    fun getPlacementsForPage(pageIndex: Int): List<ShortcutPlacement> {
        return shortcutRepository.getPlacementsForPage(pageIndex)
    }

    // === ページング関連 ===

    /**
     * プレミアム状態を取得
     */
    fun isPremiumActive(): Boolean = premiumManager.isPremiumActive()

    /**
     * アクセス可能なページ数を取得
     */
    fun getAccessiblePageCount(): Int {
        return if (premiumManager.isPremiumActive()) {
            settingsRepository.pageCount
        } else {
            1  // 非プレミアム時は1ページのみ
        }
    }

    /**
     * 設定されたページ数を取得（プレミアム状態に関係なく）
     */
    fun getTotalPageCount(): Int = settingsRepository.pageCount

    /**
     * ループページングが有効かどうか
     */
    fun isLoopPagingEnabled(): Boolean = settingsRepository.loopPagingEnabled

    /**
     * 現在のページを設定
     */
    fun setCurrentPage(pageIndex: Int) {
        currentPageIndex = pageIndex
    }

    /**
     * 動画広告視聴を記録
     */
    fun recordAdWatch() {
        premiumManager.recordAdWatch()
        refresh()
    }

    /**
     * 買い切り購入を記録
     */
    fun recordPurchase() {
        premiumManager.recordPurchase()
        refresh()
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

    fun navigateToShortcutAdd(pageIndex: Int, row: Int, column: Int) {
        targetSlot = Triple(pageIndex, row, column)
        screenState = MainScreenState.ShortcutAdd(pageIndex, row, column)
    }

    fun navigateToSlotEdit(pageIndex: Int, row: Int, column: Int, currentShortcut: ShortcutItem?) {
        screenState = MainScreenState.SlotEdit(pageIndex, row, column, currentShortcut)
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
        screenState = MainScreenState.Home
    }

    fun exitEditMode() {
        isEditMode = false
        refresh()
    }

    fun resetToDefault() {
        shortcutRepository.resetToDefault()
        refresh()
    }

    fun clearLayout() {
        shortcutRepository.clearAllLayout()
        refresh()
    }

    // === 行追加ダイアログ ===

    fun showAddRowDialogAction() {
        showAddRowDialog = true
    }

    fun dismissAddRowDialog() {
        showAddRowDialog = false
    }

    /**
     * 現在のページに行を追加
     */
    fun addRow(columns: Int) {
        addRowToPage(currentPageIndex, columns)
    }

    /**
     * 指定ページに行を追加
     */
    fun addRowToPage(pageIndex: Int, columns: Int) {
        val currentConfig = shortcutRepository.getLayoutConfig()
        val pageRows = currentConfig.getRowsForPage(pageIndex)

        // 行数上限チェック（1ページあたり最大5行）
        if (pageRows.size >= MAX_ROWS_PER_PAGE) {
            val currentPageCount = settingsRepository.pageCount
            if (currentPageCount < SettingsRepository.MAX_PAGES) {
                // ページ追加可能
                pendingRowColumns = columns
                if (premiumManager.isPremiumActive()) {
                    // プレミアム有効 → 確認ダイアログを表示
                    showAddPageConfirmDialog = true
                } else {
                    // 未課金 → プレミアム誘導ダイアログを表示
                    showPremiumRequiredForPageDialog = true
                }
            } else {
                // 5ページ全て満杯 → 何もできない
                showError("これ以上行を追加できません")
            }
            showAddRowDialog = false
            return
        }

        val newRowIndex = pageRows.maxOfOrNull { it.rowIndex }?.plus(1) ?: 0
        val newRows = currentConfig.rows + RowConfig(
            pageIndex = pageIndex,
            rowIndex = newRowIndex,
            columns = columns
        )
        shortcutRepository.saveLayoutConfig(HomeLayoutConfig(rows = newRows))
        refresh()
        showAddRowDialog = false
    }

    /**
     * ページ追加確認ダイアログを閉じる
     */
    fun dismissAddPageConfirmDialog() {
        showAddPageConfirmDialog = false
        pendingRowColumns = 0
    }

    /**
     * プレミアム誘導ダイアログを閉じる
     */
    fun dismissPremiumRequiredForPageDialog() {
        showPremiumRequiredForPageDialog = false
        pendingRowColumns = 0
    }

    /**
     * 動画視聴でプレミアム解除してページ追加
     */
    fun watchAdAndAddPage() {
        premiumManager.recordAdWatch()
        showPremiumRequiredForPageDialog = false
        // プレミアム有効になったのでページ追加を実行
        confirmAddPageWithRow()
    }

    /**
     * 課金でプレミアム解除してページ追加
     */
    fun purchaseAndAddPage() {
        premiumManager.recordPurchase()
        showPremiumRequiredForPageDialog = false
        // プレミアム有効になったのでページ追加を実行
        confirmAddPageWithRow()
    }

    // ページ遷移リクエスト（UIが監視して遷移する）
    var navigateToPageRequest by mutableStateOf<Int?>(null)
        private set

    fun clearNavigateToPageRequest() {
        navigateToPageRequest = null
    }

    /**
     * 新しいページを追加して行を追加
     */
    fun confirmAddPageWithRow() {
        val currentPageCount = settingsRepository.pageCount
        if (currentPageCount >= SettingsRepository.MAX_PAGES) {
            showAddPageConfirmDialog = false
            return
        }

        // ページ数を増やす
        val newPageCount = currentPageCount + 1
        settingsRepository.pageCount = newPageCount

        // 新しいページに行を追加
        val currentConfig = shortcutRepository.getLayoutConfig()
        val newPageIndex = newPageCount - 1
        val newRows = currentConfig.rows + RowConfig(
            pageIndex = newPageIndex,
            rowIndex = 0,
            columns = pendingRowColumns
        )
        shortcutRepository.saveLayoutConfig(HomeLayoutConfig(rows = newRows))

        refresh()
        showAddPageConfirmDialog = false
        pendingRowColumns = 0

        // 新しいページへ遷移をリクエスト
        navigateToPageRequest = newPageIndex
    }

    companion object {
        const val MAX_ROWS_PER_PAGE = 5
    }

    // === ショートカット確認ダイアログ ===

    fun showShortcutConfirmDialog(item: ShortcutItem) {
        shortcutToConfirm = item
    }

    fun dismissShortcutConfirmDialog() {
        shortcutToConfirm = null
    }

    // === ショートカット操作 ===

    fun placeShortcut(shortcut: ShortcutItem, pageIndex: Int, row: Int, column: Int) {
        shortcutRepository.savePlacement(
            ShortcutPlacement(
                shortcutId = shortcut.id,
                pageIndex = pageIndex,
                row = row,
                column = column
            )
        )
        refresh()
    }

    fun placeInternalFeature(type: ShortcutType, label: String, pageIndex: Int, row: Int, column: Int) {
        val allShortcuts = getAllShortcuts()
        val item = findOrCreateInternalShortcut(allShortcuts, type, label)
        placeShortcut(item, pageIndex, row, column)
    }

    fun placeApp(packageName: String, label: String, pageIndex: Int, row: Int, column: Int) {
        val allShortcuts = getAllShortcuts()
        val item = findOrCreateAppShortcut(allShortcuts, packageName, label)
        placeShortcut(item, pageIndex, row, column)
    }

    fun placeIntent(shortLabel: String, packageName: String, shortcutId: String, pageIndex: Int, row: Int, column: Int) {
        val item = ShortcutItem(
            id = UUID.randomUUID().toString(),
            type = ShortcutType.INTENT,
            label = shortLabel,
            packageName = packageName
        )
        shortcutRepository.saveShortcut(item)
        shortcutRepository.savePinShortcutInfo(item.id, shortcutId, packageName)
        placeShortcut(item, pageIndex, row, column)
    }

    fun placeContact(name: String, phoneNumber: String, type: ShortcutType, pageIndex: Int, row: Int, column: Int) {
        val item = ShortcutItem(
            id = UUID.randomUUID().toString(),
            type = type,
            label = name,
            phoneNumber = phoneNumber
        )
        shortcutRepository.saveShortcut(item)
        placeShortcut(item, pageIndex, row, column)
    }

    fun swapShortcuts(currentShortcut: ShortcutItem?, targetShortcut: ShortcutItem, pageIndex: Int, row: Int, column: Int) {
        val placements = getAllPlacements()
        val existingPlacement = placements.find { it.shortcutId == targetShortcut.id }

        if (existingPlacement != null) {
            if (currentShortcut != null) {
                // 現在のショートカットを相手の位置に移動
                shortcutRepository.savePlacement(
                    ShortcutPlacement(
                        shortcutId = currentShortcut.id,
                        pageIndex = existingPlacement.pageIndex,
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
            ShortcutPlacement(
                shortcutId = targetShortcut.id,
                pageIndex = pageIndex,
                row = row,
                column = column
            )
        )
        refresh()
    }

    fun clearSlot(shortcut: ShortcutItem) {
        shortcutRepository.removePlacement(shortcut.id)
        refresh()
    }

    fun deleteRow(pageIndex: Int, rowIndex: Int) {
        // この行の配置を全て削除
        val currentPlacements = shortcutRepository.getAllPlacements()
        currentPlacements.filter { it.pageIndex == pageIndex && it.row == rowIndex }.forEach {
            shortcutRepository.removePlacement(it.shortcutId)
        }
        // レイアウトから行を削除
        val currentConfig = shortcutRepository.getLayoutConfig()
        val newRows = currentConfig.rows.filter {
            !(it.pageIndex == pageIndex && it.rowIndex == rowIndex)
        }
        shortcutRepository.saveLayoutConfig(HomeLayoutConfig(rows = newRows))
        refresh()
    }

    fun changeRowColumns(pageIndex: Int, rowIndex: Int, newColumns: Int) {
        val currentConfig = shortcutRepository.getLayoutConfig()
        val currentPlacements = shortcutRepository.getAllPlacements()

        // 分割数を減らす場合、はみ出た配置を削除
        currentPlacements
            .filter { it.pageIndex == pageIndex && it.row == rowIndex && it.column >= newColumns }
            .forEach { shortcutRepository.removePlacement(it.shortcutId) }

        // レイアウト更新
        val newRows = currentConfig.rows.map { row ->
            if (row.pageIndex == pageIndex && row.rowIndex == rowIndex) {
                row.copy(columns = newColumns)
            } else {
                row
            }
        }
        shortcutRepository.saveLayoutConfig(HomeLayoutConfig(rows = newRows))
        refresh()
    }

    /**
     * 指定ページを削除
     */
    fun deletePage(pageIndex: Int) {
        val currentConfig = shortcutRepository.getLayoutConfig()
        val currentPlacements = shortcutRepository.getAllPlacements()
        val currentPageCount = settingsRepository.pageCount

        // 1ページしかない場合は削除できない
        if (currentPageCount <= 1) return

        // このページの配置を全て削除
        currentPlacements.filter { it.pageIndex == pageIndex }.forEach {
            shortcutRepository.removePlacement(it.shortcutId)
        }

        // このページの行を削除し、後続ページのpageIndexを1つ減らす
        val newRows = currentConfig.rows
            .filter { it.pageIndex != pageIndex }
            .map { row ->
                if (row.pageIndex > pageIndex) {
                    row.copy(pageIndex = row.pageIndex - 1)
                } else {
                    row
                }
            }
        shortcutRepository.saveLayoutConfig(HomeLayoutConfig(rows = newRows))

        // 後続ページの配置のpageIndexも1つ減らす
        val remainingPlacements = shortcutRepository.getAllPlacements()
        remainingPlacements.filter { it.pageIndex > pageIndex }.forEach { placement ->
            shortcutRepository.savePlacement(placement.copy(pageIndex = placement.pageIndex - 1))
        }

        // ページ数を減らす
        settingsRepository.pageCount = currentPageCount - 1

        // 削除したページが現在ページだった場合、前のページへ移動
        if (currentPageIndex >= currentPageCount - 1) {
            navigateToPageRequest = maxOf(0, currentPageIndex - 1)
        }

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
                ShortcutType.ALL_APPS -> navigateTo(MainScreenState.AllApps)
                ShortcutType.EMPTY -> { /* 何もしない */ }
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to launch shortcut", e)
            showError("起動できませんでした")
        }
    }

    private fun launchPinShortcut(context: Context, item: ShortcutItem) {
        val (shortcutId, packageName) = shortcutRepository.getPinShortcutInfo(item.id)

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
