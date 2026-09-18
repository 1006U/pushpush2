# 친구 배포용 Release APK

이 프로젝트는 Google Play에 공개하지 않고 친구에게 직접 APK를 전달하는 방식을 지원합니다.

## 배포 방식

권장 흐름:

```text
main 안정화
→ 버전 결정
→ GitHub Actions "Friend Release APK" 실행
→ 서명된 PushPush2-vX.Y.Z.apk 생성
→ APK를 직접 다운로드
→ 카카오톡 / Google Drive / NAS 등으로 친구에게 전달
```

GitHub Release를 자동 공개하지 않습니다.

## 1. 최초 1회: Release keystore 만들기

Android Studio:

```text
Build
→ Generate Signed Bundle / APK
→ APK
→ Create new...
```

예시:

```text
Key store path: C:\Users\<사용자>\Documents\pushpush2-signing\pushpush2-release.jks
Key alias: pushpush2
Validity: 50 years
```

`.jks` 파일과 비밀번호는 잃어버리면 안 됩니다.

이미 친구에게 배포한 앱을 나중에 업데이트하려면 반드시 같은 keystore/key alias로 다시 서명해야 합니다.

프로젝트의 `.gitignore`는 다음 파일을 Git에서 제외합니다.

```text
*.jks
*.keystore
*.p12
keystore.properties
signing.properties
release-keystore/
```

서명키를 GitHub 저장소에 직접 커밋하지 마세요.

## 2. keystore를 Base64 문자열로 변환

Windows PowerShell:

```powershell
$path = "C:\Users\<사용자>\Documents\pushpush2-signing\pushpush2-release.jks"
[Convert]::ToBase64String([IO.File]::ReadAllBytes($path)) | Set-Clipboard
```

클립보드에 복사된 긴 문자열을 GitHub Secret으로 등록합니다.

## 3. GitHub Actions Secrets 등록

GitHub 저장소:

```text
Settings
→ Secrets and variables
→ Actions
→ New repository secret
```

다음 4개를 등록합니다.

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

값:

- `ANDROID_KEYSTORE_BASE64`: 앞 단계에서 만든 Base64 문자열
- `ANDROID_KEYSTORE_PASSWORD`: keystore 비밀번호
- `ANDROID_KEY_ALIAS`: 예: `pushpush2`
- `ANDROID_KEY_PASSWORD`: key 비밀번호

비밀번호와 Base64 문자열은 코드나 문서에 기록하지 않습니다.

## 4. 수동으로 APK 만들기

GitHub 저장소의 Actions에서:

```text
Actions
→ Friend Release APK
→ Run workflow
```

`version_name`에 예를 들어:

```text
0.1.0
```

을 입력합니다.

workflow는 자동으로:

1. JVM unit test 실행
2. Release lint 실행
3. Release APK 빌드
4. 등록된 keystore로 APK 서명
5. `apksigner`로 서명 검증
6. APK Artifact 업로드

를 수행합니다.

성공하면 Artifact 이름:

```text
PushPush2-v0.1.0-signed-apk
```

내부 파일:

```text
PushPush2-v0.1.0.apk
```

를 다운로드합니다.

## 5. 태그로 APK 만들기

태그 이름이 `v`로 시작하면 같은 workflow가 자동 실행됩니다.

예:

```powershell
git checkout main
git pull
git tag v0.1.0
git push origin v0.1.0
```

이 경우 APK의 versionName은 태그에서 자동으로 `0.1.0`을 사용합니다.

## 6. versionCode 관리

GitHub Actions 배포에서는 workflow 실행 번호를 Android `versionCode`로 사용합니다.

따라서 Release workflow를 새로 실행할수록 versionCode가 자동으로 증가합니다.

`versionName`은 사용자가 지정합니다.

예:

```text
0.1.0
0.1.1
0.2.0
1.0.0
```

로컬 기본값은 계속:

```text
versionCode = 1
versionName = 0.1.0
```

이며 GitHub Actions 실행 시 환경 변수로 덮어씁니다.

## 7. 친구에게 업데이트 배포

첫 배포:

```text
PushPush2-v0.1.0.apk
```

다음 배포:

```text
PushPush2-v0.1.1.apk
```

처럼 생성합니다.

다음 조건이 유지되면 기존 앱을 삭제하지 않고 업데이트 설치할 수 있습니다.

- applicationId가 `com.pushpush2`로 동일
- 같은 release keystore 사용
- 같은 key alias 사용
- versionCode가 이전 APK보다 큼

기존 앱을 삭제하지 않으면 SharedPreferences에 저장된 스테이지 진행 상황도 유지됩니다.

## 8. 친구 휴대폰 설치

APK를 다운로드한 뒤 Android에서 해당 파일을 실행합니다.

처음에는 브라우저 또는 파일 관리자에 대해 `알 수 없는 앱 설치` 허용을 요구할 수 있습니다.

설치가 끝난 뒤 해당 권한은 다시 꺼도 됩니다.

## 로컬 Android Studio에서 직접 서명하기

GitHub Actions를 사용하지 않고 Android Studio에서 직접 만들 수도 있습니다.

```text
Build
→ Generate Signed Bundle / APK
→ APK
→ 기존 pushpush2-release.jks 선택
→ release
→ Create
```

같은 keystore를 사용하면 GitHub Actions에서 만든 APK와 이후 업데이트 호환성을 유지할 수 있습니다.
