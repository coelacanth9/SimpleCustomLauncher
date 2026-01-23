package com.example.simplecustomlauncher.data

import android.content.Context
import android.content.SharedPreferences

/**
 * アプリ設定の永続化
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    /**
     * タップ時に確認ダイアログを表示するか
     */
    var showConfirmDialog: Boolean
        get() = prefs.getBoolean(KEY_CONFIRM_DIALOG, false)
        set(value) = prefs.edit().putBoolean(KEY_CONFIRM_DIALOG, value).apply()

    companion object {
        private const val PREFS_NAME = "launcher_settings"
        private const val KEY_CONFIRM_DIALOG = "show_confirm_dialog"
    }
}
