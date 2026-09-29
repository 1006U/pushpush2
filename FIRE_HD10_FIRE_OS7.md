# Fire HD 10 / Fire OS 7 Port

This branch ports the current `main` game to Amazon Fire HD 10 tablets running
Fire OS 7, including the requested Fire OS 7.3.1.2 environment.

## Branch

```text
FireHD10
```

## Android / Fire OS configuration

- Fire OS family: 7
- Android compatibility level: Android 9 / API 28
- compileSdk: 36
- targetSdk: 28
- minSdk: 24
- JDK: 17
- applicationId: `com.pushpush2.firehd10`
- Large and xlarge screens explicitly enabled
- Activity remains resizable
- Portrait and landscape rotation remain enabled

The game itself is pure Android/Kotlin and does not use Google Play Services, so
there is no Google-service dependency to replace for Fire OS.

## Fire HD 10 display target

The dedicated CI smoke test uses:

```text
1920 x 1200
hdpi-class density (240 dpi emulator override)
API 28
```

The current responsive `main` layout is retained:

- Portrait: game above, touch controls below
- Landscape: game left, touch controls right
- Stage state survives orientation changes
- Touch controls, keyboard keys, D-pad and gamepad input remain available

## CI validation

Workflow:

```text
.github/workflows/firehd10-fireos7-ci.yml
```

It performs:

1. JUnit tests
2. Android lint
3. Debug APK build
4. Release APK build
5. Manifest verification for minSdk 24 / targetSdk 28
6. API 28 tablet emulator install
7. Fire HD 10-sized 1920x1200 display override
8. Landscape launch/process survival check
9. Portrait rotation/process survival check
10. Screenshot artifact upload

## Install on the Fire HD 10

For a development build, enable Developer Options / USB debugging on the tablet,
connect it by ADB, then install the debug APK:

```bash
adb install -r app-debug.apk
```

For a normal signed APK, use Android Studio:

```text
Build
→ Generate Signed App Bundle or APK
→ APK
→ release
```

Keep using the same release keystore for future updates of the
`com.pushpush2.firehd10` package.

## Notes

This port intentionally does not modify the `main` branch. Device-specific
changes stay in `FireHD10`.
