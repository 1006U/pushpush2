# PushPush 2 Android Port

Flash(SWF) 기반 **푸시푸시(PUSH II)** 를 Flash 런타임 없이 Android/Kotlin으로 재구현하는 프로젝트입니다.

## 브랜치

현재 유지하는 주요 브랜치:

- `main` — 일반 Android 스마트폰용 기준 브랜치
- `lenovo-k10-pro-android13` — Lenovo K10 Pro / Android 13 태블릿 전용 브랜치
- `blackberry-classic` — BlackBerry Classic / BB10 Android Runtime 전용 브랜치

`chatgpt/*` 브랜치는 과거 기능 실험/중간 작업 스냅샷입니다.

## 개발 환경

- Windows 11
- Android Studio
- JDK 17
- AGP 8.13.2
- Gradle 8.13
- applicationId: `com.pushpush2`
- versionCode: `2`
- versionName: `0.1.1`
- minSdk: 24
- compileSdk: 36
- targetSdk: 36

## 현재 구현 상태

### 게임

- Kotlin 기반 Sokoban 게임 엔진
- 벽 충돌 / 박스 밀기 / 목표 판정
- 원본 SWF의 실제 퍼즐 스테이지 **66개** 이식
- SharedPreferences 기반 진행 상황 저장
- 마지막으로 플레이한 스테이지 저장 및 앱 재실행 시 자동 복원
- 스테이지 클리어 시 다음 스테이지 해금
- 클리어 후 자동으로 다음 스테이지 진행
- 66 스테이지 완료 후 Game Clear 화면
- 스테이지 선택 / 리셋 / 하드웨어 키 입력
- 이동 불가 시 진동 피드백
  - 벽 충돌
  - 맵 경계
  - 박스 뒤 벽/박스/경계로 밀 수 없는 경우
- 스테이지 클리어 반응 문구는 다음 스테이지 첫 정상 이동 전까지 유지

### 원본 SWF 기준

- SWF 버전: 6
- 원본 화면: 240 × 250 px
- 프레임 속도: 10 fps
- 퍼즐 타일: 14 × 14 px
- `stage_map` frame 1~66: 퍼즐 스테이지
- frame 67: 엔딩
- frame 68: 빈 프레임

주요 심볼:

- `brick` → 벽
- `house` → 목표
- `ball` → 박스
- `charater` → 플레이어

자세한 내용은 `docs/ORIGINAL_SWF_NOTES.md` 참고.

## 그래픽 진행상황

현재 완료된 항목:

- 사용자 제공 원본 기반 벽돌 픽셀아트 적용
- 피처폰 캡처에서 생긴 불필요한 벽돌 줄무늬 정리
- 벽돌 내부 색상 톤 보정
- 연결된 벽이 끊겨 보이지 않도록 렌더링 보정
- 원본 기준 1~66 스테이지 벽돌 배치 시각 검증
- 바깥 모서리 장식 벽돌 보정
- 목표 집 이미지 최종 반영
- 목표 성공 애니메이션 적용
- 플레이어 대기/성공 반응 애니메이션
- Game Clear 화면 적용

주요 리소스:

```text
app/src/main/res/drawable-nodpi/
├── tile_brick.png
├── tile_goal.png
├── tile_goal_after_02.png
├── ...
├── tile_goal_after_11.png
├── tile_box.png
├── tile_player.png
└── game_clear_screen.webp
```

## 현재 UI

게임 화면은 스마트폰 비율에 맞게 반응형으로 구성합니다.

- 상단: 캐릭터 + 대사 패널
- 중앙: 반응형 퍼즐 보드
- 하단 상태바: `STAGE` / `STEP`
- 최하단: 피처폰 스타일 터치 조작부
- 세로모드: 게임 화면 위 / 컨트롤 UI 아래
- 가로모드: 게임 화면 왼쪽 / 컨트롤 UI 오른쪽
- 화면 회전 시 진행 중인 퍼즐 상태를 유지한 채 레이아웃만 전환

현재 터치 조작부:

- 좌상단: **단계**
- 우상단: **리셋**
- 좌하단: **취소**
  - 실제 기능은 1스텝 Undo
- 우하단: **종료**
- 중앙: **확인**
- 상 / 하 / 좌 / 우 방향 조작

UI 표시 영역과 실제 터치영역은 분리되어 있습니다.

- 방향 화살표의 시각적 크기는 유지
- 상/하 터치영역은 넓은 가로 영역으로 확장
- 좌/우 터치영역은 넓은 세로 영역으로 확장
- 확인 터치영역은 중앙에서 별도로 조정
- 단계/리셋/취소/종료/확인 글씨 크기 확대

## 사운드

현재 연결:

- 일반 이동 → `move.mp3`
- 공을 목표에 넣음 → `success.mp3`
- 스테이지 클리어 → `clear.mp3`
- UI 버튼 → `button.mp3`

목표 진입 후 전체 사운드가 멈추던 문제는 수정 완료했습니다.

- 목표 진입 시 move/success 중복 재생 방지
- 이전 재생 인스턴스 안전 정리
- 원본 success 사운드 복원
- 실기기 기준 동작 검증 완료

## 테스트 / CI

Workflow:

```text
.github/workflows/android-ci.yml
```

주요 검증:

```text
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

추가 검증:

- 66개 스테이지 구조 회귀 테스트
- 전체 벽 배치 fingerprint 테스트
- API 24 emulator smoke test
- API 36 emulator smoke test
- Debug APK Artifact
- API별 screenshot Artifact

## 친구 배포용 Signed APK

Workflow:

```text
.github/workflows/friend-release.yml
```

Signed Friend Release APK 생성 및 기존 설치본 업데이트 설치 확인까지 완료했습니다.

릴리즈 키는 저장소에 포함하지 않고 GitHub Secrets 또는 로컬 keystore로 관리합니다.

필요한 Secrets:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

자세한 내용은 `RELEASING.md` 참고.

## 현재 남은 주요 작업

- [ ] Galaxy S8 실제 화면 최종 테스트
- [ ] Galaxy S10 실제 화면 최종 테스트
- [ ] 66개 스테이지 실제 플레이 완주 검증

## 새 대화에서 이어서 개발

먼저 확인할 파일:

```text
HANDOFF.md
PROJECT_STATUS.md
README.md
docs/ORIGINAL_SWF_NOTES.md
```

시작 예시:

```text
GitHub 연동으로 1006U/pushpush2의 main 최신 상태와
HANDOFF.md / PROJECT_STATUS.md / README.md를 확인한 뒤 이어서 개발해줘.
```
