package com.example.simplecustomlauncher.data

/**
 * デフォルト配置用のアイテム定義
 */
data class ItemDef(
    val type: ShortcutType,
    val label: String,
    val packageNames: List<String> = emptyList()  // APPの場合、優先順位付きで複数指定
)

/**
 * 使用可能なアイテムのマッピング
 * 初期配置に使いたいアイテムをここに定義しておく
 */
val itemMapping = mapOf(
    // 内部機能
    "電話" to ItemDef(ShortcutType.DIALER, "電話"),
    "メモ帳" to ItemDef(ShortcutType.MEMO, "メモ帳"),
    "カレンダー" to ItemDef(ShortcutType.CALENDAR, "カレンダー"),

    // よく使うアプリ
    "フォト" to ItemDef(ShortcutType.APP, "フォト", listOf("com.google.android.apps.photos")),
    "Google" to ItemDef(ShortcutType.APP, "Google", listOf("com.google.android.googlequicksearchbox")),
    "連絡先" to ItemDef(ShortcutType.APP, "連絡先", listOf("com.google.android.contacts", "com.android.contacts")),
    "カメラ" to ItemDef(ShortcutType.APP, "カメラ", listOf("com.google.android.GoogleCamera", "com.android.camera", "com.android.camera2")),
    "LINE" to ItemDef(ShortcutType.APP, "LINE", listOf("jp.naver.line.android")),
    "メッセージ" to ItemDef(ShortcutType.APP, "メッセージ", listOf("com.google.android.apps.messaging", "com.android.mms")),
    "Chrome" to ItemDef(ShortcutType.APP, "Chrome", listOf("com.android.chrome")),
    "YouTube" to ItemDef(ShortcutType.APP, "YouTube", listOf("com.google.android.youtube")),
    "Gmail" to ItemDef(ShortcutType.APP, "Gmail", listOf("com.google.android.gm")),
    "マップ" to ItemDef(ShortcutType.APP, "マップ", listOf("com.google.android.apps.maps")),
    "設定" to ItemDef(ShortcutType.APP, "設定", listOf("com.android.settings")),
)

/**
 * デフォルトのレイアウト配置
 * 行ごとのリスト、要素数がそのまま列数になる
 */
val defaultLayout = listOf(
    listOf("電話", "メモ帳"),
    listOf("フォト", "Google", "連絡先"),
    listOf("カレンダー"),
)
