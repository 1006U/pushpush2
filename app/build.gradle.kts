plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 2
val releaseVersionName = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "0.1.1-k10pro"

val releaseKeystoreFile = System.getenv("ANDROID_KEYSTORE_FILE")
val releaseKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseKeystoreFile,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.pushpush2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pushpush2.lenovok10pro"

        // Lenovo K10 Pro Android 13 dedicated branch.
        // Keep the historical minSdk so the game engine stays reusable, while
        // targeting API 33 to match the tablet's Android 13 runtime behavior.
        minSdk = 24
        targetSdk = 33

        // Local builds use these defaults. GitHub Actions can override both
        // through VERSION_CODE / VERSION_NAME for friend-distribution builds.
        versionCode = releaseVersionCode
        versionName = releaseVersionName
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystoreFile!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false

            // Keep the keystore out of Git. CI injects it only at build time.
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")

    testImplementation("junit:junit:4.13.2")
}
