package br.com.alertasilencioso.core

import br.com.alertasilencioso.data.TriggerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PressPatternDetectorTest {

    private fun hold(seconds: Long = 10) =
        PressPatternDetector(PressPatternDetector.Config(mode = TriggerMode.HOLD, holdMillis = seconds * 1_000))

    private fun taps(count: Int = 5) =
        PressPatternDetector(PressPatternDetector.Config(mode = TriggerMode.TAPS, tapCount = count, tapWindowMillis = 4_000))

    @Test
    fun `segurar dispara so depois do tempo configurado`() {
        val d = hold()
        assertFalse(d.press(0))
        assertFalse(d.poll(9_999))
        assertTrue(d.poll(10_000))
        assertTrue(d.firedThisGesture)
    }

    @Test
    fun `soltar antes do tempo nao dispara e informa quanto tempo ficou pressionado`() {
        val d = hold()
        d.press(1_000)
        assertEquals(4_000L, d.release(5_000))
        assertFalse(d.poll(20_000))
        assertFalse(d.firedThisGesture)
    }

    @Test
    fun `continuar segurando depois de disparar nao dispara de novo`() {
        val d = hold()
        d.press(0)
        assertTrue(d.poll(10_000))
        assertFalse(d.poll(15_000))
        assertFalse(d.poll(30_000))
    }

    @Test
    fun `novo aperto depois de soltar comeca do zero`() {
        val d = hold()
        d.press(0)
        assertTrue(d.poll(10_000))
        d.release(11_000)
        d.press(12_000)
        assertFalse(d.firedThisGesture)
        assertFalse(d.poll(21_999))
        assertTrue(d.poll(22_000))
    }

    @Test
    fun `soltar sem ter apertado devolve null`() {
        assertNull(hold().release(100))
    }

    @Test
    fun `apertar o numero de vezes dentro da janela dispara no ultimo toque`() {
        val d = taps(5)
        for (i in 0 until 4) {
            assertFalse(d.press(i * 500L))
            d.release(i * 500L + 100)
        }
        assertTrue(d.press(2_000))
    }

    @Test
    fun `toques espalhados demais nao disparam`() {
        val d = taps(5)
        val results = (0 until 5).map { i ->
            d.press(i * 1_500L).also { d.release(i * 1_500L + 100) }
        }
        assertTrue(results.none { it })
    }

    @Test
    fun `no modo toques o tempo segurado nao importa`() {
        val d = taps(5)
        d.press(0)
        assertFalse(d.poll(60_000))
    }

    @Test
    fun `sinais continuos sem soltura contam como segurar`() {
        val d = hold()
        var fired = false
        var t = 0L
        while (t <= 10_000) {
            if (d.pulse(t)) fired = true
            t += 100
        }
        assertTrue(fired)
    }

    @Test
    fun `um sinal so nunca dispara o modo segurar`() {
        val d = hold()
        assertFalse(d.pulse(0))
        assertFalse(d.pulse(20_000))
    }

    @Test
    fun `pausa longa entre sinais recomeca a contagem`() {
        val d = hold()
        pulses(d, from = 0, to = 8_000).forEach { assertFalse(it) }
        // Pausa de 2 s: conta como soltar e apertar de novo, então precisa de mais 10 s a partir de 10 s.
        pulses(d, from = 10_000, to = 19_900).forEach { assertFalse(it) }
        assertTrue(d.pulse(20_000))
    }

    private fun pulses(d: PressPatternDetector, from: Long, to: Long): List<Boolean> =
        (from..to step 100).map { d.pulse(it) }

    @Test
    fun `no modo toques cada sinal conta como um toque`() {
        val d = taps(3)
        assertFalse(d.pulse(0))
        assertFalse(d.pulse(1_500))
        assertTrue(d.pulse(3_000))
    }
}
