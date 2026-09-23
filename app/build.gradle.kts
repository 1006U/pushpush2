plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
val releaseVersionName = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "0.1.0-bb10"

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
        applicationId = "com.pushpush2.blackberry"

        // BlackBerry Classic (BB10 10.3.x) uses an Android 4.3-era runtime.
        // Keep this branch installable on API 18.
        minSdk = 18

        // Target the runtime generation used by BlackBerry Classic.
        targetSdk = 18

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

    // This branch is intentionally built for the legacy Android runtime in
    // BlackBerry 10 and is not intended for Google Play publication.
    lint {
        disable += "ExpiredTargetSdkVersion"
    }
}

dependencies {

    testImplementation("junit:junit:4.13.2")
}
