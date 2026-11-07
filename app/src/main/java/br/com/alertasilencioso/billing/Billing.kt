package br.com.alertasilencioso.billing

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
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Assinatura "Guardião+" pela loja (Google Play Billing). A pessoa paga a loja (a Apple/Google fica
 * com a taxa), que é o jeito mais simples e confiável — e zero risco de recusa.
 *
 * Sem o produto configurado na Play Console, a tela do plano aparece, mas a compra fica indisponível.
 * 🔴 O socorro (SOS) NUNCA depende disto — o que é pago são só os EXTRAS.
 */
object Billing {
    const val PRODUTO_MENSAL = "guardioes_plus_mensal"

    data class Estado(
        val disponivel: Boolean = false, // o produto foi encontrado na loja
        val assinante: Boolean = false,  // a assinatura está ativa
        val preco: String = "",          // ex.: "R$ 19,90"
        val erro: String? = null,
    )

    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    private var client: BillingClient? = null
    private var produto: ProductDetails? = null

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { confirmar(it) }
        }
    }

    fun iniciar(context: Context) {
        if (client != null) return
        val c = BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        client = c
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    carregarProduto()
                    verificarAssinatura()
                } else {
                    _estado.value = _estado.value.copy(erro = "Loja indisponível (${result.responseCode})")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Tenta reconectar na próxima vez que a tela do plano abrir.
                client = null
            }
        })
    }

    private fun carregarProduto() {
        val c = client ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUTO_MENSAL)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()
        c.queryProductDetailsAsync(params) { result, lista ->
            val p = lista.firstOrNull()
            if (result.responseCode == BillingClient.BillingResponseCode.OK && p != null) {
                produto = p
                val preco = p.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                    ?.formattedPrice
                    .orEmpty()
                _estado.value = _estado.value.copy(disponivel = true, preco = preco, erro = null)
            } else {
                _estado.value = _estado.value.copy(disponivel = false, erro = "Plano ainda não configurado na loja")
            }
        }
    }

    private fun verificarAssinatura() {
        val c = client ?: return
        c.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { _, compras ->
            val ativo = compras.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            _estado.value = _estado.value.copy(assinante = ativo)
            compras.forEach { confirmar(it) }
        }
    }

    /** Abre o diálogo de compra da Play. Precisa da Activity atual. */
    fun assinar(activity: Activity) {
        val c = client ?: return
        val p = produto ?: return
        val offerToken = p.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(p)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        c.launchBillingFlow(activity, params)
    }

    private fun confirmar(compra: Purchase) {
        if (compra.purchaseState != Purchase.PurchaseState.PURCHASED) return
        _estado.value = _estado.value.copy(assinante = true)
        if (!compra.isAcknowledged) {
            client?.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(compra.purchaseToken).build()
            ) { }
        }
    }
}
