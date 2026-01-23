package com.example.simplecustomlauncher.data

import java.util.UUID

/**
 * メモアイテム
 */
data class MemoItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * メモ設定
 */
data class MemoSettings(
    val fontSize: Int = 20  // デフォルト20sp（大きめ）
)
