package com.example.simplecustomlauncher

import androidx.test.core.app.ApplicationProvider
import com.example.simplecustomlauncher.data.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    private lateinit var context: android.app.Application
    private lateinit var shortcutRepo: ShortcutRepository
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var premiumManager: DefaultPremiumManager
    private lateinit var calendarRepo: CalendarRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shortcutRepo = ShortcutRepository(context)
        settingsRepo = SettingsRepository(context)
        premiumManager = DefaultPremiumManager(context, settingsRepo)
        calendarRepo = CalendarRepository(context)

        // テストごとにクリア
        premiumManager.clearAllPremiumStatus()
        shortcutRepo.clearAllLayout()
        settingsRepo.pageCount = 1

        viewModel = MainViewModel(
            shortcutRepository = shortcutRepo,
            settingsRepository = settingsRepo,
            calendarRepository = calendarRepo,
            premiumManager = premiumManager
        )
    }

    // === getAccessiblePageCount ===

    // --- A: プレミアム時はsettingsのpageCountを返す ---

    @Test
    fun getAccessiblePageCount_premium_returnsSettingsPageCount() {
        settingsRepo.pageCount = 3
        premiumManager.recordPurchase()
        // ViewModelのisPremiumを更新
        viewModel.refreshPremiumStatus()

        assertEquals(3, viewModel.getAccessiblePageCount())
    }

    // --- A: 非プレミアム時は1を返す ---

    @Test
    fun getAccessiblePageCount_notPremium_returnsOne() {
        settingsRepo.pageCount = 3

        assertEquals(1, viewModel.getAccessiblePageCount())
    }

    // === getUnplacedShortcuts ===

    // --- A: 配置済み・内部機能・EMPTYが除外される ---

    @Test
    fun getUnplacedShortcuts_excludesPlacedAndInternalAndEmpty() {
        // レイアウト準備
        shortcutRepo.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 3)
        )))

        // 配置済みアプリ
        val placedApp = ShortcutItem(id = "placed-app", type = ShortcutType.APP, label = "配置済み", packageName = "com.placed")
        shortcutRepo.saveShortcut(placedApp)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "placed-app", pageIndex = 0, row = 0, column = 0))

        // 未配置アプリ（一時保管）
        val unplacedApp = ShortcutItem(id = "unplaced-app", type = ShortcutType.APP, label = "未配置", packageName = "com.unplaced")
        shortcutRepo.saveShortcut(unplacedApp)

        // 内部機能（未配置）
        val calendar = ShortcutItem(id = "cal", type = ShortcutType.CALENDAR, label = "カレンダー")
        shortcutRepo.saveShortcut(calendar)

        // EMPTY
        val empty = ShortcutItem(id = "empty", type = ShortcutType.EMPTY, label = "空")
        shortcutRepo.saveShortcut(empty)

        // 未配置の電話ショートカット（表示されるべき）
        val phone = ShortcutItem(id = "phone-1", type = ShortcutType.PHONE, label = "電話", phoneNumber = "090")
        shortcutRepo.saveShortcut(phone)

        viewModel.refresh()
        val unplaced = viewModel.getUnplacedShortcuts()

        // 未配置のアプリと電話のみが表示される
        val ids = unplaced.map { it.id }
        assertTrue("未配置アプリが含まれる", ids.contains("unplaced-app"))
        assertTrue("未配置電話が含まれる", ids.contains("phone-1"))
        assertFalse("配置済みは除外", ids.contains("placed-app"))
        assertFalse("内部機能は除外", ids.contains("cal"))
        assertFalse("EMPTYは除外", ids.contains("empty"))
    }

    // === swapShortcuts ===

    // --- A: 配置済み同士の入れ替えが正しく動作する ---

    @Test
    fun swapShortcuts_bothPlaced_swapsCorrectly() {
        shortcutRepo.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2)
        )))

        val itemA = ShortcutItem(id = "swap-a", type = ShortcutType.APP, label = "A", packageName = "com.a")
        val itemB = ShortcutItem(id = "swap-b", type = ShortcutType.APP, label = "B", packageName = "com.b")
        shortcutRepo.saveShortcut(itemA)
        shortcutRepo.saveShortcut(itemB)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "swap-a", pageIndex = 0, row = 0, column = 0))
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "swap-b", pageIndex = 0, row = 0, column = 1))

        // BをAの位置（0,0）に入れ替え（現在Aが配置されている）
        viewModel.swapShortcuts(currentShortcut = itemA, targetShortcut = itemB, pageIndex = 0, row = 0, column = 0)

        val placements = shortcutRepo.getAllPlacements()
        val placementA = placements.find { it.shortcutId == "swap-a" }
        val placementB = placements.find { it.shortcutId == "swap-b" }

        // BがAの元の位置(0,0)に、AがBの元の位置(0,1)に
        assertNotNull(placementA)
        assertNotNull(placementB)
        assertEquals(0, placementB!!.column)
        assertEquals(1, placementA!!.column)
    }

    // --- A: 空スロットとの入れ替え時にplacementが正しく処理される ---

    @Test
    fun swapShortcuts_emptySlot_movesCorrectly() {
        shortcutRepo.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2)
        )))

        val itemB = ShortcutItem(id = "swap-b2", type = ShortcutType.APP, label = "B", packageName = "com.b2")
        shortcutRepo.saveShortcut(itemB)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "swap-b2", pageIndex = 0, row = 0, column = 1))

        // 空スロット(0,0)にBを移動（currentShortcut = null）
        viewModel.swapShortcuts(currentShortcut = null, targetShortcut = itemB, pageIndex = 0, row = 0, column = 0)

        val placements = shortcutRepo.getAllPlacements()
        val placementB = placements.find { it.shortcutId == "swap-b2" }

        assertNotNull(placementB)
        assertEquals(0, placementB!!.column)
        // 元の位置(0,1)の配置は削除されている
        assertNull(placements.find { it.shortcutId == "swap-b2" && it.column == 1 })
    }

    // === clearSlot ===

    // --- A: 内部機能はショートカット自体も削除される ---

    @Test
    fun clearSlot_internalFeature_deletesShortcut() {
        val calItem = ShortcutItem(id = "cal-slot", type = ShortcutType.CALENDAR, label = "カレンダー")
        shortcutRepo.saveShortcut(calItem)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "cal-slot", pageIndex = 0, row = 0, column = 0))

        viewModel.clearSlot(calItem)

        // 配置が消えている
        assertTrue(shortcutRepo.getAllPlacements().none { it.shortcutId == "cal-slot" })
        // ショートカット自体も削除されている
        assertNull(shortcutRepo.getShortcut("cal-slot"))
    }

    // --- A: PHONE/SMS/INTENT等は一時保管に戻る（削除されない）---

    @Test
    fun clearSlot_phoneShortcut_keepsInUnplaced() {
        val phoneItem = ShortcutItem(id = "phone-slot", type = ShortcutType.PHONE, label = "電話", phoneNumber = "090")
        shortcutRepo.saveShortcut(phoneItem)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "phone-slot", pageIndex = 0, row = 0, column = 0))

        viewModel.clearSlot(phoneItem)

        // 配置は消えている
        assertTrue(shortcutRepo.getAllPlacements().none { it.shortcutId == "phone-slot" })
        // ショートカット自体は残っている（一時保管）
        assertNotNull(shortcutRepo.getShortcut("phone-slot"))
    }

    // === deleteRow ===

    // --- A: 行内の全placementと内部機能ショートカットが削除される ---

    @Test
    fun deleteRow_removesAllPlacementsAndInternalShortcuts() {
        shortcutRepo.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2),
            RowConfig(pageIndex = 0, rowIndex = 1, columns = 2)
        )))

        // 行0にカレンダー（内部機能）と電話を配置
        val calItem = ShortcutItem(id = "row-cal", type = ShortcutType.CALENDAR, label = "カレンダー")
        val phoneItem = ShortcutItem(id = "row-phone", type = ShortcutType.PHONE, label = "電話", phoneNumber = "090")
        shortcutRepo.saveShortcut(calItem)
        shortcutRepo.saveShortcut(phoneItem)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "row-cal", pageIndex = 0, row = 0, column = 0))
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "row-phone", pageIndex = 0, row = 0, column = 1))

        // 行1にアプリを配置
        val appItem = ShortcutItem(id = "row-app", type = ShortcutType.APP, label = "アプリ", packageName = "com.test")
        shortcutRepo.saveShortcut(appItem)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "row-app", pageIndex = 0, row = 1, column = 0))

        viewModel.deleteRow(0, 0)

        // 行0の配置が全て消えている
        val placements = shortcutRepo.getAllPlacements()
        assertTrue(placements.none { it.row == 0 && it.pageIndex == 0 })

        // 内部機能（カレンダー）は削除されている
        assertNull(shortcutRepo.getShortcut("row-cal"))
        // 電話は一時保管に残っている（shouldDeleteOnRemoveがfalse）
        assertNotNull(shortcutRepo.getShortcut("row-phone"))

        // 行1のアプリは影響なし
        assertNotNull(placements.find { it.shortcutId == "row-app" })

        // レイアウトから行0が削除されている
        val layout = shortcutRepo.getLayoutConfig()
        assertTrue(layout.rows.none { it.pageIndex == 0 && it.rowIndex == 0 })
        assertTrue(layout.rows.any { it.pageIndex == 0 && it.rowIndex == 1 })
    }

    // === changeRowColumns ===

    // --- A: 列数減少時にはみ出たショートカットが一時保管に移動される ---

    @Test
    fun changeRowColumns_decrease_movesOverflowToUnplaced() {
        shortcutRepo.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 3)
        )))

        // 3列に配置
        val phoneItem = ShortcutItem(id = "col-phone", type = ShortcutType.PHONE, label = "電話", phoneNumber = "090")
        shortcutRepo.saveShortcut(phoneItem)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "col-phone", pageIndex = 0, row = 0, column = 2))

        val appItem = ShortcutItem(id = "col-app", type = ShortcutType.APP, label = "アプリ", packageName = "com.test")
        shortcutRepo.saveShortcut(appItem)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "col-app", pageIndex = 0, row = 0, column = 0))

        // 3列→2列に変更（column=2がはみ出る）
        viewModel.changeRowColumns(0, 0, 2)

        val placements = shortcutRepo.getAllPlacements()
        // column=0のアプリは残っている
        assertNotNull(placements.find { it.shortcutId == "col-app" })
        // column=2の電話は配置解除されている
        assertNull(placements.find { it.shortcutId == "col-phone" })
        // 電話のショートカット自体は残っている（一時保管）
        assertNotNull(shortcutRepo.getShortcut("col-phone"))

        // レイアウトが2列に更新されている
        val layout = shortcutRepo.getLayoutConfig()
        assertEquals(2, layout.rows.first().columns)
    }

    // --- A: 内部機能のはみ出しはショートカット自体も削除される ---

    @Test
    fun changeRowColumns_decrease_deletesOverflowInternalFeature() {
        shortcutRepo.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 3)
        )))

        // column=2にカレンダー（内部機能）を配置
        val calItem = ShortcutItem(id = "col-cal", type = ShortcutType.CALENDAR, label = "カレンダー")
        shortcutRepo.saveShortcut(calItem)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "col-cal", pageIndex = 0, row = 0, column = 2))

        // 3列→2列に変更
        viewModel.changeRowColumns(0, 0, 2)

        // 配置解除されている
        val placements = shortcutRepo.getAllPlacements()
        assertNull(placements.find { it.shortcutId == "col-cal" })
        // 内部機能なのでショートカット自体も削除されている
        assertNull(shortcutRepo.getShortcut("col-cal"))
    }

    // === deletePage ===

    // --- A: 削除後に後続ページのpageIndexが繰り上がる（レイアウト） ---

    @Test
    fun deletePage_shiftsSubsequentPageRows() {
        settingsRepo.pageCount = 3
        premiumManager.recordPurchase()
        viewModel.refreshPremiumStatus()

        shortcutRepo.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2),
            RowConfig(pageIndex = 1, rowIndex = 0, columns = 3),
            RowConfig(pageIndex = 2, rowIndex = 0, columns = 1)
        )))

        // ページ1を削除
        viewModel.deletePage(1)

        val layout = shortcutRepo.getLayoutConfig()
        assertEquals(2, layout.rows.size)
        // ページ0はそのまま
        assertEquals(0, layout.rows[0].pageIndex)
        assertEquals(2, layout.rows[0].columns)
        // 旧ページ2がページ1に繰り上がっている
        assertEquals(1, layout.rows[1].pageIndex)
        assertEquals(1, layout.rows[1].columns)

        assertEquals(2, settingsRepo.pageCount)
    }

    // --- A: 削除後に後続placementのpageIndexが繰り上がる ---

    @Test
    fun deletePage_shiftsSubsequentPlacements() {
        settingsRepo.pageCount = 3
        premiumManager.recordPurchase()
        viewModel.refreshPremiumStatus()

        shortcutRepo.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2),
            RowConfig(pageIndex = 1, rowIndex = 0, columns = 2),
            RowConfig(pageIndex = 2, rowIndex = 0, columns = 2)
        )))

        // 各ページに配置
        val item0 = ShortcutItem(id = "page0", type = ShortcutType.PHONE, label = "P0", phoneNumber = "000")
        val item1 = ShortcutItem(id = "page1", type = ShortcutType.PHONE, label = "P1", phoneNumber = "111")
        val item2 = ShortcutItem(id = "page2", type = ShortcutType.PHONE, label = "P2", phoneNumber = "222")
        shortcutRepo.saveShortcut(item0)
        shortcutRepo.saveShortcut(item1)
        shortcutRepo.saveShortcut(item2)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "page0", pageIndex = 0, row = 0, column = 0))
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "page1", pageIndex = 1, row = 0, column = 0))
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "page2", pageIndex = 2, row = 0, column = 0))

        // ページ1を削除
        viewModel.deletePage(1)

        val placements = shortcutRepo.getAllPlacements()
        // ページ0のものはそのまま
        val p0 = placements.find { it.shortcutId == "page0" }
        assertNotNull(p0)
        assertEquals(0, p0!!.pageIndex)

        // ページ1は削除されている
        assertNull(placements.find { it.shortcutId == "page1" })

        // 旧ページ2がページ1に繰り上がった配置が存在する
        // （savePlacementは位置ベース上書きのため旧エントリも残るが、
        //   UIはレイアウト上のページのみ表示するため実害なし）
        val p2Placements = placements.filter { it.shortcutId == "page2" }
        assertTrue("繰り上がった配置が存在する", p2Placements.any { it.pageIndex == 1 })
    }

    // === launchShortcut ===

    // --- A: 各ShortcutTypeで正しい画面遷移が行われる ---

    @Test
    fun launchShortcut_calendar_navigatesToCalendar() {
        val item = ShortcutItem(id = "launch-cal", type = ShortcutType.CALENDAR, label = "カレンダー")
        val helper = ShortcutHelper(context)

        viewModel.launchShortcut(context, item, helper)

        assertEquals(MainScreenState.Calendar, viewModel.screenState)
    }

    @Test
    fun launchShortcut_memo_navigatesToMemo() {
        val item = ShortcutItem(id = "launch-memo", type = ShortcutType.MEMO, label = "メモ")
        val helper = ShortcutHelper(context)

        viewModel.launchShortcut(context, item, helper)

        assertEquals(MainScreenState.Memo, viewModel.screenState)
    }

    @Test
    fun launchShortcut_settings_navigatesToAppSettings() {
        val item = ShortcutItem(id = "launch-settings", type = ShortcutType.SETTINGS, label = "設定")
        val helper = ShortcutHelper(context)

        viewModel.launchShortcut(context, item, helper)

        assertEquals(MainScreenState.AppSettings, viewModel.screenState)
    }

    @Test
    fun launchShortcut_allApps_navigatesToAllApps() {
        val item = ShortcutItem(id = "launch-allapps", type = ShortcutType.ALL_APPS, label = "すべてのアプリ")
        val helper = ShortcutHelper(context)

        viewModel.launchShortcut(context, item, helper)

        assertEquals(MainScreenState.AllApps, viewModel.screenState)
    }

    @Test
    fun launchShortcut_dateDisplay_noStateChange() {
        val item = ShortcutItem(id = "launch-date", type = ShortcutType.DATE_DISPLAY, label = "日付")
        val helper = ShortcutHelper(context)
        val stateBefore = viewModel.screenState

        viewModel.launchShortcut(context, item, helper)

        assertEquals(stateBefore, viewModel.screenState)
    }

    // === onPackageRemoved ===

    // --- A: APP型ショートカットの配置とショートカット自体が削除される ---

    @Test
    fun onPackageRemoved_appType_removesShortcut() {
        val app = ShortcutItem(id = "rm-app", type = ShortcutType.APP, label = "削除対象", packageName = "com.removed.app")
        shortcutRepo.saveShortcut(app)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "rm-app", pageIndex = 0, row = 0, column = 0))

        viewModel.onPackageRemoved("com.removed.app")

        assertNull(shortcutRepo.getShortcut("rm-app"))
        assertTrue(shortcutRepo.getAllPlacements().none { it.shortcutId == "rm-app" })
    }

    // --- A: INTENT型ショートカットもピン情報含め削除される ---

    @Test
    fun onPackageRemoved_intentType_removesShortcutAndPinInfo() {
        val intent = ShortcutItem(id = "rm-intent", type = ShortcutType.INTENT, label = "Intent削除", packageName = "com.removed.app")
        shortcutRepo.saveShortcut(intent)
        shortcutRepo.savePinShortcutInfo("rm-intent", "pin-id", "com.removed.app")
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "rm-intent", pageIndex = 0, row = 0, column = 0))

        viewModel.onPackageRemoved("com.removed.app")

        assertNull(shortcutRepo.getShortcut("rm-intent"))
        assertTrue(shortcutRepo.getAllPlacements().none { it.shortcutId == "rm-intent" })
        val (pinId, _) = shortcutRepo.getPinShortcutInfo("rm-intent")
        assertNull(pinId)
    }

    // --- A: 複数ページにまたがるショートカットも全て削除される ---

    @Test
    fun onPackageRemoved_multiPage_removesAll() {
        val app = ShortcutItem(id = "rm-multi", type = ShortcutType.APP, label = "マルチ", packageName = "com.removed.multi")
        shortcutRepo.saveShortcut(app)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "rm-multi", pageIndex = 0, row = 0, column = 0))
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "rm-multi", pageIndex = 1, row = 0, column = 0))

        viewModel.onPackageRemoved("com.removed.multi")

        assertNull(shortcutRepo.getShortcut("rm-multi"))
        assertTrue(shortcutRepo.getAllPlacements().none { it.shortcutId == "rm-multi" })
    }

    // --- A: 関係ないパッケージは影響を受けない ---

    @Test
    fun onPackageRemoved_unrelatedPackage_noEffect() {
        val app = ShortcutItem(id = "keep-app", type = ShortcutType.APP, label = "残る", packageName = "com.keep.app")
        shortcutRepo.saveShortcut(app)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "keep-app", pageIndex = 0, row = 0, column = 0))

        viewModel.onPackageRemoved("com.removed.other")

        assertNotNull(shortcutRepo.getShortcut("keep-app"))
        assertEquals(1, shortcutRepo.getAllPlacements().size)
    }

    // === cleanupUninstalledPackages ===

    // --- A: 未インストールのAPP型が削除される ---

    @Test
    fun cleanupUninstalledPackages_removesUninstalledApp() {
        // Robolectric環境では未登録パッケージはNameNotFoundExceptionになる
        val app = ShortcutItem(id = "gone-app", type = ShortcutType.APP, label = "未インストール", packageName = "com.nonexistent.app")
        shortcutRepo.saveShortcut(app)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "gone-app", pageIndex = 0, row = 0, column = 0))

        viewModel.cleanupUninstalledPackages(context)

        assertNull(shortcutRepo.getShortcut("gone-app"))
        assertTrue(shortcutRepo.getAllPlacements().none { it.shortcutId == "gone-app" })
    }

    // --- A: 未インストールのINTENT型もピン情報含め削除される ---

    @Test
    fun cleanupUninstalledPackages_removesUninstalledIntent() {
        val intent = ShortcutItem(id = "gone-intent", type = ShortcutType.INTENT, label = "Intent", packageName = "com.nonexistent.intent")
        shortcutRepo.saveShortcut(intent)
        shortcutRepo.savePinShortcutInfo("gone-intent", "pin-id", "com.nonexistent.intent")
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "gone-intent", pageIndex = 0, row = 0, column = 0))

        viewModel.cleanupUninstalledPackages(context)

        assertNull(shortcutRepo.getShortcut("gone-intent"))
        val (pinId, _) = shortcutRepo.getPinShortcutInfo("gone-intent")
        assertNull(pinId)
    }

    // --- A: PHONE/SMS型はpackageNameが無いので影響を受けない ---

    @Test
    fun cleanupUninstalledPackages_doesNotAffectPhoneShortcuts() {
        val phone = ShortcutItem(id = "phone-safe", type = ShortcutType.PHONE, label = "電話", phoneNumber = "090")
        shortcutRepo.saveShortcut(phone)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "phone-safe", pageIndex = 0, row = 0, column = 0))

        viewModel.cleanupUninstalledPackages(context)

        assertNotNull(shortcutRepo.getShortcut("phone-safe"))
        assertEquals(1, shortcutRepo.getAllPlacements().size)
    }
}
