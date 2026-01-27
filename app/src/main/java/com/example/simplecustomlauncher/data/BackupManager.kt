package com.example.simplecustomlauncher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * バックアップ/リストア管理クラス
 */
class BackupManager(private val context: Context) {

    private val shortcutRepository = ShortcutRepository(context)
    private val settingsRepository = SettingsRepository(context)

    companion object {
        private const val BACKUP_VERSION = 1
        private const val KEY_VERSION = "version"
        private const val KEY_SHORTCUTS = "shortcuts"
        private const val KEY_PLACEMENTS = "placements"
        private const val KEY_LAYOUT = "layout"
        private const val KEY_SETTINGS = "settings"
    }

    /**
     * バックアップデータをJSON文字列として生成
     */
    fun createBackupJson(): String {
        val backup = JSONObject()

        // バージョン
        backup.put(KEY_VERSION, BACKUP_VERSION)

        // ショートカット
        val shortcuts = shortcutRepository.getAllShortcuts()
        val shortcutsArray = JSONArray()
        shortcuts.values.forEach { item ->
            shortcutsArray.put(JSONObject().apply {
                put("id", item.id)
                put("type", item.type.name)
                put("label", item.label)
                put("packageName", item.packageName ?: "")
                put("intentUri", item.intentUri ?: "")
                put("phoneNumber", item.phoneNumber ?: "")
                put("iconUri", item.iconUri ?: "")
            })
        }
        backup.put(KEY_SHORTCUTS, shortcutsArray)

        // 配置情報
        val placements = shortcutRepository.getAllPlacements()
        val placementsArray = JSONArray()
        placements.forEach { placement ->
            placementsArray.put(JSONObject().apply {
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
            })
        }
        backup.put(KEY_PLACEMENTS, placementsArray)

        // レイアウト
        val layout = shortcutRepository.getLayoutConfig()
        val layoutArray = JSONArray()
        layout.rows.forEach { row ->
            layoutArray.put(JSONObject().apply {
                put("pageIndex", row.pageIndex)
                put("rowIndex", row.rowIndex)
                put("columns", row.columns)
                if (row.fixedHeightDp != null) {
                    put("fixedHeightDp", row.fixedHeightDp)
                }
                put("textOnly", row.textOnly)
            })
        }
        backup.put(KEY_LAYOUT, layoutArray)

        // 設定
        val settingsObj = JSONObject().apply {
            put("themeMode", settingsRepository.themeMode.name)
            put("tapMode", settingsRepository.tapMode.name)
            put("showConfirmDialog", settingsRepository.showConfirmDialog)
            put("tapFeedback", settingsRepository.tapFeedback)
            put("loopPaging", settingsRepository.loopPagingEnabled)
            put("pageCount", settingsRepository.pageCount)
        }
        backup.put(KEY_SETTINGS, settingsObj)

        return backup.toString(2) // インデント付きで見やすく
    }

    /**
     * バックアップファイルを作成して共有インテントを返す
     */
    fun createShareIntent(): Intent {
        val json = createBackupJson()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "launcher_backup_$timestamp.json"

        // キャッシュディレクトリにファイルを作成
        val file = File(context.cacheDir, fileName)
        file.writeText(json)

        // FileProviderでURIを取得
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Launcher Backup")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * URIからバックアップを復元
     * @return 成功したらtrue
     */
    fun restoreFromUri(uri: Uri): RestoreResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return RestoreResult.Error("ファイルを開けませんでした")

            val json = inputStream.bufferedReader().use { it.readText() }
            restoreFromJson(json)
        } catch (e: Exception) {
            RestoreResult.Error("復元に失敗しました: ${e.message}")
        }
    }

    /**
     * JSON文字列からバックアップを復元
     */
    fun restoreFromJson(json: String): RestoreResult {
        return try {
            val backup = JSONObject(json)

            // バージョンチェック
            val version = backup.optInt(KEY_VERSION, 0)
            if (version > BACKUP_VERSION) {
                return RestoreResult.Error("新しいバージョンのバックアップです。アプリを更新してください。")
            }

            // ショートカット復元
            val shortcutsArray = backup.getJSONArray(KEY_SHORTCUTS)
            val shortcuts = mutableListOf<ShortcutItem>()
            for (i in 0 until shortcutsArray.length()) {
                val obj = shortcutsArray.getJSONObject(i)
                shortcuts.add(ShortcutItem(
                    id = obj.getString("id"),
                    type = ShortcutType.valueOf(obj.getString("type")),
                    label = obj.getString("label"),
                    packageName = obj.optString("packageName").takeIf { it.isNotEmpty() },
                    intentUri = obj.optString("intentUri").takeIf { it.isNotEmpty() },
                    phoneNumber = obj.optString("phoneNumber").takeIf { it.isNotEmpty() },
                    iconUri = obj.optString("iconUri").takeIf { it.isNotEmpty() }
                ))
            }

            // 配置情報復元
            val placementsArray = backup.getJSONArray(KEY_PLACEMENTS)
            val placements = mutableListOf<ShortcutPlacement>()
            for (i in 0 until placementsArray.length()) {
                val obj = placementsArray.getJSONObject(i)
                placements.add(ShortcutPlacement(
                    shortcutId = obj.getString("shortcutId"),
                    pageIndex = obj.optInt("pageIndex", 0),
                    row = obj.getInt("row"),
                    column = obj.getInt("column"),
                    spanX = obj.optInt("spanX", 1),
                    spanY = obj.optInt("spanY", 1),
                    backgroundColor = obj.optString("backgroundColor", "").takeIf { it.isNotEmpty() },
                    textColor = obj.optString("textColor", "").takeIf { it.isNotEmpty() }
                ))
            }

            // レイアウト復元
            val layoutArray = backup.getJSONArray(KEY_LAYOUT)
            val rows = mutableListOf<RowConfig>()
            for (i in 0 until layoutArray.length()) {
                val obj = layoutArray.getJSONObject(i)
                rows.add(RowConfig(
                    pageIndex = obj.optInt("pageIndex", 0),
                    rowIndex = obj.getInt("rowIndex"),
                    columns = obj.getInt("columns"),
                    fixedHeightDp = if (obj.has("fixedHeightDp")) obj.getInt("fixedHeightDp") else null,
                    textOnly = obj.optBoolean("textOnly", false)
                ))
            }

            // 設定復元
            val settingsObj = backup.optJSONObject(KEY_SETTINGS)
            if (settingsObj != null) {
                settingsRepository.themeMode = try {
                    ThemeMode.valueOf(settingsObj.getString("themeMode"))
                } catch (e: Exception) {
                    ThemeMode.SYSTEM
                }
                settingsRepository.tapMode = try {
                    TapMode.valueOf(settingsObj.getString("tapMode"))
                } catch (e: Exception) {
                    TapMode.SINGLE_TAP
                }
                settingsRepository.showConfirmDialog = settingsObj.optBoolean("showConfirmDialog", true)
                settingsRepository.tapFeedback = settingsObj.optBoolean("tapFeedback", true)
                settingsRepository.loopPagingEnabled = settingsObj.optBoolean("loopPaging", false)
                settingsRepository.pageCount = settingsObj.optInt("pageCount", 1)
            }

            // データを保存（既存データはクリア）
            shortcutRepository.clearAllLayout()
            shortcuts.forEach { shortcutRepository.saveShortcut(it) }
            placements.forEach { shortcutRepository.savePlacement(it) }
            shortcutRepository.saveLayoutConfig(HomeLayoutConfig(rows))

            RestoreResult.Success(
                shortcutCount = shortcuts.size,
                pageCount = (rows.maxOfOrNull { it.pageIndex } ?: 0) + 1
            )
        } catch (e: Exception) {
            RestoreResult.Error("復元に失敗しました: ${e.message}")
        }
    }
}

/**
 * 復元結果
 */
sealed class RestoreResult {
    data class Success(val shortcutCount: Int, val pageCount: Int) : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}
