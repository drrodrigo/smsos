package br.com.alertasilencioso.relogio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * O CONTRATO mora em dois módulos (o app do celular e o do relógio). Este teste LÊ os arquivos-fonte dos dois lados
 * e reprova se o relógio e o celular deixarem de falar a mesma língua: caminhos, chaves, pacote, assinatura e
 * versão. (Como o relogioApp.test.ts do App Funcional.)
 */
class ContratoDoRelogioTest {
    private val raiz: File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").exists() }

    private fun ler(caminho: String) = File(raiz, caminho).also { assertTrue("falta $caminho", it.exists()) }.readText()

    /** Todas as `const val NOME = "valor"` de um arquivo Kotlin. */
    private fun constantes(fonte: String): Map<String, String> =
        Regex("""const\s+val\s+(\w+)\s*=\s*"([^"]*)"""").findAll(fonte).associate { it.groupValues[1] to it.groupValues[2] }

    private val doRelogio = constantes(ler("relogio/src/main/java/br/com/alertasilencioso/relogio/Contrato.kt"))
    private val doCelular = constantes(ler("app/src/main/java/br/com/alertasilencioso/relogio/ContratoDoRelogio.kt"))

    @Test
    fun `os dois lados tem os MESMOS caminhos e chaves, com os mesmos valores`() {
        assertEquals("o relógio e o celular precisam ter as mesmas constantes", doRelogio.keys, doCelular.keys)
        for ((nome, valor) in doRelogio) assertEquals("$nome diferente nos dois lados", valor, doCelular[nome])
    }

    @Test
    fun `os caminhos e as chaves sao os combinados`() {
        assertEquals("/guardioes/sos", doCelular["CAMINHO_SOS"])
        assertEquals("/guardioes/sos/resultado", doCelular["CAMINHO_RESULTADO"])
        assertEquals(
            listOf("perfil", "id", "ok", "teste", "contatos", "erro"),
            listOf("CHAVE_PERFIL", "CHAVE_ID", "CHAVE_OK", "CHAVE_TESTE", "CHAVE_CONTATOS", "CHAVE_ERRO").map { doCelular[it] },
        )
        assertEquals("seguranca", doCelular["PERFIL_SEGURANCA"])
        assertEquals("saude", doCelular["PERFIL_SAUDE"])
        // As constantes do Kotlin batem com as do teste (se alguém trocar o arquivo e não a constante, ou o contrário).
        assertEquals(ContratoDoRelogio.CAMINHO_SOS, doCelular["CAMINHO_SOS"])
        assertEquals(ContratoDoRelogio.CAMINHO_RESULTADO, doCelular["CAMINHO_RESULTADO"])
    }

    @Test
    fun `o manifesto do celular escuta exatamente o caminho do pedido`() {
        val manifesto = ler("app/src/main/AndroidManifest.xml")
        assertTrue(manifesto.contains(".relogio.OuvinteDoRelogio"))
        assertTrue(manifesto.contains("""android:path="${ContratoDoRelogio.CAMINHO_SOS}""""))
        assertTrue(manifesto.contains("com.google.android.gms.wearable.MESSAGE_RECEIVED"))
    }

    /** O valor de `campo = ...` numa linha de código (não em comentário) do build. */
    private fun valor(build: String, campo: String): String? =
        Regex("""(?m)^\s*$campo\s*=\s*"?([^"\n]+)"?""").find(build)?.groupValues?.get(1)?.trim()

    @Test
    fun `mesmo pacote, mesmo sufixo de teste e MESMA chave - senao a ponte do Wear OS nao liga`() {
        val celular = ler("app/build.gradle.kts")
        val relogio = ler("relogio/build.gradle.kts")
        assertEquals(valor(celular, "applicationId"), valor(relogio, "applicationId"))
        assertEquals("br.com.guardioessos", valor(relogio, "applicationId"))
        assertEquals(valor(celular, "applicationIdSuffix"), valor(relogio, "applicationIdSuffix"))
        for (campo in listOf("storeFile", "storePassword", "keyAlias", "keyPassword")) {
            val c = Regex("""getByName\("debug"\)\s*\{[^}]*\b$campo\s*=\s*([^\n]+)""").find(celular)?.groupValues?.get(1)
            val r = Regex("""getByName\("debug"\)\s*\{[^}]*\b$campo\s*=\s*([^\n]+)""").find(relogio)?.groupValues?.get(1)
            assertNotNull("$campo da chave de teste", c)
            assertEquals("$campo da chave de teste igual nos dois", c, r)
        }
        // A chave da loja também é a mesma (keystore/release.properties, fora do git).
        assertTrue(relogio.contains("""rootProject.file("keystore/release.properties")"""))
    }

    @Test
    fun `versao do relogio = a do celular + 100000, com o sufixo -relogio`() {
        val celular = ler("app/build.gradle.kts")
        val relogio = ler("relogio/build.gradle.kts")
        val codigo = valor(celular, "versionCode")!!.toInt()
        val nome = valor(celular, "versionName")!!
        assertEquals("subiu a versão do celular? suba a do relógio junto", codigo + 100_000, valor(relogio, "versionCode")!!.toInt())
        assertEquals("$nome-relogio", valor(relogio, "versionName"))
    }

    @Test
    fun `nenhum dos dois manifestos pede SEND_SMS - o alerta sai pelo nosso servidor`() {
        assertFalse(ler("app/src/main/AndroidManifest.xml").contains("android.permission.SEND_SMS"))
        assertFalse(ler("app/src/debug/AndroidManifest.xml").contains("android.permission.SEND_SMS"))
        assertFalse(ler("relogio/src/main/AndroidManifest.xml").contains("android.permission.SEND_SMS"))
    }

    @Test
    fun `a bateria do relogio - a tela so fica acesa pela regra da maquina (contagem e envio)`() {
        val tela = ler("relogio/src/main/java/br/com/alertasilencioso/relogio/TelaDoSos.kt")
        assertTrue(tela.contains("MaquinaDoSos.telaAcesa(e)"))
        assertTrue("a tela tem de APAGAR fora da contagem", tela.contains("clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)"))
        val maquina = ler("relogio/src/main/java/br/com/alertasilencioso/relogio/MaquinaDoSos.kt")
        assertTrue(maquina.contains("fun telaAcesa(e: Estado): Boolean = e is Estado.Contando || e is Estado.Enviando"))
    }
}
