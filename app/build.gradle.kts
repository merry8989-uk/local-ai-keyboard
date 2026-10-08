plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.merry8989.localaikeyboard"
    compileSdk = 34

    // ---- Signing -------------------------------------------------------------
    // Priority:
    //   1. A keystore supplied via a CI secret / gradle property — your own key.
    //   2. The committed debug keystore (app/debug.p12). Every build shares this
    //      one key, so new APKs install OVER the old app with no uninstall.
    //      It is a DEBUG key: public by design, never use it for a release build.
    val envKeystorePath = System.getenv("KEYSTORE_FILE")
        ?: project.findProperty("KEYSTORE_FILE")?.toString()
    val envKeystoreFile = envKeystorePath?.takeIf { it.isNotBlank() }?.let { file(it) }
    val hasEnvKeystore = envKeystoreFile?.exists() == true

    val sharedDebugFile = file("debug.p12")
    val hasSharedDebug = sharedDebugFile.exists()

    signingConfigs {
        if (hasEnvKeystore) {
            create("stable") {
                storeFile = envKeystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                    ?: project.findProperty("KEYSTORE_PASSWORD")?.toString()
                keyAlias = System.getenv("KEY_ALIAS")
                    ?: project.findProperty("KEY_ALIAS")?.toString()
                keyPassword = System.getenv("KEY_PASSWORD")
                    ?: project.findProperty("KEY_PASSWORD")?.toString()
            }
        }
        if (hasSharedDebug) {
            create("sharedDebug") {
                storeFile = sharedDebugFile
                storeType = "PKCS12"
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    defaultConfig {
        applicationId = "com.merry8989.localaikeyboard"
        minSdk = 29
        targetSdk = 34
        versionCode = 9
        versionName = "0.9.0"
        // Pure Kotlin/Java: no native libraries, so every ABI is supported.
    }

    buildTypes {
        getByName("debug") {
            signingConfig = when {
                hasEnvKeystore -> signingConfigs.getByName("stable")
                hasSharedDebug -> signingConfigs.getByName("sharedDebug")
                else -> signingConfigs.getByName("debug")
            }
        }
        getByName("release") {
            if (hasEnvKeystore) signingConfig = signingConfigs.getByName("stable")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
