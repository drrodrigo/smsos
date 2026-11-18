package br.com.alertasilencioso.trigger

import android.content.Context
import android.os.Looper
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.data.TriggerMode
import br.com.alertasilencioso.testutil.RobolectricJarLeak
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

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

    // Ver RobolectricJarLeak: sem isso, o próximo Android simulado (outra versão de SDK) quebra ao abrir.
    @After
    fun fechaJarVazadoDoRobolectric() = RobolectricJarLeak.fechar()

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

    // --- Modo "os dois" (segurar OU apertar): pedido do Rodrigo ---

    private fun modoOsDois() = Repository.updateSettings {
        it.copy(triggerMode = TriggerMode.BOTH, holdSeconds = 3, tapCount = 3, dualSos = true, testMode = true)
    }

    private fun esperar(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    @Test
    fun `os dois - tres toques disparam`() {
        modoOsDois()
        var fires = 0
        val controller = TriggerController(context) { _, _ -> fires++ }
        repeat(3) {
            controller.onAccessibilityKey(down(KeyEvent.KEYCODE_VOLUME_DOWN))
            controller.onAccessibilityKey(up(KeyEvent.KEYCODE_VOLUME_DOWN))
        }
        assertEquals(1, fires)
    }

    @Test
    fun `os dois - tres toques SEM ACTION_UP disparam uma vez so`() {
        modoOsDois()
        var fires = 0
        val controller = TriggerController(context) { _, _ -> fires++ }
        repeat(3) { controller.onAccessibilityKey(down(KeyEvent.KEYCODE_VOLUME_DOWN)) }
        esperar(10_000)
        assertEquals(1, fires)
    }

    @Test
    fun `os dois - segurar pelo tempo dispara`() {
        modoOsDois()
        var fires = 0
        val controller = TriggerController(context) { _, _ -> fires++ }
        controller.onAccessibilityKey(down(KeyEvent.KEYCODE_VOLUME_DOWN))
        esperar(2_900)
        assertEquals(0, fires)
        esperar(500)
        assertEquals(1, fires)
        controller.onAccessibilityKey(up(KeyEvent.KEYCODE_VOLUME_DOWN))
        esperar(10_000)
        assertEquals(1, fires)
    }

    @Test
    fun `os dois - um toque rapido nao dispara`() {
        modoOsDois()
        var fires = 0
        val controller = TriggerController(context) { _, _ -> fires++ }
        controller.onAccessibilityKey(down(KeyEvent.KEYCODE_VOLUME_DOWN))
        controller.onAccessibilityKey(up(KeyEvent.KEYCODE_VOLUME_DOWN))
        esperar(10_000)
        assertEquals(0, fires)
    }
}
