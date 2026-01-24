package com.example.simplecustomlauncher.data

import android.content.Intent
import android.net.Uri

/**
 * ショートカットの種類
 */
enum class ShortcutType {
    APP,            // アプリ起動
    PHONE,          // 電話をかける
    SMS,            // SMSを送る
    DIALER,         // 電話アプリ（キーパッド画面で開く）
    INTENT,         // 外部アプリから受け取ったIntent（LINE等）
    CALENDAR,       // アプリ内カレンダー
    MEMO,           // アプリ内メモ帳
    SETTINGS,       // アプリ設定
    EMPTY           // 空きスロット
}

/**
 * ショートカットのデータ
 */
data class ShortcutItem(
    val id: String,
    val type: ShortcutType,
    val label: String,
    val packageName: String? = null,
    val intentUri: String? = null,
    val phoneNumber: String? = null,
    val iconUri: String? = null
) {
    /**
     * 起動用のIntentを生成
     */
    fun toIntent(): Intent? {
        return when (type) {
            ShortcutType.APP -> {
                packageName?.let { pkg ->
                    Intent(Intent.ACTION_MAIN).apply {
                        setPackage(pkg)
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                }
            }
            ShortcutType.PHONE -> {
                phoneNumber?.let { number ->
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                }
            }
            ShortcutType.SMS -> {
                phoneNumber?.let { number ->
                    Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
                }
            }
            ShortcutType.DIALER -> {
                Intent(Intent.ACTION_DIAL)
            }
            ShortcutType.INTENT -> {
                intentUri?.let { uri ->
                    try {
                        Intent.parseUri(uri, 0)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            ShortcutType.CALENDAR -> null
            ShortcutType.MEMO -> null
            ShortcutType.SETTINGS -> null
            ShortcutType.EMPTY -> null
        }
    }
}

/**
 * ショートカットの配置情報
 * 将来の拡張に備えてグリッド座標で保持
 */
data class ShortcutPlacement(
    val shortcutId: String,
    val row: Int,
    val column: Int,
    val spanX: Int = 1,  // 横に何セル分（今は1〜3、将来拡張可能）
    val spanY: Int = 1   // 縦に何セル分（今は1固定、将来拡張可能）
)

/**
 * 行の設定
 */
data class RowConfig(
    val rowIndex: Int,
    val columns: Int = 2  // この行の分割数（1〜3）
)

/**
 * ホーム画面全体のレイアウト設定
 */
data class HomeLayoutConfig(
    val rows: List<RowConfig> = emptyList()
) {
    /**
     * 最初の空きスロットを探す
     * @return Pair(rowIndex, columnIndex) or null
     */
    fun findFirstEmptySlot(placements: List<ShortcutPlacement>): Pair<Int, Int>? {
        val occupied = placements.map { it.row to it.column }.toSet()

        for (row in rows) {
            for (col in 0 until row.columns) {
                if ((row.rowIndex to col) !in occupied) {
                    return row.rowIndex to col
                }
            }
        }
        return null
    }

    /**
     * 総スロット数
     */
    fun totalSlots(): Int = rows.sumOf { it.columns }

    /**
     * 指定行の分割数を取得
     */
    fun getColumnsForRow(rowIndex: Int): Int {
        return rows.find { it.rowIndex == rowIndex }?.columns ?: 2
    }
}
