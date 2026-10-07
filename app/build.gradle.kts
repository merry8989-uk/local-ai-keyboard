plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.merry8989.localaikeyboard"
    compileSdk = 34

    // ---- Signing -------------------------------------------------------------
    // If a keystore is supplied (via CI secrets or local gradle.properties),
    // BOTH debug and release builds are signed with it. A single stable key
    // means each new APK installs *over* the existing app — no uninstall needed.
    // Without a keystore, everything falls back to the normal debug key.
    val keystorePath = System.getenv("KEYSTORE_FILE")
        ?: project.findProperty("KEYSTORE_FILE")?.toString()
    val keystoreFile = keystorePath?.takeIf { it.isNotBlank() }?.let { file(it) }
    val hasKeystore = keystoreFile?.exists() == true

    signingConfigs {
        if (hasKeystore) {
            create("stable") {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                    ?: project.findProperty("KEYSTORE_PASSWORD")?.toString()
                keyAlias = System.getenv("KEY_ALIAS")
                    ?: project.findProperty("KEY_ALIAS")?.toString()
                keyPassword = System.getenv("KEY_PASSWORD")
                    ?: project.findProperty("KEY_PASSWORD")?.toString()
            }
        }
    }

    defaultConfig {
        applicationId = "com.merry8989.localaikeyboard"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        // arm64 only: the on-device LLM runtime is 64-bit.
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        getByName("debug") {
            if (hasKeystore) signingConfig = signingConfigs.getByName("stable")
        }
        getByName("release") {
            if (hasKeystore) signingConfig = signingConfigs.getByName("stable")
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

    // Keep model bundles uncompressed when they are ever bundled as assets.
    androidResources {
        noCompress += listOf("task", "litertlm", "gguf")
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

    // On-device LLM runtime (Gemma / Phi via the MediaPipe LLM Inference API).
    implementation("com.google.mediapipe:tasks-genai:0.10.24")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
