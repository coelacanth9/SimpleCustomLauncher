package com.example.simplecustomlauncher.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShortcutRepositoryTest {

    private lateinit var repo: ShortcutRepository

    @Before
    fun setUp() {
        repo = ShortcutRepository(ApplicationProvider.getApplicationContext())
    }

    // --- A: 保存したショートカットを正しく取得できる ---

    @Test
    fun saveAndGetShortcut() {
        val item = ShortcutItem(
            id = "test-1",
            type = ShortcutType.APP,
            label = "テスト",
            packageName = "com.example.test"
        )
        repo.saveShortcut(item)

        val result = repo.getShortcut("test-1")
        assertNotNull(result)
        assertEquals("テスト", result!!.label)
        assertEquals(ShortcutType.APP, result.type)
        assertEquals("com.example.test", result.packageName)
    }

    // --- A: 削除時に関連するplacementも連動して削除される ---

    @Test
    fun deleteShortcut_alsoRemovesPlacement() {
        val item = ShortcutItem(id = "del-1", type = ShortcutType.APP, label = "削除対象", packageName = "com.test")
        repo.saveShortcut(item)
        repo.savePlacement(ShortcutPlacement(shortcutId = "del-1", row = 0, column = 0))

        repo.deleteShortcut("del-1")

        assertNull(repo.getShortcut("del-1"))
        assertTrue(repo.getAllPlacements().none { it.shortcutId == "del-1" })
    }

    // --- A: レイアウト設定の保存と読み込みが一致する ---

    @Test
    fun saveAndGetLayoutConfig() {
        val config = HomeLayoutConfig(
            rows = listOf(
                RowConfig(pageIndex = 0, rowIndex = 0, columns = 2),
                RowConfig(pageIndex = 0, rowIndex = 1, columns = 3, fixedHeightDp = 80),
                RowConfig(pageIndex = 1, rowIndex = 0, columns = 1, textOnly = true)
            )
        )
        repo.saveLayoutConfig(config)

        val result = repo.getLayoutConfig()
        assertEquals(3, result.rows.size)
        assertEquals(2, result.rows[0].columns)
        assertEquals(80, result.rows[1].fixedHeightDp)
        assertEquals(true, result.rows[2].textOnly)
        assertEquals(1, result.rows[2].pageIndex)
    }

    // --- A: 指定ページのplacement・layout・ショートカットが削除される ---

    @Test
    fun clearPageLayout_removesPageData() {
        // ページ0にショートカット配置
        val item0 = ShortcutItem(id = "p0", type = ShortcutType.MEMO, label = "ページ0用")
        repo.saveShortcut(item0)
        repo.savePlacement(ShortcutPlacement(shortcutId = "p0", pageIndex = 0, row = 0, column = 0))

        // ページ1にショートカット配置
        val item1 = ShortcutItem(id = "p1", type = ShortcutType.CALENDAR, label = "ページ1用")
        repo.saveShortcut(item1)
        repo.savePlacement(ShortcutPlacement(shortcutId = "p1", pageIndex = 1, row = 0, column = 0))

        // レイアウト
        repo.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2),
            RowConfig(pageIndex = 1, rowIndex = 0, columns = 2)
        )))

        // ページ0をクリア
        repo.clearPageLayout(0)

        // ページ0のデータが消えている
        assertTrue(repo.getPlacementsForPage(0).isEmpty())
        assertTrue(repo.getLayoutConfig().getRowsForPage(0).isEmpty())
        assertNull(repo.getShortcut("p0"))

        // ページ1のデータは残っている
        assertEquals(1, repo.getPlacementsForPage(1).size)
        assertEquals(1, repo.getLayoutConfig().getRowsForPage(1).size)
        assertNotNull(repo.getShortcut("p1"))
    }

    // --- A: 他ページで使用中のショートカットは削除されない ---

    @Test
    fun clearPageLayout_keepsShortcutUsedOnOtherPages() {
        // 同じショートカットをページ0とページ1の両方に配置
        val shared = ShortcutItem(id = "shared", type = ShortcutType.MEMO, label = "共有")
        repo.saveShortcut(shared)
        repo.savePlacement(ShortcutPlacement(shortcutId = "shared", pageIndex = 0, row = 0, column = 0))
        repo.savePlacement(ShortcutPlacement(shortcutId = "shared", pageIndex = 1, row = 0, column = 0))

        repo.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2),
            RowConfig(pageIndex = 1, rowIndex = 0, columns = 2)
        )))

        // ページ0をクリア
        repo.clearPageLayout(0)

        // ショートカット自体はページ1で使用中なので残る
        assertNotNull(repo.getShortcut("shared"))
        assertEquals(1, repo.getPlacementsForPage(1).size)
    }

    // --- A: パッケージ名でショートカットを取得できる ---

    @Test
    fun getShortcutsByPackageName_returnsMatchingShortcuts() {
        val app1 = ShortcutItem(id = "app1", type = ShortcutType.APP, label = "App1", packageName = "com.example.target")
        val app2 = ShortcutItem(id = "app2", type = ShortcutType.APP, label = "App2", packageName = "com.example.other")
        val intent1 = ShortcutItem(id = "intent1", type = ShortcutType.INTENT, label = "Intent1", packageName = "com.example.target")
        repo.saveShortcut(app1)
        repo.saveShortcut(app2)
        repo.saveShortcut(intent1)

        val result = repo.getShortcutsByPackageName("com.example.target")
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "app1" })
        assertTrue(result.any { it.id == "intent1" })
    }

    @Test
    fun getShortcutsByPackageName_noMatch_returnsEmpty() {
        val app = ShortcutItem(id = "app", type = ShortcutType.APP, label = "App", packageName = "com.example.other")
        repo.saveShortcut(app)

        val result = repo.getShortcutsByPackageName("com.example.nonexistent")
        assertTrue(result.isEmpty())
    }
}
