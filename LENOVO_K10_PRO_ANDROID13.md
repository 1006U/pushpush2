# Lenovo K10 Pro / Android 13 Port

This branch is dedicated to the Lenovo K10 Pro tablet running Android 13.

## Branch

```text
lenovo-k10-pro-android13
```

## Package

```text
com.pushpush2.lenovok10pro
```

The package ID is intentionally different from the main phone build, so the
Lenovo build can be installed independently.

## Android configuration

- compileSdk: 36
- targetSdk: 33 (Android 13)
- minSdk: 24
- JDK: 17
- Portrait tablet UI with sensor portrait support
- Resizable activity enabled for Android tablet window management
- Large/xlarge screens explicitly supported

## Validation

The dedicated workflow:

```text
.github/workflows/lenovo-k10-pro-android13-ci.yml
```

performs:

1. Unit tests
2. Android lint
3. Debug/release APK build
4. APK installation on an API 33 tablet emulator
5. App launch
6. Process survival check
7. Screenshot artifact capture

## Local signed APK

Use the same Android Studio flow:

```text
Build
→ Generate Signed App Bundle or APK
→ APK
→ release
```

For a fresh Lenovo installation, the package is separate from the main build.

If updating a previously installed Lenovo-specific APK later, keep using the
same release keystore and increase versionCode.
