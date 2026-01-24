package com.example.simplecustomlauncher.data

import android.content.Context
import android.content.SharedPreferences

/**
 * タップ操作モード
 */
enum class TapMode {
    SINGLE_TAP,  // タップで起動
    LONG_TAP     // ロングタップで起動
}

/**
 * アプリ設定の永続化
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    /**
     * タップ操作モード
     */
    var tapMode: TapMode
        get() {
            val value = prefs.getString(KEY_TAP_MODE, TapMode.SINGLE_TAP.name)
            return try {
                TapMode.valueOf(value ?: TapMode.SINGLE_TAP.name)
            } catch (e: Exception) {
                TapMode.SINGLE_TAP
            }
        }
        set(value) = prefs.edit().putString(KEY_TAP_MODE, value.name).apply()

    /**
     * 起動時に確認ダイアログを表示するか
     */
    var showConfirmDialog: Boolean
        get() = prefs.getBoolean(KEY_CONFIRM_DIALOG, false)
        set(value) = prefs.edit().putBoolean(KEY_CONFIRM_DIALOG, value).apply()

    /**
     * タップ時に振動フィードバックを行うか
     */
    var tapFeedback: Boolean
        get() = prefs.getBoolean(KEY_TAP_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_TAP_FEEDBACK, value).apply()

    companion object {
        private const val PREFS_NAME = "launcher_settings"
        private const val KEY_TAP_MODE = "tap_mode"
        private const val KEY_CONFIRM_DIALOG = "show_confirm_dialog"
        private const val KEY_TAP_FEEDBACK = "tap_feedback"
    }
}
