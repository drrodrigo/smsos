package br.com.alertasilencioso.trigger

import android.content.Context
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.data.TriggerMode
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Garante que o gesto de "apertar várias vezes" dispara mesmo em aparelhos que NÃO avisam a soltura
 * do botão pela acessibilidade (caso real do Samsung M54): lá só chegam os ACTION_DOWN.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TriggerControllerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setup() {
        Repository.init(context)
        Repository.updateSettings {
            it.copy(triggerMode = TriggerMode.TAPS, tapCount = 3, dualSos = true, testMode = true)
        }
    }

    private fun down(keyCode: Int) = KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, 0)
    private fun up(keyCode: Int) = KeyEvent(0L, 0L, KeyEvent.ACTION_UP, keyCode, 0)

    @Test
    fun `tres toques SEM ACTION_UP disparam (aparelho que nao avisa a soltura)`() {
        var fires = 0
        val controller = TriggerController(context) { _, _ -> fires++ }
        repeat(3) { controller.onAccessibilityKey(down(KeyEvent.KEYCODE_VOLUME_DOWN)) }
        assertEquals(1, fires)
    }

    @Test
    fun `tres toques COM soltura normal tambem disparam (nao quebrou o caso comum)`() {
        var fires = 0
        val controller = TriggerController(context) { _, _ -> fires++ }
        repeat(3) {
            controller.onAccessibilityKey(down(KeyEvent.KEYCODE_VOLUME_DOWN))
            controller.onAccessibilityKey(up(KeyEvent.KEYCODE_VOLUME_DOWN))
        }
        assertEquals(1, fires)
    }
}
