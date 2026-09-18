# PushPush 2 Android Port

기존 Flash(SWF) 기반 **푸시푸시**를 Android 스마트폰에서 플레이할 수 있도록 네이티브 Android 앱으로 재구현하는 프로젝트입니다.

## 개발 환경

현재 개발 기준 환경은 다음과 같습니다.

- **Windows 11**
- **Android Studio**
- **GitHub**
- **웹 ChatGPT + GitHub MCP**

WSL2, Docker, 로컬 MCP 서버는 사용하지 않습니다.

웹 ChatGPT가 GitHub MCP를 통해 저장소 코드를 직접 수정하고,
Windows 11에서 `git pull` 후 Android Studio로 빌드/실행하는 방식으로 진행합니다.

## 현재 구현 상태

현재 다음 기능까지 구현되어 있습니다.

- Android 앱 기본 프로젝트
- Kotlin 기반 Sokoban 게임 엔진
- 벽 충돌 / 박스 밀기 / 목표 판정
- 원본 SWF에서 **실제 스테이지 66개 추출 및 이식**
- 터치 방향패드
- 스테이지 재시작
- 스테이지 선택
- 클리어 후 다음 스테이지 해금
- SharedPreferences 기반 진행상황 저장
- 원본 SWF의 벽 / 목표 / 박스 / 플레이어 기본 그래픽 추출
- 원본 14×14 픽셀 타일 그래픽을 Android 게임 화면에 적용

현재 캐릭터는 원본 기본 프레임 1장을 사용하고 있으며,
방향별 이동 애니메이션과 사운드 연결은 다음 작업입니다.

## 원본 SWF 분석 결과

확인된 주요 정보:

- Flash/SWF 버전: **6**
- 원본 화면 크기: **240 × 250 px**
- 프레임 속도: **10 fps**
- 실제 퍼즐 스테이지: **66개**
- 원본 퍼즐 격자 간격: **14 px**
- `stage_map` 프레임 1~66: 게임 스테이지
- 프레임 67: 엔딩
- 프레임 68: 빈 프레임

원본에서 확인된 주요 심볼:

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

실제 SWF 내부 사운드는 MP3 압축 데이터로 저장되어 있으며 추출 가능함을 확인했습니다.

자세한 분석 내용은:

```text
docs/ORIGINAL_SWF_NOTES.md
```

를 참고하세요.

## Windows 11에서 프로젝트 받기

PowerShell:

```powershell
cd C:\Users\kim\Documents

git clone https://github.com/1006U/pushpush2.git
cd pushpush2
```

이미 clone했다면 이후에는:

```powershell
cd C:\Users\kim\Documents\pushpush2
git pull
```

만 실행하면 됩니다.

## Android Studio에서 실행

Android Studio에서:

```text
File
→ Open
→ C:\Users\kim\Documents\pushpush2
```

를 선택합니다.

Gradle Sync가 끝나면 에뮬레이터 또는 실제 Android 스마트폰을 연결하고 **Run ▶** 을 실행합니다.

## 명령줄에서 Debug APK 빌드

Android SDK와 JDK가 설정되어 있다면 PowerShell에서:

```powershell
.\gradlew.bat assembleDebug
```

성공하면 APK는:

```text
app\build\outputs\apk\debug\app-debug.apk
```

에 생성됩니다.

## 원본 SWF 보관

원본 `game.swf`는 GitHub 저장소에 올리지 않고 로컬 참조용으로 관리합니다.

Windows 프로젝트 폴더에서:

```text
pushpush2/
└── original/
    └── game.swf
```

형태로 두면 됩니다.

`.gitignore`에 의해 `original/*` 파일은 Git에 올라가지 않습니다.

## 현재 프로젝트 구조

```text
pushpush2/
├── app/
│   └── src/main/
│       ├── java/com/pushpush2/
│       │   ├── MainActivity.kt
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
├── PROJECT_STATUS.md
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradlew.bat
```

## 앞으로의 작업

1. 원본 캐릭터 방향별 프레임 분석 및 애니메이션 적용
2. 원본 `move / clear / start / button / success` 사운드 연결
3. 원본 240×250 화면 배치에 더 가깝게 UI 조정
4. 터치패드 길게 누르기 / 반복 이동 개선
5. 66개 스테이지 실제 플레이 검증
6. 실기기 테스트
7. APK 릴리즈

현재 진행상황은 `PROJECT_STATUS.md`에 기록합니다.
