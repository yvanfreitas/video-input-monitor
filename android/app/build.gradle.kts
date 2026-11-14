plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// O número do build do GitHub Actions vira o versionCode, para que cada APK
// novo possa ser instalado por cima do anterior.
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.videoinputmonitor.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.videoinputmonitor.android"
        minSdk = 26
        targetSdk = 34
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    // App distribuído fora da Play Store. A chave padrão fica no repositório
    // para que as atualizações instalem por cima; pode ser trocada por uma
    // chave própria via variáveis de ambiente (secrets do GitHub).
    signingConfigs {
        create("sideload") {
            storeFile = file(System.getenv("ANDROID_KEYSTORE_PATH") ?: "sideload.jks")
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: "videoinputmonitor"
            keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: "sideload"
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: "videoinputmonitor"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("sideload")
        }
        debug {
            signingConfig = signingConfigs.getByName("sideload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-common:2.8.7")

    // Captura de placas/webcams UVC via USB Host (libuvc), sem depender do fabricante.
    implementation("com.herohan:UVCAndroid:1.0.13")

    // Android Auto (Car App Library).
    implementation("androidx.car.app:app:1.4.0")
    implementation("androidx.car.app:app-projected:1.4.0")
}
