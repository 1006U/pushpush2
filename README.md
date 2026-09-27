# PushPush 2 — BlackBerry Classic

이 브랜치는 **BlackBerry Classic(Q20) / BlackBerry 10 Android Runtime** 전용 호환 버전입니다.

```text
branch: blackberry-classic
applicationId: com.pushpush2.blackberry
versionName: 0.1.0-bb10
minSdk: 18
targetSdk: 18
compileSdk: 36
```

일반 Android 스마트폰용 `main`과 독립적으로 유지합니다.

## 현재 구현 상태

공통 게임 기능:

- Kotlin Sokoban 엔진
- 원본 SWF의 66개 스테이지
- 벽 충돌 / 박스 밀기 / 목표 판정
- 진행 상황 저장 및 스테이지 해금
- 자동 다음 스테이지 진행
- 66 스테이지 완료 후 Game Clear 화면
- 목표 성공/클리어 사운드
- 이동 불가 시 진동 피드백
- 목표 진입 후 사운드 중단 문제 수정
- 클리어 반응을 다음 스테이지 첫 이동 전까지 유지

## BlackBerry 전용 UI

BlackBerry Classic에는 물리 QWERTY 키보드가 있으므로 `main`의 스마트폰용 하단 터치 조작 패널을 제거했습니다.

화면 구성:

- 상단 캐릭터/대사 패널
- 중앙 게임 보드
- 하단 `STAGE / STEP` 상태바
- 별도 터치 D-pad 없음

## 물리 키 조작

기본 매핑:

```text
        T
     F  G  H
        V

T = 위
V = 아래
F = 왼쪽
H = 오른쪽
G = 확인

P = 리셋
Q = 스테이지 선택
```

호환용으로 DPAD와 WASD 입력도 유지합니다.

스테이지 선택 화면에서도 T/V/F/H 이동과 G 확인을 사용할 수 있습니다.

Game Clear 화면에서는 G가 확인 키로 동작합니다.

## 진동

모든 이동 실패 상황에서 진동하도록 구현되어 있습니다.

- 벽 충돌
- 맵 경계
- 박스를 더 이상 밀 수 없는 상황

BB10 Android Runtime 호환을 위해 API 18의 legacy vibration API를 사용합니다.

## 빌드

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

일반 Android 앱과 패키지 ID가 다르므로 별도 앱으로 설치할 수 있습니다.

## CI

브랜치 관련 workflow:

```text
.github/workflows/android-ci.yml
.github/workflows/blackberry-ci.yml
.github/workflows/friend-release.yml
```

## 현재 남은 주요 작업

- [ ] BlackBerry Classic 실제 기기에서 최신 APK 실행 재검증
- [ ] BB10 10.3.x 물리 키 매핑 최종 확인
- [ ] 실제 기기 진동 동작 확인
- [ ] BlackBerry 전용 Signed APK 업데이트 설치 검증

## 브랜치 관계

- `main`: 일반 Android 스마트폰
- `lenovo-k10-pro-android13`: Lenovo Android 13 태블릿
- `blackberry-classic`: BlackBerry Classic 전용

`main`의 최신 터치 UI 변경은 이 브랜치에 자동으로 합쳐지지 않습니다. BlackBerry 버전은 물리 키 중심 UI를 유지합니다.

자세한 BlackBerry 전용 설명은 `BLACKBERRY_CLASSIC.md` 참고.
