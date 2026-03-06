import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

fun localProp(key: String) = "\"${localProps.getProperty(key, "")}\""

android {
    namespace = "com.fridgelist.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fridgelist.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.0"
        manifestPlaceholders["appAuthRedirectScheme"] = "com.fridgelist.auth"

        // OAuth client credentials — set in local.properties (gitignored)
        buildConfigField("String", "TODOIST_CLIENT_ID", localProp("todoist.clientId"))
        buildConfigField("String", "TODOIST_CLIENT_SECRET", localProp("todoist.clientSecret"))
        buildConfigField("String", "MICROSOFT_CLIENT_ID", localProp("microsoft.clientId"))
        buildConfigField("String", "GOOGLE_CLIENT_ID", localProp("google.clientId"))
        buildConfigField("String", "TICKTICK_CLIENT_ID", localProp("ticktick.clientId"))
        buildConfigField("String", "TICKTICK_CLIENT_SECRET", localProp("ticktick.clientSecret"))
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProps.getProperty("keystore.path", ""))
            storePassword = localProps.getProperty("keystore.storePassword", "")
            keyAlias = localProps.getProperty("keystore.keyAlias", "")
            keyPassword = localProps.getProperty("keystore.keyPassword", "")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)

    // Auth
    implementation(libs.appauth)

    // Security
    implementation(libs.androidx.security.crypto)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
}
