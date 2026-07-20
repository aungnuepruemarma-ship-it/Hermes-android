plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "net.nous.hermes"
    compileSdk = 34

    defaultConfig {
        applicationId = "net.nous.hermes"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        compose = true
    }
    composeOptions {
        // Compose compiler version is INDEPENDENT of the Kotlin version.
        // For Kotlin 1.9.24 the matching Compose compiler is 1.5.14.
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Release signing reads credentials from environment (set by CI from
    // repo secrets). When any are absent, this block is skipped so local
    // debug builds and unsigned assemblies are unaffected.
    val ksPath = System.getenv("HERMES_KEYSTORE_PATH")
    val ksPass = System.getenv("HERMES_KEYSTORE_PASSWORD")
    val ksAlias = System.getenv("HERMES_KEY_ALIAS")
    val ksKeyPass = System.getenv("HERMES_KEY_PASSWORD")
    if (ksPath != null && ksPass != null && ksAlias != null && ksKeyPass != null) {
        signingConfigs {
            create("release") {
                storeFile = file(ksPath)
                storePassword = ksPass
                keyAlias = ksAlias
                keyPassword = ksKeyPass
            }
        }
        buildTypes {
            release {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.android)
}
