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
 * テーマモード
 */
enum class ThemeMode {
    LIGHT,   // ライトモード
    DARK,    // ダークモード
    SYSTEM   // 端末設定に合わせる
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

    /**
     * テーマモード
     */
    var themeMode: ThemeMode
        get() {
            val value = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            return try {
                ThemeMode.valueOf(value ?: ThemeMode.SYSTEM.name)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            }
        }
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    /**
     * ループページングが有効かどうか
     * 有効時: 最後のページから最初のページへ、最初のページから最後のページへスワイプ可能
     */
    var loopPagingEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOOP_PAGING, false)
        set(value) = prefs.edit().putBoolean(KEY_LOOP_PAGING, value).apply()

    /**
     * ホーム画面のページ数（1〜5）
     */
    var pageCount: Int
        get() = prefs.getInt(KEY_PAGE_COUNT, 1).coerceIn(1, MAX_PAGES)
        set(value) = prefs.edit().putInt(KEY_PAGE_COUNT, value.coerceIn(1, MAX_PAGES)).apply()

    /**
     * 初回起動時のオンボーディングダイアログを表示済みか
     */
    var onboardingShown: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_SHOWN, value).apply()

    companion object {
        private const val PREFS_NAME = "launcher_settings"
        private const val KEY_TAP_MODE = "tap_mode"
        private const val KEY_CONFIRM_DIALOG = "show_confirm_dialog"
        private const val KEY_TAP_FEEDBACK = "tap_feedback"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LOOP_PAGING = "loop_paging"
        private const val KEY_PAGE_COUNT = "page_count"
        private const val KEY_ONBOARDING_SHOWN = "onboarding_shown"

        const val MAX_PAGES = 5
    }
}
