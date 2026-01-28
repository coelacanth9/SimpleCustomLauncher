package com.example.simplecustomlauncher.billing

import com.example.simplecustomlauncher.BuildConfig
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
    private val onPurchaseComplete: () -> Unit,
    private val onPurchaseCleared: () -> Unit = {}
) : PurchasesUpdatedListener {

    companion object {
        // 統一TAG: "Billing" でフィルタすれば全て見える
        private const val TAG = "Billing"
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
        Log.d(TAG, "========== initialize() 開始 ==========")
        Log.d(TAG, "Version: ${BuildConfig.VERSION_NAME}")

        if (billingClient != null) {
            Log.d(TAG, "initialize(): billingClient already exists, skipping")
            return
        }

        Log.d(TAG, "initialize(): Creating new BillingClient...")
        _connectionState.value = BillingConnectionState.Connecting

        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        Log.d(TAG, "initialize(): BillingClient created, starting connection...")
        startConnection()
    }

    private fun startConnection() {
        Log.d(TAG, "---------- startConnection() ----------")
        Log.d(TAG, "startConnection(): billingClient exists = ${billingClient != null}")

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                Log.d(TAG, "onBillingSetupFinished(): responseCode=${result.responseCode}, debugMessage=${result.debugMessage}")

                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "onBillingSetupFinished(): Connection SUCCESS")
                    _connectionState.value = BillingConnectionState.Connected

                    Log.d(TAG, "onBillingSetupFinished(): Calling queryProductDetails()...")
                    queryProductDetails()

                    Log.d(TAG, "onBillingSetupFinished(): Calling restorePurchases()...")
                    restorePurchases()
                } else {
                    Log.e(TAG, "onBillingSetupFinished(): Connection FAILED - code=${result.responseCode}, msg=${result.debugMessage}")
                    _connectionState.value = BillingConnectionState.Error(
                        result.debugMessage,
                        result.responseCode
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "onBillingServiceDisconnected(): Service disconnected, will retry in ${RECONNECT_DELAY_MS}ms")
                _connectionState.value = BillingConnectionState.NotConnected
                // 自動再接続
                scope.launch {
                    delay(RECONNECT_DELAY_MS)
                    Log.d(TAG, "onBillingServiceDisconnected(): Retrying connection...")
                    startConnection()
                }
            }
        })
    }

    /**
     * 商品情報を取得
     */
    private fun queryProductDetails() {
        Log.d(TAG, "========== queryProductDetails() 開始 ==========")
        Log.d(TAG, "queryProductDetails(): billingClient exists = ${billingClient != null}")
        Log.d(TAG, "queryProductDetails(): productId = ${BillingConstants.PRODUCT_ID_PREMIUM_UNLOCK}")

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingConstants.PRODUCT_ID_PREMIUM_UNLOCK)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        Log.d(TAG, "queryProductDetails(): Calling queryProductDetailsAsync...")

        billingClient?.queryProductDetailsAsync(params) { result, productDetailsList ->
            try {
                Log.d(TAG, "---------- queryProductDetailsAsync CALLBACK ----------")
                Log.d(TAG, "queryProductDetailsAsync: responseCode=${result.responseCode}")
                Log.d(TAG, "queryProductDetailsAsync: debugMessage=${result.debugMessage}")
                Log.d(TAG, "queryProductDetailsAsync: productDetailsList.size=${productDetailsList.size}")

                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "queryProductDetailsAsync: Response OK")

                    if (productDetailsList.isEmpty()) {
                        Log.w(TAG, "queryProductDetailsAsync: Product list is EMPTY - product may not be available")
                    }

                    val details = productDetailsList.firstOrNull()
                    if (details != null) {
                        Log.d(TAG, "Product found: productId=${details.productId}")
                        Log.d(TAG, "Product found: title=${details.title}")
                        Log.d(TAG, "Product found: description=${details.description}")

                        cachedProductDetails = details
                        val offerDetails = details.oneTimePurchaseOfferDetails

                        if (offerDetails != null) {
                            Log.d(TAG, "OfferDetails: formattedPrice=${offerDetails.formattedPrice}")
                            Log.d(TAG, "OfferDetails: priceAmountMicros=${offerDetails.priceAmountMicros}")
                            Log.d(TAG, "OfferDetails: priceCurrencyCode=${offerDetails.priceCurrencyCode}")

                            _productInfo.value = ProductInfo(
                                productId = details.productId,
                                title = details.title,
                                description = details.description,
                                formattedPrice = offerDetails.formattedPrice,
                                priceAmountMicros = offerDetails.priceAmountMicros,
                                priceCurrencyCode = offerDetails.priceCurrencyCode
                            )
                            Log.d(TAG, "Product info stored successfully")
                        } else {
                            Log.w(TAG, "queryProductDetailsAsync: oneTimePurchaseOfferDetails is NULL")
                        }
                    } else {
                        Log.w(TAG, "queryProductDetailsAsync: No product details found")
                    }
                } else {
                    Log.e(TAG, "queryProductDetailsAsync: FAILED - code=${result.responseCode}, msg=${result.debugMessage}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "queryProductDetailsAsync: EXCEPTION - ${e.message}", e)
            }

            Log.d(TAG, "========== queryProductDetails() 完了 ==========")
        }
    }

    /**
     * 購入を復元（アプリ起動時に呼び出し）
     */
    fun restorePurchases() {
        Log.d(TAG, "========== restorePurchases() 開始 ==========")
        Log.d(TAG, "restorePurchases(): billingClient exists = ${billingClient != null}")
        Log.d(TAG, "restorePurchases(): billingClient isReady = ${billingClient?.isReady}")

        if (billingClient == null) {
            Log.e(TAG, "restorePurchases(): billingClient is NULL, aborting")
            return
        }

        if (billingClient?.isReady != true) {
            Log.e(TAG, "restorePurchases(): billingClient is NOT READY, aborting")
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        Log.d(TAG, "restorePurchases(): Calling queryPurchasesAsync for INAPP products...")

        billingClient?.queryPurchasesAsync(params) { result, purchasesList ->
            Log.d(TAG, "---------- queryPurchasesAsync CALLBACK ----------")
            Log.d(TAG, "queryPurchasesAsync: responseCode=${result.responseCode}")
            Log.d(TAG, "queryPurchasesAsync: debugMessage=${result.debugMessage}")
            Log.d(TAG, "queryPurchasesAsync: purchasesList.size=${purchasesList.size}")

            // 全購入情報を詳細にログ出力
            if (purchasesList.isEmpty()) {
                Log.d(TAG, "queryPurchasesAsync: No purchases found (list is empty)")
            } else {
                purchasesList.forEachIndexed { index, purchase ->
                    Log.d(TAG, "Purchase[$index]: products=${purchase.products}")
                    Log.d(TAG, "Purchase[$index]: purchaseState=${purchase.purchaseState} (0=UNSPECIFIED, 1=PURCHASED, 2=PENDING)")
                    Log.d(TAG, "Purchase[$index]: isAcknowledged=${purchase.isAcknowledged}")
                    Log.d(TAG, "Purchase[$index]: orderId=${purchase.orderId}")
                    Log.d(TAG, "Purchase[$index]: purchaseTime=${purchase.purchaseTime}")
                }
            }

            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "queryPurchasesAsync: Response OK, searching for premium product...")
                Log.d(TAG, "queryPurchasesAsync: Looking for productId=${BillingConstants.PRODUCT_ID_PREMIUM_UNLOCK}")

                val premiumPurchase = purchasesList.find { purchase ->
                    val hasProduct = purchase.products.contains(BillingConstants.PRODUCT_ID_PREMIUM_UNLOCK)
                    val isPurchased = purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    Log.d(TAG, "queryPurchasesAsync: Checking purchase - hasProduct=$hasProduct, isPurchased=$isPurchased")
                    hasProduct && isPurchased
                }

                if (premiumPurchase != null) {
                    Log.d(TAG, "########## PREMIUM PURCHASE FOUND ##########")
                    Log.d(TAG, "Premium purchase: products=${premiumPurchase.products}")
                    Log.d(TAG, "Premium purchase: isAcknowledged=${premiumPurchase.isAcknowledged}")
                    _isPurchased.value = true
                    Log.d(TAG, "Calling onPurchaseComplete callback...")
                    onPurchaseComplete()

                    // 未確認の購入があれば確認
                    if (!premiumPurchase.isAcknowledged) {
                        Log.d(TAG, "Purchase not acknowledged, calling acknowledgePurchase...")
                        acknowledgePurchase(premiumPurchase)
                    } else {
                        Log.d(TAG, "Purchase already acknowledged")
                    }
                } else {
                    Log.d(TAG, "########## NO PREMIUM PURCHASE FOUND ##########")
                    Log.d(TAG, "Clearing local premium state...")
                    _isPurchased.value = false
                    Log.d(TAG, "Calling onPurchaseCleared callback...")
                    onPurchaseCleared()
                }
            } else {
                Log.e(TAG, "queryPurchasesAsync: FAILED - code=${result.responseCode}, msg=${result.debugMessage}")
            }

            Log.d(TAG, "========== restorePurchases() 完了 ==========")
        }
    }

    /**
     * 購入フローを起動
     */
    fun launchPurchaseFlow(activity: Activity) {
        Log.d(TAG, "========== launchPurchaseFlow() 開始 ==========")

        val details = cachedProductDetails
        if (details == null) {
            Log.e(TAG, "launchPurchaseFlow(): cachedProductDetails is NULL")
            _purchaseState.value = PurchaseState.Error("商品情報を読み込めませんでした")
            return
        }
        Log.d(TAG, "launchPurchaseFlow(): Using product=${details.productId}")

        val client = billingClient
        if (client == null) {
            Log.e(TAG, "launchPurchaseFlow(): billingClient is NULL")
            _purchaseState.value = PurchaseState.Error("課金サービスに接続できません")
            return
        }

        if (!client.isReady) {
            Log.e(TAG, "launchPurchaseFlow(): billingClient is NOT READY")
            _purchaseState.value = PurchaseState.Error("課金サービスに接続できません")
            return
        }

        Log.d(TAG, "launchPurchaseFlow(): billingClient is ready, launching flow...")

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

        Log.d(TAG, "launchPurchaseFlow(): result=${result.responseCode}, msg=${result.debugMessage}")

        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "launchPurchaseFlow(): FAILED - code=${result.responseCode}")
            _purchaseState.value = PurchaseState.Error("購入を開始できませんでした")
        } else {
            Log.d(TAG, "launchPurchaseFlow(): Purchase flow launched successfully")
        }
    }

    /**
     * 購入結果のコールバック
     */
    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        Log.d(TAG, "========== onPurchasesUpdated() ==========")
        Log.d(TAG, "onPurchasesUpdated: responseCode=${result.responseCode}")
        Log.d(TAG, "onPurchasesUpdated: debugMessage=${result.debugMessage}")
        Log.d(TAG, "onPurchasesUpdated: purchases count=${purchases?.size ?: 0}")

        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.d(TAG, "onPurchasesUpdated: OK - processing purchases...")
                purchases?.forEachIndexed { index, purchase ->
                    Log.d(TAG, "onPurchasesUpdated: Processing purchase[$index] - products=${purchase.products}")
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "onPurchasesUpdated: USER_CANCELED")
                _purchaseState.value = PurchaseState.Idle
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "onPurchasesUpdated: ITEM_ALREADY_OWNED - calling restorePurchases()")
                restorePurchases()
            }
            else -> {
                Log.e(TAG, "onPurchasesUpdated: FAILED - code=${result.responseCode}, msg=${result.debugMessage}")
                _purchaseState.value = PurchaseState.Error("購入に失敗しました")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        Log.d(TAG, "---------- handlePurchase() ----------")
        Log.d(TAG, "handlePurchase: products=${purchase.products}")
        Log.d(TAG, "handlePurchase: purchaseState=${purchase.purchaseState}")
        Log.d(TAG, "handlePurchase: isAcknowledged=${purchase.isAcknowledged}")

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            Log.d(TAG, "handlePurchase: State is PURCHASED")
            if (!purchase.isAcknowledged) {
                Log.d(TAG, "handlePurchase: Not acknowledged - granting entitlement first")
                // 重要: 先に特典付与、その後acknowledge
                _isPurchased.value = true
                Log.d(TAG, "handlePurchase: Calling onPurchaseComplete callback...")
                onPurchaseComplete()
                Log.d(TAG, "handlePurchase: Calling acknowledgePurchase...")
                acknowledgePurchase(purchase)
            } else {
                Log.d(TAG, "handlePurchase: Already acknowledged - calling completePurchase()")
                completePurchase()
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "handlePurchase: State is PENDING")
            _purchaseState.value = PurchaseState.Pending
        } else {
            Log.d(TAG, "handlePurchase: Unknown state=${purchase.purchaseState}")
        }
    }

    /**
     * 購入確認（3日以内に必須）
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        Log.d(TAG, "---------- acknowledgePurchase() ----------")
        Log.d(TAG, "acknowledgePurchase: purchaseToken=${purchase.purchaseToken.take(20)}...")

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(params) { result ->
            Log.d(TAG, "acknowledgePurchase callback: responseCode=${result.responseCode}")
            Log.d(TAG, "acknowledgePurchase callback: debugMessage=${result.debugMessage}")

            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "acknowledgePurchase: SUCCESS")
                _purchaseState.value = PurchaseState.Purchased
            } else {
                Log.e(TAG, "acknowledgePurchase: FAILED - code=${result.responseCode}")
                // 特典は既に付与済みなので、状態は Purchased にする
                // 次回起動時に再度 acknowledge を試みる
                Log.d(TAG, "acknowledgePurchase: Entitlement already granted, setting state to Purchased anyway")
                _purchaseState.value = PurchaseState.Purchased
            }
        }
    }

    private fun completePurchase() {
        Log.d(TAG, "---------- completePurchase() ----------")
        Log.d(TAG, "completePurchase: Setting isPurchased=true")
        _isPurchased.value = true
        _purchaseState.value = PurchaseState.Purchased
        Log.d(TAG, "completePurchase: Calling onPurchaseComplete callback...")
        onPurchaseComplete()
        Log.d(TAG, "completePurchase: Done")
    }

    /**
     * 接続を終了
     */
    fun endConnection() {
        Log.d(TAG, "========== endConnection() ==========")
        // CoroutineScopeをキャンセルして再接続ループを停止
        Log.d(TAG, "endConnection: Cancelling scope...")
        scope.cancel()
        Log.d(TAG, "endConnection: Ending billingClient connection...")
        billingClient?.endConnection()
        billingClient = null
        cachedProductDetails = null
        _connectionState.value = BillingConnectionState.NotConnected
        Log.d(TAG, "endConnection: Done")
    }
}
