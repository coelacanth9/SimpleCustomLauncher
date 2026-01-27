package com.example.simplecustomlauncher.billing

/**
 * BillingClient の接続状態
 */
sealed class BillingConnectionState {
    object NotConnected : BillingConnectionState()
    object Connecting : BillingConnectionState()
    object Connected : BillingConnectionState()
    data class Error(val message: String, val responseCode: Int) : BillingConnectionState()
}

/**
 * 商品詳細情報
 */
data class ProductInfo(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String
)

/**
 * 購入処理の状態
 */
sealed class PurchaseState {
    object Idle : PurchaseState()
    object Pending : PurchaseState()
    object Purchased : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}
