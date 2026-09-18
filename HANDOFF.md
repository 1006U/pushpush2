# HANDOFF

이 파일은 웹 ChatGPT 대화가 길어졌을 때 새 대화에서 프로젝트를 그대로 이어가기 위한 인수인계 문서입니다.

## 새 ChatGPT 대화에서 가장 먼저 보낼 문장

아래 내용을 그대로 붙여넣으면 됩니다.

```text
GitHub MCP를 사용해서 https://github.com/1006U/pushpush2 프로젝트를 이어서 개발해줘.

먼저 main 브랜치의 HANDOFF.md, PROJECT_STATUS.md, README.md,
docs/ORIGINAL_SWF_NOTES.md와 최신 소스 코드를 읽고 현재 상태를 파악해.

개발 환경은 Windows 11 + Android Studio + 웹 ChatGPT + GitHub MCP야.
WSL2, Docker, 로컬 MCP 서버는 사용하지 않아.

코드를 수정할 때는 GitHub MCP로 1006U/pushpush2 저장소에 직접 반영하고,
매 변경 후 GitHub Actions Android CI 상태를 확인해.
CI가 실패하면 workflow/job 로그를 읽고 원인을 수정한 다음 다시 성공 여부를 확인해.

원본 game.swf 자체를 추가 분석해야 하는 작업이라면 내가 새 대화에 다시 첨부할게.

현재 우선순위는 HANDOFF.md와 PROJECT_STATUS.md의 "다음 우선순위"를 기준으로 이어서 진행해줘.
```

## 프로젝트 목적

Flash 기반 원본 **푸시푸시**를 Flash 런타임 없이
Android 스마트폰용 네이티브 Kotlin 앱으로 재구현합니다.

목표는 원본 게임의 스테이지/그래픽/사운드/게임 규칙은 최대한 보존하되,
UI와 조작은 현대 스마트폰에 맞게 최적화하는 것입니다.

## 저장소

```text
https://github.com/1006U/pushpush2
branch: main
```

## 개발 환경

- Windows 11
- Android Studio
- 웹 ChatGPT
- GitHub MCP
- WSL2 사용 안 함
- Docker 사용 안 함
- 로컬 MCP 서버 사용 안 함

작업 흐름:

```text
웹 ChatGPT
  ↓ GitHub MCP
GitHub main 브랜치 수정
  ↓
GitHub Actions assembleDebug 검증
  ↓
Windows 11
  ↓ git pull
Android Studio / Emulator / 실제 스마트폰 테스트
```

## 현재까지 완료된 주요 기능

- Android 기본 프로젝트 구성
- Kotlin Sokoban 게임 엔진
- 벽 충돌 / 박스 밀기 / 목표 판정
- 원본 SWF 스테이지 66개 추출 및 적용
- 원본 벽 / 목표 / 박스 / 플레이어 기본 그래픽 적용
- SharedPreferences 진행 상황 저장
- 스테이지 클리어 시 다음 스테이지 해금
- 원작처럼 클리어 팝업 없이 짧은 페이드 후 다음 스테이지 자동 진행
- 원형 4방향 터치 D-pad
- 짧게 누르면 1칸 이동
- 길게 누르면 약 280ms 후 110ms 간격 반복 이동
- D-pad 드래그 방향 전환
- 누른 방향 시각 피드백
- STAGE / RETRY 소프트키
- 66개 스테이지 6열 선택 그리드
- 현재/해금/잠금 상태 표시
- 현대 스마트폰의 게임 영역에 맞춘 반응형 보드 스케일링
- 원본 14×14 픽셀 타일 비율 유지
- 가능한 경우 정수 배율 확대
- 원본 SWF 사운드 추출용 Windows Python 스크립트
- move / clear / button / start / success 사운드 연결
- 플레이어 Sprite 370의 1~70프레임 원본 10fps 대기/눈 깜빡임 적용
- 플레이어 Sprite 370의 71~76프레임 성공 반응 애니메이션 적용
- 박스 Sprite 356의 2~13프레임 목표 진입 애니메이션 적용
- 목표 위 박스는 원본 frame 13 완료 모습 유지
- 66스테이지 이후 stage_map frame 67 원본 엔딩 화면/크레딧 순환 적용
- Galaxy S8(Android 7.0/API 24)부터 실행 가능하도록 minSdk 24 적용
- Android 16 대응: compileSdk 36 / targetSdk 36
- 사용자 제공 새 캐릭터 이미지를 14×14 타일로 적용
- 피처폰 원작형 상단 캐릭터/대사 패널 적용
- 게임판 바깥 원작형 파란 배경 적용
- STAGE/STEP 하단 파란 상태바 적용
- 인접 벽을 셀별 박스가 아닌 연결형 벽돌 패턴으로 렌더링
- 원본 피처폰형 베이지 게임판 점무늬 적용
- 플레이어 스프라이트 고대비 14×14 버전 적용
- 새 캐릭터의 기본/눈 깜빡임/성공 반응 프레임 적용
- AGP 8.13.2 / Gradle 8.13 / JDK 17
- maxSdkVersion 미지정으로 이후 Android 설치 차단 없음
- GitHub Actions Debug APK 자동 빌드
- GitHub Actions Android 16(API 36) 실제 실행 smoke test
- 이미지/오디오 디코딩 오류가 앱 시작을 종료하지 않도록 startup-safe 처리

## 원본 SWF 분석 결과

- SWF 버전: 6
- 원본 Stage 크기: 240 × 250 px
- 프레임 속도: 10 fps
- 퍼즐 격자: 14 px
- 실제 퍼즐 스테이지: 66개
- stage_map 프레임 1~66: 퍼즐
- 프레임 67: 엔딩
- 프레임 68: 빈 프레임

주요 원본 심볼:

- brick → 벽
- house → 목표
- ball → 박스
- charater → 플레이어

확인된 사운드:

- success
- start
- move
- clear
- button

자세한 분석은:

```text
docs/ORIGINAL_SWF_NOTES.md
```

참조.

## UI 방향

원본의 240×250 전체 화면을 현대 스마트폰에 그대로 레터박스로 복제하지 않습니다.

대신:

- 게임 보드는 현재 기기의 실제 가용 영역을 최대한 활용
- 스테이지 종횡비 유지
- 보드가 화면 밖으로 잘리지 않게 자동 스케일
- 픽셀 아트는 가능한 경우 정수 배율 확대
- 하단은 현대 스마트폰 터치용 원형 D-pad
- 게임 화면보다 조작 UI가 과도하게 커지지 않도록 실기기에서 조정

참고했던 영상:

```text
https://youtu.be/MLvyuz7ky8c?t=8
```

영상은 원작 플레이 감각의 주요 기준으로 사용합니다.

특히 다음을 영상과 원본 SWF에 맞춥니다.

- 클리어 후 확인 팝업 없이 자동 진행
- 빠른 스테이지 전환 템포
- 캐릭터/박스 성공 반응
- 픽셀 그래픽 비율과 배치
- 게임 화면을 가리는 불필요한 Android UI 최소화

화면 해상도와 터치 조작부는 현대 스마트폰 기준으로 최적화합니다.

## 현재 주요 소스

```text
app/src/main/java/com/pushpush2/
├── MainActivity.kt
├── audio/
│   └── AudioPlayer.kt
├── data/
│   └── ProgressStore.kt
├── game/
│   ├── Direction.kt
│   ├── GameEngine.kt
│   ├── GameState.kt
│   ├── Position.kt
│   ├── Stage.kt
│   └── StageRepository.kt
└── ui/
    ├── GameView.kt
    ├── PlayerCharacterAsset.kt
    ├── RetroControlsView.kt
    └── StageSelectView.kt
```

원본 기본 그래픽:

```text
app/src/main/res/drawable-nodpi/
├── tile_brick.png
├── tile_goal.png
├── tile_box.png
└── tile_player.png
```

## 사운드 추출

원본 SWF는 GitHub에 올리지 않습니다.

Windows 로컬 경로:

```text
pushpush2\original\game.swf
```

추출:

```powershell
py tools\extract_original_audio.py
```

결과:

```text
app\src\main\res\raw\
├── success.mp3
├── start.mp3
├── move.mp3
├── clear.mp3
└── button.mp3
```

## GitHub CI

Workflow:

```text
.github/workflows/android-ci.yml
```

핵심 빌드 명령:

```text
./gradlew lintDebug assembleDebug
```

새 ChatGPT 대화에서는 코드 변경 후 항상 최신 workflow run을 확인합니다.

이 문서를 작성할 당시 최신 main 빌드는 성공 상태입니다.

## 다음 우선순위

원본 SWF 재분석 결과 Sprite 370의 1~70프레임은 방향 이동이 아니라 대기/눈 깜빡임 루프이며 이미 적용했습니다.
또한 박스가 목표에 들어갈 때 박스 Sprite 356은 frame 2부터, 플레이어 Sprite 370은 frame 71부터 반응 애니메이션을 재생하는 것을 확인했습니다.

1. Galaxy S8 / S10에서 게임 영역과 D-pad 크기 실기기 미세 조정
2. 66개 스테이지 실제 플레이 검증
3. Android Studio 에뮬레이터/실기기 테스트
4. Debug APK 안정화 후 릴리즈 APK 생성

## 중요한 주의사항

원본 `game.swf`는 GitHub에 없습니다.

따라서 다음과 같은 작업을 새 대화에서 할 경우
원본 SWF를 채팅에 다시 첨부해야 합니다.

- 새로운 Shape 추출
- 플레이어 Sprite 프레임 추가 분석
- ActionScript bytecode 추가 분석
- 원본 사운드/그래픽 재추출

이미 GitHub에 추출/기록된 스테이지 데이터와 기본 그래픽 분석 결과는
SWF 재첨부 없이 사용할 수 있습니다.

## Windows 로컬에서 최신 코드 받기

```powershell
cd C:\Users\kim\Documents\pushpush2
git pull
```

로컬 빌드:

```powershell
.\gradlew.bat assembleDebug
```

Debug APK:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## 새 대화 운영 원칙

새 ChatGPT 대화에서는:

1. HANDOFF.md를 먼저 읽음
2. PROJECT_STATUS.md로 최신 작업 상태 확인
3. main 최신 커밋과 GitHub Actions 결과 확인
4. 필요한 소스 파일을 직접 읽음
5. GitHub MCP로 코드 수정
6. CI 실패 시 로그 확인 후 직접 수정
7. 작업 완료 후 PROJECT_STATUS.md와 필요 시 HANDOFF.md 갱신

이 흐름을 유지하면 긴 채팅을 새 대화로 옮겨도 프로젝트 컨텍스트를 거의 그대로 이어갈 수 있습니다.
