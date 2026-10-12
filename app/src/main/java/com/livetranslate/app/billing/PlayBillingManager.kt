package com.livetranslate.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface BillingState {
    data object Loading : BillingState
    data class Ready(val price: String) : BillingState
    data object Pro : BillingState
    data class Unavailable(val message: String) : BillingState
    data class Error(val message: String) : BillingState
}

class PlayBillingManager(context: Context) {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow<BillingState>(BillingState.Loading)
    val state: StateFlow<BillingState> = _state

    private var productDetails: ProductDetails? = null
    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                processPurchases(purchases)
            } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
                _state.value = BillingState.Error(result.debugMessage.ifBlank { "Purchase could not be completed." })
            }
        }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .enableAutoServiceReconnection()
        .build()

    init {
        connect()
    }

    private fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    _state.value = BillingState.Unavailable("Google Play purchases are unavailable on this device.")
                    return
                }
                queryProduct()
                restorePurchases()
            }

            override fun onBillingServiceDisconnected() {
                _state.value = BillingState.Unavailable("Google Play is temporarily unavailable.")
            }
        })
    }

    private fun queryProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        billingClient.queryProductDetailsAsync(params) { result, detailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.value = BillingState.Unavailable("Pro is not available in this store yet.")
                return@queryProductDetailsAsync
            }
            productDetails = detailsResult.productDetailsList.firstOrNull()
            val price = productDetails?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
            _state.value = if (price == null) {
                BillingState.Unavailable("Pro is not configured in Google Play yet.")
            } else {
                BillingState.Ready(price)
            }
        }
    }

    private fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        purchases.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED && it.products.contains(PRODUCT_ID)
        }.forEach { purchase ->
            _state.value = BillingState.Pro
            if (!purchase.isAcknowledged) {
                billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build(),
                ) { result ->
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        _state.value = BillingState.Error("The purchase needs to be restored from Google Play.")
                    }
                }
            }
        }
    }

    fun launchPurchase(activity: Activity) {
        val details = productDetails
        if (details == null) {
            _state.value = BillingState.Unavailable("Pro is not ready yet. Try again in a moment.")
            return
        }
        val offerToken = details.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken.orEmpty()
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()
        val result = billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build(),
        )
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.value = BillingState.Error(result.debugMessage.ifBlank { "Purchase could not be started." })
        }
    }

    fun close() {
        billingClient.endConnection()
    }

    companion object {
        const val PRODUCT_ID = "live_translator_pro"
    }
}
