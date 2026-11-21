import java.util.Properties

// O APP DO RELÓGIO (Galaxy Watch 4 em diante / Wear OS 3+). Um botão de pânico no pulso: abriu, conta e
// dispara SOZINHO; o pedido vai pela ponte do Wear OS para o app do celular, que manda o alerta pelo NOSSO
// servidor (o relógio não envia SMS nem fala com o servidor). Ver docs/relogio-sos.md.
//
// 🔴 A ponte do Wear OS só liga app com o MESMO pacote e a MESMA chave de assinatura nos dois aparelhos:
// o relógio usa o applicationId do celular (br.com.guardioessos; no teste, o MESMO sufixo .teste) e as
// MESMAS chaves (keystore/teste.keystore no teste; keystore/release.properties na loja).
//
// 🔴 Versão: versionCode = o do celular + 100000 e versionName = o do celular + "-relogio" (os dois sobem
// no MESMO app da Play, cada pacote com um número só dele; o sufixo evita trocar as faixas na Play).
// Subiu a versão do celular (app/build.gradle.kts)? Suba esta junto: o ContratoDoRelogioTest trava.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// A mesma chave de produção do celular (fora do git). Sem ela, o release sai sem assinatura.
val releaseProps = rootProject.file("keystore/release.properties").takeIf { it.exists() }?.let {
    Properties().apply { it.inputStream().use(::load) }
}

android {
    namespace = "br.com.alertasilencioso.relogio"
    compileSdk = 37

    defaultConfig {
        applicationId = "br.com.guardioessos"
        // Wear OS 3 em diante (Galaxy Watch 4, 5, 6, 7, Ultra e os outros relógios Wear OS).
        minSdk = 30
        targetSdk = 36
        versionCode = 100056
        versionName = "0.34.1-relogio"

        manifestPlaceholders["appLabel"] = "GuardiõesSOS"
    }

    signingConfigs {
        // A MESMA chave de teste do celular: sem ela a ponte não liga o relógio ao "GuardiõesSOS teste".
        getByName("debug") {
            storeFile = rootProject.file("keystore/teste.keystore")
            storePassword = "android"
            keyAlias = "teste"
            keyPassword = "android"
        }
        if (releaseProps != null) {
            create("release") {
                storeFile = rootProject.file(releaseProps.getProperty("storeFile"))
                storePassword = releaseProps.getProperty("storePassword")
                keyAlias = releaseProps.getProperty("keyAlias")
                keyPassword = releaseProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // O MESMO sufixo do celular de teste (br.com.guardioessos.teste): é com ele que a ponte conversa.
            applicationIdSuffix = ".teste"
            manifestPlaceholders["appLabel"] = "GuardiõesSOS teste"
        }
        release {
            isMinifyEnabled = false
            if (releaseProps != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// O APK de TESTE vai para o relógio pelo Wi-Fi (ADB): com o código comprimido ele cai de ~34 MB para ~14 MB.
androidComponents {
    onVariants(selector().withBuildType("debug")) { variante ->
        variante.packaging.dex.useLegacyPackaging.set(true)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    // Compose para relógio: botões e lista que acompanham a tela redonda.
    implementation("androidx.wear.compose:compose-material:1.7.2")
    implementation("androidx.wear.compose:compose-foundation:1.7.2")
    // A ponte com o celular (MessageClient / NodeClient).
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    // A complicação "SOS" do mostrador.
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    testImplementation("junit:junit:4.13.2")
    // O org.json de verdade nos testes (o do android.jar dos testes é só casca).
    testImplementation("org.json:json:20260814")
}
