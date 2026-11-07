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
 * Assinaturas pela loja (Google Play Billing). A pessoa paga a loja (que fica com a taxa) — o jeito
 * mais simples e confiável, zero risco de recusa, e a loja faz o preço local de cada país sozinha.
 *
 * Dois planos pagos; o SOS básico é sempre GRÁTIS (só os extras são pagos).
 * Sem os produtos configurados na Play Console, a tela do plano aparece, mas a compra fica indisponível.
 */
object Billing {
    // Os preços ($5 e $10) são definidos na Play Console / App Store Connect, não aqui.
    const val PROTECAO = "guardioes_protecao_mensal" // ~$5: SMS garantido + mais guardiões + histórico
    const val COMPLETO = "guardioes_completo_mensal" // ~$10: tudo + GPS ao vivo + vídeo + microfone

    data class Estado(
        /** productId -> preço já formatado na moeda do país (ex.: "R$ 29,90"). Só os achados na loja. */
        val precos: Map<String, String> = emptyMap(),
        /** productId da assinatura ativa, ou null (grátis). */
        val ativo: String? = null,
        val erro: String? = null,
    ) {
        fun disponivel(id: String) = precos.containsKey(id)
    }

    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    private var client: BillingClient? = null
    private val produtos = mutableMapOf<String, ProductDetails>()

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { confirmar(it) }
        }
    }

    fun iniciar(context: Context) {
        if (client != null) return
        val c = BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesListener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        client = c
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    carregarProdutos()
                    verificarAssinatura()
                } else {
                    _estado.value = _estado.value.copy(erro = "Loja indisponível (${result.responseCode})")
                }
            }

            override fun onBillingServiceDisconnected() {
                client = null
            }
        })
    }

    private fun carregarProdutos() {
        val c = client ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(PROTECAO, COMPLETO).map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()
        c.queryProductDetailsAsync(params) { result, lista ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val precos = mutableMapOf<String, String>()
                lista.forEach { p ->
                    produtos[p.productId] = p
                    val preco = p.subscriptionOfferDetails
                        ?.firstOrNull()
                        ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                        ?.formattedPrice
                        .orEmpty()
                    if (preco.isNotBlank()) precos[p.productId] = preco
                }
                _estado.value = _estado.value.copy(precos = precos, erro = if (precos.isEmpty()) "Planos ainda não configurados na loja" else null)
            } else {
                _estado.value = _estado.value.copy(erro = "Planos ainda não configurados na loja")
            }
        }
    }

    private fun verificarAssinatura() {
        val c = client ?: return
        c.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { _, compras ->
            val ativa = compras.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            _estado.value = _estado.value.copy(ativo = ativa?.products?.firstOrNull())
            compras.forEach { confirmar(it) }
        }
    }

    /** Abre o diálogo de compra da Play para o plano escolhido. */
    fun assinar(activity: Activity, productId: String) {
        val c = client ?: return
        val p = produtos[productId] ?: return
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
        _estado.value = _estado.value.copy(ativo = compra.products.firstOrNull())
        if (!compra.isAcknowledged) {
            client?.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(compra.purchaseToken).build()
            ) { }
        }
    }
}
