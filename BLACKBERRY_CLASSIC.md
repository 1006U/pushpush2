# PushPush 2 - BlackBerry Classic branch

This branch is a dedicated compatibility build for **BlackBerry Classic (Q20)**.
It is intentionally independent from the Android smartphone build on `main`.

## Branch isolation

- Android smartphone version: `main`
- BlackBerry Classic version: `blackberry-classic`
- BlackBerry application ID: `com.pushpush2.blackberry`
- BlackBerry minimum Android runtime: API 18 (Android 4.3 era)
- Touch D-pad panel: removed
- Primary input: physical QWERTY keyboard

Changes made on this branch do not modify the files on `main`.

## BlackBerry Classic keyboard

Primary layout:

```text
        T
     F  G  H
        V

T = Up
V = Down
F = Left
H = Right
G = OK / Confirm

R = Retry current stage
M = Open stage select
```

DPAD and the older WASD mappings are retained as fallback mappings for emulator
and compatibility testing.

Inside Stage Select:

- T / V / F / H move the selection
- G confirms the selected stage
- Touch selection remains available as a fallback

On the final game-clear screen, G acts as the OK/confirm button.

## UI changes

The smartphone touch-control panel is completely removed. The square display is
used for the game instead.

The BlackBerry layout uses:

- compact header character/message area
- expanded game board area
- compact STAGE / STEP status bar
- no bottom D-pad or touch soft-key area

## Build

Windows PowerShell:

```powershell
git clone https://github.com/1006U/pushpush2.git
cd pushpush2
git checkout blackberry-classic
.\gradlew.bat assembleDebug
```

Debug APK:

```text
app\build\outputs\apk\debug\app-debug.apk
```

The APK is a dedicated package and can coexist with the normal Android package
at the package-name level.

## Notes

This branch targets the Android runtime generation used by BlackBerry 10 rather
than current Android phones. Real-device behavior still needs to be verified on
a BlackBerry Classic because BB10's Android compatibility layer can map physical
keyboard events differently from a standard Android device.
