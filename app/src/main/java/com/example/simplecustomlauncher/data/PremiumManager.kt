package com.example.simplecustomlauncher.data

import android.content.Context
import android.content.SharedPreferences

/**
 * プレミアム機能の解除方法
 */
enum class PremiumSource {
    AD_WATCH,           // 動画広告視聴（24時間有効）
    ONE_TIME_PURCHASE,  // 買い切り購入（永続）
    SUBSCRIPTION        // サブスクリプション（期間中有効）
}

/**
 * プレミアム状態
 */
data class PremiumStatus(
    val isActive: Boolean,
    val activeSources: Set<PremiumSource>,
    val adWatchExpiresAt: Long? = null
)

/**
 * プレミアム機能の管理インターフェース
 */
interface PremiumManager {
    /**
     * プレミアムが有効かどうか
     */
    fun isPremiumActive(): Boolean

    /**
     * プレミアム状態の詳細を取得
     */
    fun getPremiumStatus(): PremiumStatus

    /**
     * 動画広告視聴を記録（24時間有効）
     */
    fun recordAdWatch()

    /**
     * 買い切り購入を記録
     */
    fun recordPurchase()

    /**
     * サブスクリプション状態を更新
     */
    fun updateSubscriptionStatus(isActive: Boolean)

    /**
     * アクセス可能な最大ページインデックスを取得
     * プレミアム有効時: 設定されたページ数 - 1
     * プレミアム無効時: 0（1ページ目のみ）
     */
    fun getMaxAccessiblePageIndex(): Int
}

/**
 * PremiumManager のデフォルト実装
 * SharedPreferences で状態を永続化
 */
class DefaultPremiumManager(
    context: Context,
    private val settingsRepository: SettingsRepository
) : PremiumManager {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    override fun isPremiumActive(): Boolean {
        return getPremiumStatus().isActive
    }

    override fun getPremiumStatus(): PremiumStatus {
        val sources = mutableSetOf<PremiumSource>()
        var adWatchExpiresAt: Long? = null

        // 買い切り購入チェック
        if (prefs.getBoolean(KEY_ONE_TIME_PURCHASE, false)) {
            sources.add(PremiumSource.ONE_TIME_PURCHASE)
        }

        // サブスクリプションチェック
        if (prefs.getBoolean(KEY_SUBSCRIPTION_ACTIVE, false)) {
            sources.add(PremiumSource.SUBSCRIPTION)
        }

        // 動画広告視聴チェック（24時間有効）
        val adExpiry = prefs.getLong(KEY_AD_WATCH_EXPIRY, 0)
        if (adExpiry > System.currentTimeMillis()) {
            sources.add(PremiumSource.AD_WATCH)
            adWatchExpiresAt = adExpiry
        }

        return PremiumStatus(
            isActive = sources.isNotEmpty(),
            activeSources = sources,
            adWatchExpiresAt = adWatchExpiresAt
        )
    }

    override fun recordAdWatch() {
        val expiryTime = System.currentTimeMillis() + AD_WATCH_DURATION_MS
        prefs.edit()
            .putLong(KEY_AD_WATCH_EXPIRY, expiryTime)
            .apply()
    }

    override fun recordPurchase() {
        prefs.edit()
            .putBoolean(KEY_ONE_TIME_PURCHASE, true)
            .apply()
    }

    override fun updateSubscriptionStatus(isActive: Boolean) {
        prefs.edit()
            .putBoolean(KEY_SUBSCRIPTION_ACTIVE, isActive)
            .apply()
    }

    override fun getMaxAccessiblePageIndex(): Int {
        return if (isPremiumActive()) {
            settingsRepository.pageCount - 1
        } else {
            0  // 非プレミアム時は1ページ目のみ
        }
    }

    companion object {
        private const val PREFS_NAME = "premium_status"
        private const val KEY_ONE_TIME_PURCHASE = "one_time_purchase"
        private const val KEY_SUBSCRIPTION_ACTIVE = "subscription_active"
        private const val KEY_AD_WATCH_EXPIRY = "ad_watch_expiry"

        // 動画広告視聴の有効期間（24時間）
        private const val AD_WATCH_DURATION_MS = 24 * 60 * 60 * 1000L
        // 動画広告視聴の有効期間テスト用（1分）
        //private const val AD_WATCH_DURATION_MS = 1* 60 * 1000L

    }
}
