pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.7.3"
        id("org.jetbrains.kotlin.android") version "2.0.21"
        id("org.jetbrains.kotlin.jvm") version "2.0.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    }
}

val coreOnly = providers.gradleProperty("coreOnly").isPresent

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (!coreOnly) google()
        mavenCentral()
    }
}

rootProject.name = "MonEmploiDuTempsEtudes"

// Logique de planification (Kotlin pur, testable sans SDK Android)
include(":core")

// Application Android. `-PcoreOnly` permet de tester :core sans SDK Android.
if (!coreOnly) {
    include(":app")
}
