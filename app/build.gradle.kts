plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 2
val releaseVersionName = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "0.1.1"

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
        applicationId = "com.pushpush2"

        // Galaxy S8 shipped with Android 7.0 (API 24).
        // Keep this baseline so the original target device remains supported.
        minSdk = 24

        // Target the current stable Android compatibility level while keeping
        // minSdk independent. Newer Android releases remain installable unless
        // a future platform explicitly introduces a compatibility issue.
        targetSdk = 36

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
