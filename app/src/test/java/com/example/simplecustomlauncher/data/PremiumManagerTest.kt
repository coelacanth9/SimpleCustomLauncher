package com.example.simplecustomlauncher.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PremiumManagerTest {

    private lateinit var context: android.app.Application
    private lateinit var premiumManager: DefaultPremiumManager
    private lateinit var settingsRepo: SettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        settingsRepo = SettingsRepository(context)
        premiumManager = DefaultPremiumManager(context, settingsRepo)
        // テストごとにクリア
        premiumManager.clearAllPremiumStatus()
    }

    // --- A: 購入済みの場合trueを返す ---

    @Test
    fun isPremiumActive_afterPurchase_returnsTrue() {
        premiumManager.recordPurchase()

        assertTrue(premiumManager.isPremiumActive())
        val status = premiumManager.getPremiumStatus()
        assertTrue(status.activeSources.contains(PremiumSource.ONE_TIME_PURCHASE))
    }

    // --- A: 広告視聴後24時間以内の場合trueを返す ---

    @Test
    fun isPremiumActive_afterAdWatch_withinExpiry_returnsTrue() {
        premiumManager.recordAdWatch()

        assertTrue(premiumManager.isPremiumActive())
        val status = premiumManager.getPremiumStatus()
        assertTrue(status.activeSources.contains(PremiumSource.AD_WATCH))
        assertNotNull(status.adWatchExpiresAt)
    }

    // --- A: 広告視聴後24時間超過の場合falseを返す ---

    @Test
    fun isPremiumActive_afterAdWatch_expired_returnsFalse() {
        // SharedPreferences に過去の有効期限を直接設定
        val prefs = context.getSharedPreferences("premium_status", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("ad_watch_expiry", System.currentTimeMillis() - 1000)
            .apply()

        assertFalse(premiumManager.isPremiumActive())
        val status = premiumManager.getPremiumStatus()
        assertFalse(status.activeSources.contains(PremiumSource.AD_WATCH))
    }

    // --- A: 課金+広告視聴の両方がactive時に広告期限切れしても課金側がfalseにならない ---

    @Test
    fun isPremiumActive_purchaseAndAdWatch_adExpired_stillTrue() {
        // 購入を記録
        premiumManager.recordPurchase()

        // 広告視聴の有効期限を過去に設定（期限切れ）
        val prefs = context.getSharedPreferences("premium_status", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("ad_watch_expiry", System.currentTimeMillis() - 1000)
            .apply()

        // 購入があるのでプレミアムは有効
        assertTrue(premiumManager.isPremiumActive())
        val status = premiumManager.getPremiumStatus()
        assertTrue(status.activeSources.contains(PremiumSource.ONE_TIME_PURCHASE))
        assertFalse(status.activeSources.contains(PremiumSource.AD_WATCH))
    }

    // --- A: プレミアム時はpageCount-1を返す ---

    @Test
    fun getMaxAccessiblePageIndex_premium_returnsPageCountMinusOne() {
        settingsRepo.pageCount = 3
        premiumManager.recordPurchase()

        assertEquals(2, premiumManager.getMaxAccessiblePageIndex())
    }

    // --- A: 非プレミアム時は0を返す ---

    @Test
    fun getMaxAccessiblePageIndex_notPremium_returnsZero() {
        settingsRepo.pageCount = 3

        assertEquals(0, premiumManager.getMaxAccessiblePageIndex())
    }
}
