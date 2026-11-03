pluginManagement {
    repositories {
        google()
        // Espelho do Maven Central mantido pelo Google (o endereço principal às vezes limita downloads).
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Espelho do Maven Central mantido pelo Google (o endereço principal às vezes limita downloads).
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
    }
}

rootProject.name = "AlertaSilencioso"
include(":app")
