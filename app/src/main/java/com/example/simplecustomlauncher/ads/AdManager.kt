package com.example.simplecustomlauncher.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 広告の状態
 */
sealed class AdState {
    data object NotLoaded : AdState()
    data object Loading : AdState()
    data object Ready : AdState()
    data object Showing : AdState()
    data class Error(val message: String) : AdState()
}

/**
 * リワード広告の管理クラス
 */
class AdManager(private val context: Context) {

    companion object {
        private const val TAG = "AdManager"

        // テスト用広告ユニットID（リリース時は本番IDに差し替え）
        private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

        // TODO: 本番用広告ユニットIDをここに設定
        // private const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-xxxx/xxxx"
    }

    private var rewardedAd: RewardedAd? = null

    private val _adState = MutableStateFlow<AdState>(AdState.NotLoaded)
    val adState: StateFlow<AdState> = _adState.asStateFlow()

    private var onRewardEarned: (() -> Unit)? = null

    /**
     * Mobile Ads SDKを初期化
     */
    fun initialize() {
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "AdMob initialized: $initializationStatus")
            loadRewardedAd()
        }
    }

    /**
     * リワード広告を読み込む
     */
    fun loadRewardedAd() {
        if (_adState.value == AdState.Loading) {
            return
        }

        _adState.value = AdState.Loading

        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            getAdUnitId(),
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded")
                    rewardedAd = ad
                    _adState.value = AdState.Ready
                    setupFullScreenContentCallback()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${loadAdError.message}")
                    rewardedAd = null
                    _adState.value = AdState.Error(loadAdError.message)
                }
            }
        )
    }

    /**
     * リワード広告を表示
     * @param activity 表示元のActivity
     * @param onRewarded 報酬獲得時のコールバック
     */
    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            Log.w(TAG, "Rewarded ad is not ready")
            _adState.value = AdState.Error("広告の準備ができていません")
            return
        }

        onRewardEarned = onRewarded
        _adState.value = AdState.Showing

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            onRewardEarned?.invoke()
            onRewardEarned = null
        }
    }

    private fun setupFullScreenContentCallback() {
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed")
                rewardedAd = null
                _adState.value = AdState.NotLoaded
                // 次の広告を事前読み込み
                loadRewardedAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                rewardedAd = null
                _adState.value = AdState.Error(adError.message)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed")
            }
        }
    }

    private fun getAdUnitId(): String {
        // TODO: BuildConfig.DEBUGで切り替え
        // return if (BuildConfig.DEBUG) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID
        return TEST_REWARDED_AD_UNIT_ID
    }

    /**
     * 広告が表示可能かどうか
     */
    fun isAdReady(): Boolean = _adState.value == AdState.Ready
}
