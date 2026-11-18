package br.com.alertasilencioso.testutil

import java.nio.file.FileSystems

/**
 * Contorna um vazamento do Robolectric 4.17 (só nos testes, não muda nada no app).
 *
 * Ao carregar o runtime nativo, o Robolectric abre o jar `nativeruntime-dist-compat` como sistema de
 * arquivos para copiar o `hyphen-data` e NÃO fecha. Quando um segundo Android simulado (outra versão de
 * SDK, ex.: o AppSmokeTest roda no 26 e no 35) carrega o runtime, ele tenta abrir o mesmo jar para copiar
 * as fontes e quebra com `FileSystemAlreadyExistsException`. Os arquivos já foram copiados para uma pasta
 * temporária, então fechar o jar depois de cada teste é seguro.
 */
object RobolectricJarLeak {
    fun fechar() {
        val url = RobolectricJarLeak::class.java.classLoader?.getResource("hyphen-data/") ?: return
        if (url.protocol != "jar") return
        runCatching { FileSystems.getFileSystem(url.toURI()).close() }
    }
}
