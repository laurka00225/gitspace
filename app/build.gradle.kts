plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ci.etudes.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "ci.etudes.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
    }

    signingConfigs {
        // Clé fixe, conservée dans le dépôt : chaque nouvel APK a la même signature,
        // donc il s'installe par-dessus l'ancien sans perdre les données de l'élève.
        // Pour une publication sur le Play Store, remplacer par une clé gardée secrète.
        create("release") {
            storeFile = file("mesetudes.jks")
            storePassword = "mesetudes"
            keyAlias = "mesetudes"
            keyPassword = "mesetudes"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
}
