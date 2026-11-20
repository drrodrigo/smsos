package br.com.alertasilencioso.relogio

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

/**
 * A COMPLICAÇÃO "SOS" do mostrador: o escudo (e "SOS", no formato de texto curto). Um toque abre a tela do SOS
 * JÁ CONTANDO, como o botão de cima. Não muda sozinha (UPDATE_PERIOD 0 no manifesto): não gasta bateria.
 */
class ComplicacaoSos : SuspendingComplicationDataSourceService() {

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? =
        dados(request.complicationType)

    override fun getPreviewData(type: ComplicationType): ComplicationData? = dados(type)

    private fun dados(tipo: ComplicationType): ComplicationData? {
        val toque = PendingIntent.getActivity(
            this,
            0,
            Intent(this, TelaDoSos::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val descricao = PlainComplicationText.Builder(getString(R.string.complicacao_descricao)).build()
        val escudo = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_escudo)).build()
        return when (tipo) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                PlainComplicationText.Builder(getString(R.string.complicacao_nome)).build(),
                descricao,
            ).setMonochromaticImage(escudo).setTapAction(toque).build()

            ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(escudo, descricao)
                .setTapAction(toque)
                .build()

            ComplicationType.SMALL_IMAGE -> SmallImageComplicationData.Builder(
                SmallImage.Builder(Icon.createWithResource(this, R.drawable.logo_escudo), SmallImageType.ICON).build(),
                descricao,
            ).setTapAction(toque).build()

            else -> null
        }
    }
}
