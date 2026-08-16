plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

android {
    namespace = "com.jipzeongit.arcsync"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jipzeongit.arcsync"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "1.0.2"
    }

    val releaseStoreFile = System.getenv("ARC_RELEASE_STORE_FILE")
    val releaseStorePassword = System.getenv("ARC_RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = System.getenv("ARC_RELEASE_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("ARC_RELEASE_KEY_PASSWORD")

    signingConfigs {
        if (
            !releaseStoreFile.isNullOrBlank() &&
            !releaseStorePassword.isNullOrBlank() &&
            !releaseKeyAlias.isNullOrBlank() &&
            !releaseKeyPassword.isNullOrBlank()
        ) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose - JetBrains 版本
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    
    // Material3 - AndroidX 版本
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    
    // Backdrop - 液态玻璃效果
    implementation(libs.kyant.backdrop)
    implementation(libs.kyant.shapes)
    
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.jsoup)
    implementation(libs.okhttp)

    debugImplementation("org.jetbrains.compose.ui:ui-tooling:1.11.1")
}
