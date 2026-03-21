plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.jipzeongit.arcsync"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jipzeongit.arcsync"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"
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

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.jsoup)
    implementation(libs.okhttp)

    debugImplementation("androidx.compose.ui:ui-tooling")
}

