package com.example.simplecustomlauncher.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Google Play Billing の管理クラス
 */
class BillingManager(
    private val context: Context,
    private val onPurchaseComplete: () -> Unit
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        private const val RECONNECT_DELAY_MS = 5000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var billingClient: BillingClient? = null
    private var cachedProductDetails: ProductDetails? = null

    // 接続状態
    private val _connectionState = MutableStateFlow<BillingConnectionState>(BillingConnectionState.NotConnected)
    val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    // 商品情報
    private val _productInfo = MutableStateFlow<ProductInfo?>(null)
    val productInfo: StateFlow<ProductInfo?> = _productInfo.asStateFlow()

    // 購入状態
    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    // 購入済みフラグ
    private val _isPurchased = MutableStateFlow(false)
    val isPurchased: StateFlow<Boolean> = _isPurchased.asStateFlow()

    /**
     * BillingClient を初期化して接続開始
     */
    fun initialize() {
        if (billingClient != null) return

        _connectionState.value = BillingConnectionState.Connecting

        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        startConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "BillingClient connected")
                    _connectionState.value = BillingConnectionState.Connected
                    queryProductDetails()
                    restorePurchases()
                } else {
                    Log.e(TAG, "BillingClient connection failed: ${result.debugMessage}")
                    _connectionState.value = BillingConnectionState.Error(
                        result.debugMessage,
                        result.responseCode
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "BillingClient disconnected")
                _connectionState.value = BillingConnectionState.NotConnected
                // 自動再接続
                scope.launch {
                    delay(RECONNECT_DELAY_MS)
                    startConnection()
                }
            }
        })
    }

    /**
     * 商品情報を取得
     */
    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingConstants.PRODUCT_ID_PREMIUM_UNLOCK)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { result, productDetailsList ->
            Log.d(TAG, "queryProductDetails response: ${result.responseCode}, list size: ${productDetailsList.size}")
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                if (productDetailsList.isEmpty()) {
                    Log.w(TAG, "Product list is empty - product may not be available yet")
                }
                productDetailsList.firstOrNull()?.let { details ->
                    cachedProductDetails = details
                    val offerDetails = details.oneTimePurchaseOfferDetails
                    if (offerDetails != null) {
                        _productInfo.value = ProductInfo(
                            productId = details.productId,
                            title = details.title,
                            description = details.description,
                            formattedPrice = offerDetails.formattedPrice,
                            priceAmountMicros = offerDetails.priceAmountMicros,
                            priceCurrencyCode = offerDetails.priceCurrencyCode
                        )
                        Log.d(TAG, "Product details loaded: ${offerDetails.formattedPrice}")
                    } else {
                        Log.w(TAG, "oneTimePurchaseOfferDetails is null")
                    }
                }
            } else {
                Log.e(TAG, "queryProductDetails failed: ${result.debugMessage}")
            }
        }
    }

    /**
     * 購入を復元（アプリ起動時に呼び出し）
     */
    fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient?.queryPurchasesAsync(params) { result, purchasesList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val premiumPurchase = purchasesList.find { purchase ->
                    purchase.products.contains(BillingConstants.PRODUCT_ID_PREMIUM_UNLOCK) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }

                if (premiumPurchase != null) {
                    Log.d(TAG, "Premium purchase found, restoring...")
                    _isPurchased.value = true
                    onPurchaseComplete()

                    // 未確認の購入があれば確認
                    if (!premiumPurchase.isAcknowledged) {
                        acknowledgePurchase(premiumPurchase)
                    }
                }
            } else {
                Log.e(TAG, "restorePurchases failed: ${result.debugMessage}")
            }
        }
    }

    /**
     * 購入フローを起動
     */
    fun launchPurchaseFlow(activity: Activity) {
        val details = cachedProductDetails
        if (details == null) {
            Log.e(TAG, "Product details not loaded")
            _purchaseState.value = PurchaseState.Error("商品情報を読み込めませんでした")
            return
        }

        val client = billingClient
        if (client == null) {
            Log.e(TAG, "BillingClient not initialized")
            _purchaseState.value = PurchaseState.Error("課金サービスに接続できません")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        _purchaseState.value = PurchaseState.Pending
        val result = client.launchBillingFlow(activity, billingFlowParams)

        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "launchBillingFlow failed: ${result.debugMessage}")
            _purchaseState.value = PurchaseState.Error("購入を開始できませんでした")
        }
    }

    /**
     * 購入結果のコールバック
     */
    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.d(TAG, "Purchase updated: OK")
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "Purchase canceled by user")
                _purchaseState.value = PurchaseState.Idle
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned, restoring...")
                restorePurchases()
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${result.debugMessage}")
                _purchaseState.value = PurchaseState.Error("購入に失敗しました")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                // 重要: 先に特典付与、その後acknowledge
                _isPurchased.value = true
                onPurchaseComplete()
                acknowledgePurchase(purchase)
            } else {
                completePurchase()
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "Purchase pending")
            _purchaseState.value = PurchaseState.Pending
        }
    }

    /**
     * 購入確認（3日以内に必須）
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(params) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged")
                _purchaseState.value = PurchaseState.Purchased
            } else {
                Log.e(TAG, "Acknowledge failed: ${result.debugMessage}")
                // 特典は既に付与済みなので、状態は Purchased にする
                // 次回起動時に再度 acknowledge を試みる
                _purchaseState.value = PurchaseState.Purchased
            }
        }
    }

    private fun completePurchase() {
        _isPurchased.value = true
        _purchaseState.value = PurchaseState.Purchased
        onPurchaseComplete()
    }

    /**
     * 接続を終了
     */
    fun endConnection() {
        // CoroutineScopeをキャンセルして再接続ループを停止
        scope.cancel()
        billingClient?.endConnection()
        billingClient = null
        cachedProductDetails = null
        _connectionState.value = BillingConnectionState.NotConnected
    }
}
