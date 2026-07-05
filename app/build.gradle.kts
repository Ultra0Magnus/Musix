import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// ── Release signing ────────────────────────────────────────────────────────────
// Secrets live in keystore.properties at the repo root (git-ignored, never committed).
// If the file is absent (CI, fresh clone), the release build falls back to the debug
// key so the project always builds — only the maintainer's machine produces a
// properly-signed v1.0 APK. See keystore.properties.template for the format.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(FileInputStream(keystorePropsFile))
}
val hasReleaseKeystore = keystorePropsFile.exists()

android {
    namespace = "com.louis.musix"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.louis.musix"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.0.0"

        vectorDrawables { useSupportLibrary = true }

        // CLIENT_ID Spotify injecté depuis local.properties (ne pas committer la valeur réelle)
        // Ajouter dans local.properties : spotify.client.id=50fa02008df5469fbdeb8407ec15ff80
        buildConfigField("String", "SPOTIFY_CLIENT_ID",
            (project.findProperty("spotify.client.id") as String? ?: "").let { '"' + it + '"' }
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile     = rootProject.file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias      = keystoreProps["keyAlias"] as String
                keyPassword   = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // NewPipeExtractor relies on reflection: minification disabled for the sideload build
            isMinifyEnabled = false
            isShrinkResources = false
            // Use the real release key when keystore.properties is present, else fall back to debug
            signingConfig = if (hasReleaseKeystore)
                signingConfigs.getByName("release")
            else
                signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Room: export du schéma versionné (app/schemas/) pour pouvoir écrire et tester les migrations
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Compose (BOM aligne toutes les versions Compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.navigation.compose)

    // Media3 (ExoPlayer + MediaSession)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.common)
    implementation(libs.media3.ui)

    // NewPipeExtractor (extraction YouTube)
    implementation(libs.newpipe.extractor)
    implementation(libs.okhttp)

    // Coil pour les images
    implementation(libs.coil.compose)

    // Palette (extraction couleur dominante artwork)
    implementation(libs.androidx.palette)

    // Room (DB locale)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Reorderable (drag & drop)
    implementation(libs.reorderable)

    // Koin (DI)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Kotlinx
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Testing
    testImplementation(libs.junit)
}
