# PushPush 2 Android Port

기존 Flash(SWF) 기반 **푸시푸시**를 Android 스마트폰에서 플레이할 수 있도록 네이티브 Android 앱으로 재구현하는 프로젝트입니다.

## 개발 환경

현재 개발 기준 환경:

- **Windows 11**
- **Android Studio**
- **GitHub**
- **웹 ChatGPT + GitHub MCP**

WSL2, Docker, 로컬 MCP 서버는 사용하지 않습니다.

작업 흐름:

```text
웹 ChatGPT
→ GitHub MCP
→ 1006U/pushpush2 코드 수정
→ Windows 11에서 git pull
→ Android Studio 실행/테스트
```

## 현재 구현 상태

- Android 앱 기본 프로젝트
- Kotlin 기반 Sokoban 게임 엔진
- 벽 충돌 / 박스 밀기 / 목표 판정
- 원본 SWF에서 **실제 스테이지 66개 추출 및 이식**
- 터치 방향패드
- 스테이지 재시작
- 스테이지 선택
- 클리어 후 다음 스테이지 해금
- SharedPreferences 기반 진행상황 저장
- 원본 벽 / 목표 / 박스 / 플레이어 기본 그래픽 추출
- 원본 14×14 픽셀 그래픽 적용
- 원본 사운드 추출 스크립트 추가
- 이동 / 클리어 / 버튼 사운드 연결 코드 추가
- GitHub Actions Android 빌드 CI 추가
- GameEngine JVM 회귀 테스트 + 66개 스테이지 구조 무결성 테스트
- Galaxy S8(Android 7.0/API 24)부터 지원
- Android 16(API 36) compileSdk/targetSdk 대응
- CI에서 API 24 / API 36 에뮬레이터 실제 실행 및 화면 캡처

플레이어는 원본 Sprite 370의 1~70프레임 대기/눈 깜빡임과 71~76프레임 성공 반응 타이밍을 재현합니다.
현재 인게임 캐릭터 그래픽은 사용자 제공 캐릭터를 14×14 픽셀 타일에 맞춰 사용합니다.

## 원본 SWF 분석 결과

- Flash/SWF 버전: **6**
- 원본 화면 크기: **240 × 250 px**
- 프레임 속도: **10 fps**
- 실제 퍼즐 스테이지: **66개**
- 원본 퍼즐 격자 간격: **14 px**
- `stage_map` 프레임 1~66: 퍼즐 스테이지
- 프레임 67: 엔딩
- 프레임 68: 빈 프레임

주요 심볼:

- `brick` → 벽
- `house` → 목표
- `ball` → 박스
- `charater` → 플레이어

원본 사운드:

- `success.wav`
- `start.wav`
- `move.wav`
- `clear.wav`
- `button.wav`

실제 DefineSound 데이터는 MP3 형식입니다.

자세한 분석:

```text
docs/ORIGINAL_SWF_NOTES.md
```

## Windows 11에서 프로젝트 받기

PowerShell:

```powershell
cd C:\Users\kim\Documents
git clone https://github.com/1006U/pushpush2.git
cd pushpush2
```

이미 clone했다면:

```powershell
cd C:\Users\kim\Documents\pushpush2
git pull
```

## 원본 SWF 넣기

첨부한 원본 파일을 Windows 프로젝트에:

```text
C:\Users\kim\Documents\pushpush2\original\game.swf
```

로 넣습니다.

`original/*`는 `.gitignore` 처리되어 GitHub에 올라가지 않습니다.

## 원본 사운드 추출

원본 SWF를 위 경로에 넣은 뒤 PowerShell에서:

```powershell
cd C:\Users\kim\Documents\pushpush2
py tools\extract_original_audio.py
```

실행합니다.

성공하면 자동으로:

```text
app\src\main\res\raw\
├── success.mp3
├── start.mp3
├── move.mp3
├── clear.mp3
└── button.mp3
```

가 생성됩니다.

앱 코드는 해당 파일이 존재할 경우 자동으로 재생합니다.

현재 연결된 동작:

- 이동 성공 → `move.mp3`
- 박스를 목표에 넣음 → `success.mp3`
- 스테이지 클리어 → `clear.mp3`
- 스테이지 선택 / 재시작 / 다음 스테이지 → `button.mp3`
- 인트로 제거에 따라 `start` 사운드는 현재 자동 재생하지 않음

## Android Studio에서 실행

Android Studio:

```text
File
→ Open
→ C:\Users\kim\Documents\pushpush2
```

Gradle Sync 완료 후 에뮬레이터 또는 실제 스마트폰에서 **Run ▶** 을 실행합니다.

## PowerShell에서 Debug APK 빌드

```powershell
.\gradlew.bat assembleDebug
```

성공 시:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## GitHub Actions 자동 빌드

`main` 브랜치에 코드가 올라가면 GitHub Actions가:

```text
./gradlew testDebugUnitTest lintDebug assembleDebug
```

를 자동 실행합니다.

성공하면 Actions 실행 결과에 Debug APK와 API별 smoke screenshot Artifact가 생성됩니다.

```text
pushpush2-debug-apk
pushpush2-smoke-api-24
pushpush2-smoke-api-36
```

따라서 로컬 Android Studio를 열기 전에도 GitHub에서 컴파일 오류를 확인할 수 있습니다.

## 현재 프로젝트 구조

```text
pushpush2/
├── .github/
│   └── workflows/
│       └── android-ci.yml
├── app/
│   └── src/main/
│       ├── java/com/pushpush2/
│       │   ├── MainActivity.kt
│       │   ├── audio/
│       │   │   └── AudioPlayer.kt
│       │   ├── data/
│       │   │   └── ProgressStore.kt
│       │   ├── game/
│       │   │   ├── Direction.kt
│       │   │   ├── GameEngine.kt
│       │   │   ├── GameState.kt
│       │   │   ├── Position.kt
│       │   │   ├── Stage.kt
│       │   │   └── StageRepository.kt
│       │   └── ui/
│       │       └── GameView.kt
│       └── res/
│           ├── drawable-nodpi/
│           │   ├── tile_brick.png
│           │   ├── tile_goal.png
│           │   ├── tile_box.png
│           │   └── tile_player.png
│           └── values/
├── docs/
│   └── ORIGINAL_SWF_NOTES.md
├── original/
│   └── README.md
├── tools/
│   └── extract_original_audio.py
├── PROJECT_STATUS.md
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradlew.bat
```

## 다음 작업

1. Galaxy S8 / S10 실기기에서 게임 영역과 D-pad 크기 미세 조정
2. 66개 스테이지 실제 플레이 검증
3. Android Studio 실기기 테스트
4. Debug APK 안정화
5. 릴리즈 APK 생성

현재 진행상황은 `PROJECT_STATUS.md`에 기록합니다.


## 새 ChatGPT 대화에서 이어서 개발

웹 ChatGPT 대화가 길어졌다면 새 채팅에서 다음 파일을 먼저 읽도록 요청하세요.

```text
HANDOFF.md
PROJECT_STATUS.md
README.md
docs/ORIGINAL_SWF_NOTES.md
```

가장 간단한 시작 문장:

```text
GitHub MCP로 1006U/pushpush2의 HANDOFF.md를 읽고
main 최신 상태와 GitHub Actions 결과를 확인한 뒤 이어서 개발해줘.
```

원본 SWF를 추가 분석해야 하는 작업에서는 `game.swf`를 새 채팅에 다시 첨부해야 합니다.
