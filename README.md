# PushPush 2 Android Port

Flash(SWF) 기반 **푸시푸시(PUSH II)** 를 Flash 런타임 없이 Android/Kotlin으로 재구현하는 프로젝트입니다.

저장소:

```text
https://github.com/1006U/pushpush2
branch: main
```

## 개발 환경

현재 기준 환경:

- Windows 11
- Android Studio
- GitHub
- 웹 ChatGPT + GitHub 연동
- JDK 17
- AGP 8.13.2
- Gradle 8.13
- minSdk 24 (Android 7.0 / Galaxy S8)
- compileSdk 36
- targetSdk 36

로컬 작업 경로 예시:

```text
C:\Dev\Android\pushpush2
```

## 현재 구현 상태

### 게임

- Kotlin 기반 Sokoban 게임 엔진
- 벽 충돌 / 박스 밀기 / 목표 판정
- 원본 SWF의 실제 퍼즐 스테이지 **66개** 이식
- SharedPreferences 기반 진행 상황 저장
- 스테이지 클리어 시 다음 스테이지 해금
- 클리어 팝업 없이 자동으로 다음 스테이지 진행
- 66 스테이지 클리어 후 Game Clear 화면 표시
- STAGE 선택 / RESET / 하드웨어 키 입력 지원

### 원본 SWF 분석

원본 분석 결과:

- SWF 버전: 6
- 원본 화면: 240 × 250 px
- 프레임 속도: 10 fps
- 퍼즐 타일: 14 × 14 px
- `stage_map` frame 1~66: 퍼즐 스테이지
- frame 67: 엔딩
- frame 68: 빈 프레임

원본 주요 심볼:

- `brick` → 벽
- `house` → 목표
- `ball` → 박스
- `charater` → 플레이어

자세한 분석:

```text
docs/ORIGINAL_SWF_NOTES.md
```

## 현재 그래픽

현재 `main`은 원작 피처폰 화면을 기준으로 픽셀아트 UI를 재구성하고 있습니다.

### 게임 타일

```text
app/src/main/res/drawable-nodpi/
├── tile_brick.png
├── tile_goal.png
├── tile_goal_after_02.png
├── tile_goal_after_03.png
├── ...
├── tile_goal_after_11.png
├── tile_box.png
├── tile_player.png
└── game_clear_screen.webp
```

현재 적용 상태:

- 벽돌: 사용자 제공 원본 벽돌을 기반으로 한 56×56 픽셀아트 리소스
- 실제 게임 벽도 `tile_brick.png`를 직접 렌더링
- 내부 통로: 원본 대각선 타일 bitmap을 nearest-neighbor로 확대
- 공을 넣기 전 목표 집: 노란 집 픽셀아트
- 목표 성공 애니메이션: `tile_goal_after_02 ~ 11`
- 이미지 확대 시 bitmap filtering을 끄고 픽셀 경계를 유지
- Game Clear 화면은 별도 `game_clear_screen.webp` 리소스로 표시

### 플레이어

플레이어는 원본 Sprite 370의 타이밍을 기준으로 동작합니다.

- frame 1~70: 대기 / 눈 깜빡임
- frame 71~76: 목표 성공 반응
- 10 fps 기준
- 현재 인게임 캐릭터는 사용자 지정 픽셀 캐릭터 사용
- 상단 UI에는 별도의 리마스터 캐릭터 이미지 사용

## UI

현재 Android UI는 원본 240×250 화면을 그대로 레터박스로 복제하지 않고 스마트폰 화면에 맞게 확장합니다.

- 상단: 캐릭터 + 대사 패널
- 중앙: 반응형 퍼즐 보드
- 하단: `STAGE` / `STEP` 상태바
- 최하단: 피처폰 키패드형 터치 조작부
- 보드는 스테이지 크기에 따라 자동 확대/축소
- 작은 화면에서도 전체 스테이지가 잘리지 않도록 조정
- Galaxy S8 / S10 실기기 기준을 포함

## 사운드

현재 `res/raw`에 포함된 사운드:

```text
app/src/main/res/raw/
├── button.mp3
├── clear.mp3
├── move.mp3
└── success.mp3
```

현재 연결:

- 일반 이동 → `move.mp3`
- 공을 목표에 넣음 → `success.mp3`
- 스테이지 클리어 → `clear.mp3`
- UI 버튼 → `button.mp3`

`start.mp3`는 이전 작업에서 제거되어 현재 자동 재생하지 않습니다.

### 현재 확인 중인 사운드 문제

일부 실행 환경에서 **공을 목표에 넣는 순간까지 사운드가 정상 재생되다가 이후 사운드가 멈추는 현상**이 보고되었습니다.

현재 코드에서는 목표 진입 시 짧은 시간 안에 `move`와 `success` 재생이 연속으로 발생할 수 있어 `MediaPlayer` 디코더 충돌 가능성을 점검 중입니다.

다음 수정 우선순위:

1. 목표 진입 시 `move`와 `success` 중복 재생 제거
2. 효과음 재생 전에 이전 `MediaPlayer` 안전 정리
3. API 24 / 최신 Android에서 사운드 회귀 테스트
4. 필요하면 짧은 효과음을 `SoundPool` 기반으로 전환

## GitHub Actions

Workflow:

```text
.github/workflows/android-ci.yml
```

주요 검증:

```text
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

추가로:

- Android 7.0 / API 24 emulator smoke test
- Android 16 / API 36 emulator smoke test
- Debug APK Artifact
- API별 화면 캡처 Artifact

최근 `main`의 Android CI #140은 성공했습니다.

## 친구 배포용 APK

Workflow:

```text
.github/workflows/friend-release.yml
```

GitHub Secrets:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

자세한 배포 방법:

```text
RELEASING.md
```

## 프로젝트 구조

```text
pushpush2/
├── .github/workflows/
│   ├── android-ci.yml
│   └── friend-release.yml
├── app/src/main/
│   ├── java/com/pushpush2/
│   │   ├── MainActivity.kt
│   │   ├── audio/AudioPlayer.kt
│   │   ├── data/ProgressStore.kt
│   │   ├── game/
│   │   │   ├── Direction.kt
│   │   │   ├── GameEngine.kt
│   │   │   ├── GameState.kt
│   │   │   ├── Position.kt
│   │   │   ├── Stage.kt
│   │   │   └── StageRepository.kt
│   │   └── ui/
│   │       ├── GameView.kt
│   │       ├── HeaderCharacterAsset.kt
│   │       ├── OriginalAnimationFrames.kt
│   │       ├── RetroControlsView.kt
│   │       ├── StageLayoutPolicy.kt
│   │       └── StageSelectView.kt
│   └── res/
│       ├── drawable-nodpi/
│       └── raw/
├── docs/ORIGINAL_SWF_NOTES.md
├── original/README.md
├── tools/
├── HANDOFF.md
├── PROJECT_STATUS.md
├── README.md
└── RELEASING.md
```

## 현재 남은 주요 작업

- [ ] 목표 진입 후 사운드 정지 문제 수정 및 실기기 검증
- [ ] 새 목표 집 이미지 최종 반영/검증
- [ ] 원본 기준으로 1~66 스테이지 벽돌 배치 시각 검증
- [ ] 벽돌이 끊겨 보이는 스테이지 수정
- [ ] Galaxy S8 실제 화면 테스트
- [ ] Galaxy S10 실제 화면 테스트
- [ ] 66개 스테이지 실제 플레이 검증
- [ ] 첫 Signed Friend Release APK 생성/업데이트 설치 확인

## 새 ChatGPT 대화에서 이어서 개발

새 대화에서는 아래 파일을 먼저 확인합니다.

```text
HANDOFF.md
PROJECT_STATUS.md
README.md
docs/ORIGINAL_SWF_NOTES.md
```

시작 문장 예시:

```text
GitHub 연동으로 1006U/pushpush2의 HANDOFF.md와 PROJECT_STATUS.md를 읽고
main 최신 상태와 GitHub Actions 결과를 확인한 뒤 이어서 개발해줘.
```

원본 `game.swf` 자체를 추가 분석해야 하는 작업에서는 원본 파일을 다시 제공해야 합니다.
