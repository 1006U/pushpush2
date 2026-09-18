plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

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

        versionCode = 1
        versionName = "0.1.0"
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
