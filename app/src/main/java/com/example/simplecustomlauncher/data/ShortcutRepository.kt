package com.example.simplecustomlauncher.data

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * ショートカットとレイアウトの永続化を担当
 */
class ShortcutRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    private val packageManager: PackageManager = context.packageManager

    // スレッドセーフ用のロックオブジェクト
    private val shortcutLock = Any()
    private val placementLock = Any()
    private val layoutLock = Any()

    // ===== ショートカット =====

    fun saveShortcut(item: ShortcutItem) {
        synchronized(shortcutLock) {
            val shortcuts = getAllShortcutsInternal().toMutableMap()
            shortcuts[item.id] = item
            saveAllShortcuts(shortcuts.values.toList())
        }
    }

    fun deleteShortcut(id: String) {
        synchronized(shortcutLock) {
            val shortcuts = getAllShortcutsInternal().toMutableMap()
            shortcuts.remove(id)
            saveAllShortcuts(shortcuts.values.toList())
        }

        // 配置情報も削除
        synchronized(placementLock) {
            val placements = getAllPlacementsInternal().filter { it.shortcutId != id }
            saveAllPlacements(placements)
        }
    }

    fun getShortcut(id: String): ShortcutItem? {
        return getAllShortcuts()[id]
    }

    fun getAllShortcuts(): Map<String, ShortcutItem> {
        synchronized(shortcutLock) {
            return getAllShortcutsInternal()
        }
    }

    private fun getAllShortcutsInternal(): Map<String, ShortcutItem> {
        val json = prefs.getString(KEY_SHORTCUTS, null) ?: return emptyMap()
        return try {
            val array = JSONArray(json)
            val map = mutableMapOf<String, ShortcutItem>()
            for (i in 0 until array.length()) {
                val item = jsonToShortcutItem(array.getJSONObject(i))
                map[item.id] = item
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveAllShortcuts(shortcuts: List<ShortcutItem>) {
        val array = JSONArray()
        shortcuts.forEach { array.put(shortcutItemToJson(it)) }
        prefs.edit().putString(KEY_SHORTCUTS, array.toString()).apply()
    }

    // ===== 配置情報 =====

    fun savePlacement(placement: ShortcutPlacement) {
        synchronized(placementLock) {
            val placements = getAllPlacementsInternal().toMutableList()
            // 同じ位置（ページ+行+列）の既存配置を削除（上書き）
            // 注: 同じショートカットを別ページに配置することは許可する
            placements.removeAll {
                it.pageIndex == placement.pageIndex &&
                it.row == placement.row &&
                it.column == placement.column
            }
            placements.add(placement)
            saveAllPlacements(placements)
        }
    }

    /**
     * 指定ページの配置を取得
     */
    fun getPlacementsForPage(pageIndex: Int): List<ShortcutPlacement> {
        return getAllPlacements().filter { it.pageIndex == pageIndex }
    }

    fun removePlacement(shortcutId: String) {
        synchronized(placementLock) {
            val placements = getAllPlacementsInternal().filter { it.shortcutId != shortcutId }
            saveAllPlacements(placements)
        }
    }

    fun getAllPlacements(): List<ShortcutPlacement> {
        synchronized(placementLock) {
            return getAllPlacementsInternal()
        }
    }

    private fun getAllPlacementsInternal(): List<ShortcutPlacement> {
        val json = prefs.getString(KEY_PLACEMENTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { jsonToPlacement(array.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveAllPlacements(placements: List<ShortcutPlacement>) {
        val array = JSONArray()
        placements.forEach { array.put(placementToJson(it)) }
        prefs.edit().putString(KEY_PLACEMENTS, array.toString()).apply()
    }

    // ===== レイアウト設定 =====

    fun saveLayoutConfig(config: HomeLayoutConfig) {
        synchronized(layoutLock) {
            saveLayoutConfigInternal(config)
        }
    }

    private fun saveLayoutConfigInternal(config: HomeLayoutConfig) {
        val array = JSONArray()
        config.rows.forEach { row ->
            array.put(JSONObject().apply {
                put("pageIndex", row.pageIndex)
                put("rowIndex", row.rowIndex)
                put("columns", row.columns)
                if (row.fixedHeightDp != null) {
                    put("fixedHeightDp", row.fixedHeightDp)
                }
                put("textOnly", row.textOnly)
            })
        }
        prefs.edit().putString(KEY_LAYOUT, array.toString()).apply()
    }

    fun getLayoutConfig(): HomeLayoutConfig {
        synchronized(layoutLock) {
            return getLayoutConfigInternal()
        }
    }

    private fun getLayoutConfigInternal(): HomeLayoutConfig {
        val json = prefs.getString(KEY_LAYOUT, null) ?: return HomeLayoutConfig()
        return try {
            val array = JSONArray(json)
            val rows = (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                RowConfig(
                    pageIndex = obj.optInt("pageIndex", 0),  // マイグレーション対応
                    rowIndex = obj.getInt("rowIndex"),
                    columns = obj.getInt("columns"),
                    fixedHeightDp = if (obj.has("fixedHeightDp")) obj.getInt("fixedHeightDp") else null,
                    textOnly = obj.optBoolean("textOnly", false)
                )
            }
            HomeLayoutConfig(rows)
        } catch (e: Exception) {
            HomeLayoutConfig()
        }
    }

    // ===== 便利メソッド =====

    /**
     * ショートカットを追加して最初の空きスロットに配置
     */
    fun addShortcutToFirstEmpty(item: ShortcutItem): Boolean {
        val layout = getLayoutConfig()
        val placements = getAllPlacements()
        val emptySlot = layout.findFirstEmptySlot(placements)

        return if (emptySlot != null) {
            saveShortcut(item)
            savePlacement(ShortcutPlacement(
                shortcutId = item.id,
                row = emptySlot.first,
                column = emptySlot.second
            ))
            true
        } else {
            // 空きがない場合はショートカットだけ保存（未配置）
            saveShortcut(item)
            false
        }
    }

    /**
     * 行ごとに配置されたショートカットを取得（後方互換：ページ0のみ）
     */
    fun getShortcutsByRow(): Map<Int, List<Pair<ShortcutPlacement, ShortcutItem?>>> {
        return getShortcutsByRowForPage(0)
    }

    /**
     * 指定ページの行ごとに配置されたショートカットを取得
     */
    fun getShortcutsByRowForPage(pageIndex: Int): Map<Int, List<Pair<ShortcutPlacement, ShortcutItem?>>> {
        val shortcuts = getAllShortcuts()
        val placements = getPlacementsForPage(pageIndex)

        return placements
            .groupBy { it.row }
            .mapValues { (_, rowPlacements) ->
                rowPlacements
                    .sortedBy { it.column }
                    .map { placement -> placement to shortcuts[placement.shortcutId] }
            }
    }

    // ===== デフォルトレイアウト =====

    /**
     * 初回起動かどうか
     */
    fun isFirstLaunch(): Boolean {
        return !prefs.getBoolean(KEY_INITIALIZED, false)
    }

    /**
     * 初期化済みとしてマーク
     */
    fun markAsInitialized() {
        prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()
    }

    /**
     * デフォルトレイアウトを適用
     */
    fun applyDefaultLayout() {
        // 既存データをクリア
        prefs.edit()
            .remove(KEY_SHORTCUTS)
            .remove(KEY_PLACEMENTS)
            .remove(KEY_LAYOUT)
            .apply()

        // レイアウト設定を生成（日付・時計行は固定高さ）
        val rows = defaultLayout.mapIndexed { index, row ->
            val fixedHeight = getFixedHeightForRow(row)
            RowConfig(rowIndex = index, columns = row.size, fixedHeightDp = fixedHeight)
        }
        saveLayoutConfig(HomeLayoutConfig(rows))

        // 各アイテムを配置
        defaultLayout.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, itemName ->
                val itemDef = itemMapping[itemName]
                if (itemDef != null) {
                    val shortcutItem = createShortcutFromDef(itemDef)
                    if (shortcutItem != null) {
                        saveShortcut(shortcutItem)
                        savePlacement(ShortcutPlacement(
                            shortcutId = shortcutItem.id,
                            row = rowIndex,
                            column = colIndex
                        ))
                    }
                }
            }
        }

        markAsInitialized()
    }

    /**
     * 行に固定高さが必要かどうかを判定
     * 日付・時計表示は固定高さを返す
     */
    private fun getFixedHeightForRow(row: List<String>): Int? {
        val hasDateDisplay = row.any { itemMapping[it]?.type == ShortcutType.DATE_DISPLAY }
        val hasTimeDisplay = row.any { itemMapping[it]?.type == ShortcutType.TIME_DISPLAY }

        return when {
            hasTimeDisplay -> 80  // 時計は大きめ
            hasDateDisplay -> 56  // 日付は少し小さめ
            else -> null
        }
    }

    /**
     * 初期状態に戻す
     */
    fun resetToDefault() {
        applyDefaultLayout()
    }

    /**
     * レイアウトを全削除（行も配置もすべて削除）
     */
    fun clearAllLayout() {
        prefs.edit()
            .remove(KEY_SHORTCUTS)
            .remove(KEY_PLACEMENTS)
            .remove(KEY_LAYOUT)
            .apply()
        // 空のレイアウトを保存
        saveLayoutConfig(HomeLayoutConfig(rows = emptyList()))
    }

    /**
     * 指定ページのレイアウトをクリア（行と配置を削除）
     * 他のページはそのまま維持
     */
    fun clearPageLayout(pageIndex: Int) {
        // 複数のデータを一貫して更新するため、順番にロックを取得
        synchronized(placementLock) {
            synchronized(layoutLock) {
                synchronized(shortcutLock) {
                    // 該当ページの配置を削除
                    val placements = getAllPlacementsInternal().filter { it.pageIndex != pageIndex }
                    saveAllPlacements(placements)

                    // 該当ページの行を削除
                    val layout = getLayoutConfigInternal()
                    val remainingRows = layout.rows.filter { it.pageIndex != pageIndex }
                    saveLayoutConfigInternal(HomeLayoutConfig(remainingRows))

                    // 該当ページに配置されていたショートカットを削除（未配置にはしない）
                    // 注：他ページで使われていないショートカットのみ削除
                    val usedShortcutIds = placements.map { it.shortcutId }.toSet()
                    val shortcuts = getAllShortcutsInternal().filter { it.key in usedShortcutIds }
                    saveAllShortcuts(shortcuts.values.toList())
                }
            }
        }
    }

    /**
     * ItemDefからShortcutItemを生成
     * APPタイプの場合、インストールされているパッケージを探す
     */
    private fun createShortcutFromDef(def: ItemDef): ShortcutItem? {
        val label = context.getString(def.labelResId)
        return when (def.type) {
            ShortcutType.APP -> {
                // インストールされているパッケージを探す
                val installedPackage = def.packageNames.firstOrNull { pkg ->
                    isAppInstalled(pkg)
                }
                if (installedPackage != null) {
                    ShortcutItem(
                        id = UUID.randomUUID().toString(),
                        type = ShortcutType.APP,
                        label = label,
                        packageName = installedPackage
                    )
                } else {
                    null  // インストールされていない場合はスキップ
                }
            }
            else -> {
                // 内部機能の場合
                ShortcutItem(
                    id = UUID.randomUUID().toString(),
                    type = def.type,
                    label = label
                )
            }
        }
    }

    /**
     * アプリがインストールされているか確認
     */
    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    // ===== JSON変換 =====

    private fun shortcutItemToJson(item: ShortcutItem): JSONObject {
        return JSONObject().apply {
            put("id", item.id)
            put("type", item.type.name)
            put("label", item.label)
            put("packageName", item.packageName)
            put("intentUri", item.intentUri)
            put("phoneNumber", item.phoneNumber)
            put("iconUri", item.iconUri)
        }
    }

    private fun jsonToShortcutItem(json: JSONObject): ShortcutItem {
        return ShortcutItem(
            id = json.getString("id"),
            type = ShortcutType.valueOf(json.getString("type")),
            label = json.getString("label"),
            packageName = json.optString("packageName", null),
            intentUri = json.optString("intentUri", null),
            phoneNumber = json.optString("phoneNumber", null),
            iconUri = json.optString("iconUri", null)
        )
    }

    private fun placementToJson(placement: ShortcutPlacement): JSONObject {
        return JSONObject().apply {
            put("shortcutId", placement.shortcutId)
            put("pageIndex", placement.pageIndex)
            put("row", placement.row)
            put("column", placement.column)
            put("spanX", placement.spanX)
            put("spanY", placement.spanY)
            if (placement.backgroundColor != null) {
                put("backgroundColor", placement.backgroundColor)
            }
            if (placement.textColor != null) {
                put("textColor", placement.textColor)
            }
        }
    }

    private fun jsonToPlacement(json: JSONObject): ShortcutPlacement {
        return ShortcutPlacement(
            shortcutId = json.getString("shortcutId"),
            pageIndex = json.optInt("pageIndex", 0),  // マイグレーション対応
            row = json.getInt("row"),
            column = json.getInt("column"),
            spanX = json.optInt("spanX", 1),
            spanY = json.optInt("spanY", 1),
            backgroundColor = json.optString("backgroundColor", "").takeIf { it.isNotEmpty() },
            textColor = json.optString("textColor", "").takeIf { it.isNotEmpty() }
        )
    }

    // ===== Pinショートカット情報 =====

    private val pinPrefs: SharedPreferences = context.getSharedPreferences(
        PIN_PREFS_NAME, Context.MODE_PRIVATE
    )

    fun savePinShortcutInfo(itemId: String, shortcutId: String, packageName: String) {
        pinPrefs.edit()
            .putString("${itemId}_shortcut_id", shortcutId)
            .putString("${itemId}_package", packageName)
            .apply()
    }

    fun getPinShortcutInfo(itemId: String): Pair<String?, String?> {
        val shortcutId = pinPrefs.getString("${itemId}_shortcut_id", null)
        val packageName = pinPrefs.getString("${itemId}_package", null)
        return shortcutId to packageName
    }

    companion object {
        private const val PREFS_NAME = "launcher_shortcuts"
        private const val PIN_PREFS_NAME = "pin_shortcuts"
        private const val KEY_SHORTCUTS = "shortcuts"
        private const val KEY_PLACEMENTS = "placements"
        private const val KEY_LAYOUT = "layout"
        private const val KEY_INITIALIZED = "initialized"
    }
}
