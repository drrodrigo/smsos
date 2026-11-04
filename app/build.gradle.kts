import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Dados da chave de produção. Ficam fora do projeto (keystore/release.properties, no .gitignore).
// Sem esse arquivo, o build de release sai sem assinatura — o de teste (debug) continua funcionando.
val releaseProps = rootProject.file("keystore/release.properties").takeIf { it.exists() }?.let {
    Properties().apply { it.inputStream().use(::load) }
}

// Endereço e chave do gateway de SMS (função smsos-alerta). Ficam fora do git (gateway.properties).
// Sem o arquivo, o app compila com gateway vazio e cai no envio pelo chip (fallback).
val gatewayProps = rootProject.file("gateway.properties").takeIf { it.exists() }?.let {
    Properties().apply { it.inputStream().use(::load) }
}
val gatewayUrl = gatewayProps?.getProperty("gatewayUrl").orEmpty()
val gatewayKey = gatewayProps?.getProperty("gatewayKey").orEmpty()

android {
    namespace = "br.com.alertasilencioso"
    compileSdk = 37

    defaultConfig {
        applicationId = "br.com.smsos"
        minSdk = 26
        targetSdk = 36
        versionCode = 15
        versionName = "0.9.0"

        buildConfigField("String", "GATEWAY_URL", "\"$gatewayUrl\"")
        buildConfigField("String", "GATEWAY_KEY", "\"$gatewayKey\"")
    }

    signingConfigs {
        // Chave só para as versões de teste: fica no projeto para que cada APK novo
        // instale por cima do anterior sem apagar os contatos cadastrados.
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
        release {
            isMinifyEnabled = false
            if (releaseProps != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperty(
                "robolectric.dependency.repo.url",
                "https://maven-central.storage-download.googleapis.com/maven2",
            )
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    testImplementation("junit:junit:4.13.2")
    // Abre as telas num Android simulado, sem precisar de celular.
    testImplementation(composeBom)
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
