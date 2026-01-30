package com.example.simplecustomlauncher.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupManagerTest {

    private lateinit var backupManager: BackupManager
    private lateinit var shortcutRepo: ShortcutRepository
    private lateinit var settingsRepo: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        backupManager = BackupManager(context)
        shortcutRepo = ShortcutRepository(context)
        settingsRepo = SettingsRepository(context)
    }

    // --- A: 全データがJSON化される ---

    @Test
    fun createBackupJson_containsAllData() {
        // テストデータを準備
        val item = ShortcutItem(id = "bk-1", type = ShortcutType.APP, label = "テスト", packageName = "com.test")
        shortcutRepo.saveShortcut(item)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "bk-1", pageIndex = 0, row = 0, column = 0))
        shortcutRepo.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2)
        )))
        settingsRepo.themeMode = ThemeMode.DARK

        val json = backupManager.createBackupJson()

        // JSON文字列に各データが含まれている
        assertTrue(json.contains("bk-1"))
        assertTrue(json.contains("テスト"))
        assertTrue(json.contains("com.test"))
        assertTrue(json.contains("DARK"))
        assertTrue(json.contains("shortcuts"))
        assertTrue(json.contains("placements"))
        assertTrue(json.contains("layout"))
        assertTrue(json.contains("settings"))
    }

    // --- A: 正常なJSONから復元できる ---

    @Test
    fun restoreFromJson_validJson_restoresSuccessfully() {
        // まずバックアップを作成
        val item = ShortcutItem(id = "rs-1", type = ShortcutType.PHONE, label = "電話", phoneNumber = "090")
        shortcutRepo.saveShortcut(item)
        shortcutRepo.savePlacement(ShortcutPlacement(shortcutId = "rs-1", pageIndex = 0, row = 0, column = 0))
        shortcutRepo.saveLayoutConfig(HomeLayoutConfig(rows = listOf(
            RowConfig(pageIndex = 0, rowIndex = 0, columns = 2)
        )))
        val backupJson = backupManager.createBackupJson()

        // データをクリア
        shortcutRepo.clearAllLayout()
        assertTrue(shortcutRepo.getAllShortcuts().isEmpty())

        // 復元
        val result = backupManager.restoreFromJson(backupJson)

        assertTrue(result is RestoreResult.Success)
        val success = result as RestoreResult.Success
        assertEquals(1, success.shortcutCount)

        // データが復元されている
        val restored = shortcutRepo.getShortcut("rs-1")
        assertNotNull(restored)
        assertEquals("電話", restored!!.label)
        assertEquals("090", restored.phoneNumber)
    }

    // --- A: 不正なJSONでErrorが返る ---

    @Test
    fun restoreFromJson_invalidJson_returnsError() {
        val result = backupManager.restoreFromJson("this is not json{{{")
        assertTrue(result is RestoreResult.Error)
    }

    @Test
    fun restoreFromJson_invalidJson_doesNotCrash() {
        // 不正なJSONでアプリがフリーズしないこと
        val badInputs = listOf(
            "",
            "null",
            "[]",
            "{\"version\":1}",
            "{\"version\":1,\"shortcuts\":\"not array\",\"placements\":[],\"layout\":[]}"
        )
        for (input in badInputs) {
            val result = backupManager.restoreFromJson(input)
            assertTrue("Input '$input' should return Error", result is RestoreResult.Error)
        }
    }
}
