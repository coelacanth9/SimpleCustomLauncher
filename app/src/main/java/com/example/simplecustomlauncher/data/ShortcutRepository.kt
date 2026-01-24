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

    // ===== ショートカット =====

    fun saveShortcut(item: ShortcutItem) {
        val shortcuts = getAllShortcuts().toMutableMap()
        shortcuts[item.id] = item
        saveAllShortcuts(shortcuts.values.toList())
    }

    fun deleteShortcut(id: String) {
        val shortcuts = getAllShortcuts().toMutableMap()
        shortcuts.remove(id)
        saveAllShortcuts(shortcuts.values.toList())

        // 配置情報も削除
        val placements = getAllPlacements().filter { it.shortcutId != id }
        saveAllPlacements(placements)
    }

    fun getShortcut(id: String): ShortcutItem? {
        return getAllShortcuts()[id]
    }

    fun getAllShortcuts(): Map<String, ShortcutItem> {
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
        val placements = getAllPlacements().toMutableList()
        // 同じショートカットの既存配置を削除
        placements.removeAll { it.shortcutId == placement.shortcutId }
        // 同じ位置の既存配置も削除（上書き）
        placements.removeAll { it.row == placement.row && it.column == placement.column }
        placements.add(placement)
        saveAllPlacements(placements)
    }

    fun removePlacement(shortcutId: String) {
        val placements = getAllPlacements().filter { it.shortcutId != shortcutId }
        saveAllPlacements(placements)
    }

    fun getAllPlacements(): List<ShortcutPlacement> {
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
        val array = JSONArray()
        config.rows.forEach { row ->
            array.put(JSONObject().apply {
                put("rowIndex", row.rowIndex)
                put("columns", row.columns)
            })
        }
        prefs.edit().putString(KEY_LAYOUT, array.toString()).apply()
    }

    fun getLayoutConfig(): HomeLayoutConfig {
        val json = prefs.getString(KEY_LAYOUT, null) ?: return HomeLayoutConfig()
        return try {
            val array = JSONArray(json)
            val rows = (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                RowConfig(
                    rowIndex = obj.getInt("rowIndex"),
                    columns = obj.getInt("columns")
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
     * 行ごとに配置されたショートカットを取得
     */
    fun getShortcutsByRow(): Map<Int, List<Pair<ShortcutPlacement, ShortcutItem?>>> {
        val shortcuts = getAllShortcuts()
        val placements = getAllPlacements()

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

        // レイアウト設定を生成
        val rows = defaultLayout.mapIndexed { index, row ->
            RowConfig(rowIndex = index, columns = row.size)
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
     * ItemDefからShortcutItemを生成
     * APPタイプの場合、インストールされているパッケージを探す
     */
    private fun createShortcutFromDef(def: ItemDef): ShortcutItem? {
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
                        label = def.label,
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
                    label = def.label
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
            put("row", placement.row)
            put("column", placement.column)
            put("spanX", placement.spanX)
            put("spanY", placement.spanY)
        }
    }

    private fun jsonToPlacement(json: JSONObject): ShortcutPlacement {
        return ShortcutPlacement(
            shortcutId = json.getString("shortcutId"),
            row = json.getInt("row"),
            column = json.getInt("column"),
            spanX = json.optInt("spanX", 1),
            spanY = json.optInt("spanY", 1)
        )
    }

    companion object {
        private const val PREFS_NAME = "launcher_shortcuts"
        private const val KEY_SHORTCUTS = "shortcuts"
        private const val KEY_PLACEMENTS = "placements"
        private const val KEY_LAYOUT = "layout"
        private const val KEY_INITIALIZED = "initialized"
    }
}
