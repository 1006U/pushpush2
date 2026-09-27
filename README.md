# PushPush 2 — Lenovo K10 Pro / Android 13

이 브랜치는 **Lenovo K10 Pro Android 13 태블릿** 전용 호환 버전입니다.

```text
branch: lenovo-k10-pro-android13
applicationId: com.pushpush2.lenovok10pro
versionCode: 2
versionName: 0.1.1-k10pro
minSdk: 24
targetSdk: 33
compileSdk: 36
```

패키지 ID가 `main`과 달라 일반 Android 버전과 독립적으로 설치할 수 있습니다.

## 현재 구현 상태

공통 게임 기능:

- Kotlin Sokoban 엔진
- 원본 SWF의 66개 스테이지
- 벽 충돌 / 박스 밀기 / 목표 판정
- 진행 상황 저장 및 스테이지 해금
- 자동 다음 스테이지 진행
- Game Clear 화면
- 목표 성공/클리어 사운드
- 이동 불가 진동 처리
- 피처폰 스타일 터치 조작부

## Lenovo 전용 Android 설정

- Android 13 기준 `targetSdk 33`
- `sensorPortrait`
- `resizeableActivity=true`
- small / normal / large / xlarge 화면 지원
- anyDensity 지원
- 상태바 inset 처리
- 태블릿에서 게임 화면과 조작부를 세로 비율에 맞게 배치

## 설치 상태

Lenovo K10 Pro 실기기에서 테스트 APK 설치가 확인되었습니다.

일반 `main` 패키지와 다른:

```text
com.pushpush2.lenovok10pro
```

를 사용하므로 두 버전이 패키지 이름으로 충돌하지 않습니다.

업데이트 설치 시에는 같은 keystore를 사용하고 versionCode를 증가시켜야 합니다.

## CI

전용 workflow:

```text
.github/workflows/lenovo-k10-pro-android13-ci.yml
```

검증 항목:

1. Unit tests
2. Android lint
3. Debug/release APK build
4. API 33 태블릿 emulator 설치
5. 앱 실행
6. 프로세스 생존 확인
7. screenshot artifact

공통 workflow도 존재합니다.

```text
.github/workflows/android-ci.yml
.github/workflows/friend-release.yml
```

## UI 상태

이 브랜치는 분기 시점의 태블릿용 터치 UI를 유지합니다.

`main`에서는 이후 하단 UI가 추가로 수정되어 현재:

- 단계
- 리셋
- 취소(1스텝 Undo)
- 종료
- 확인
- 독립 조정된 방향 터치영역

구조로 발전했습니다.

이 최신 `main` UI 변경은 Lenovo 브랜치에 자동 반영되지 않으며, 필요할 경우 Android 13 태블릿 레이아웃에 맞춰 선택적으로 동기화합니다.

## 사운드 / 그래픽 상태

프로젝트 공통으로 완료된 주요 작업:

- 목표 성공 사운드 복원
- 목표 진입 후 사운드 중단 문제 수정
- 목표 집 이미지 최종 반영
- 벽돌 타일 정리 및 색상 보정
- 1~66 스테이지 벽 배치 검증
- 벽 연결부 시각 보정

단, `main` 이후 변경사항은 이 브랜치와 코드가 실제로 동기화된 범위 내에서만 적용됩니다.

## 현재 남은 주요 작업

- [ ] Lenovo K10 Pro 실기기에서 진동 지원 여부 최종 확인
- [ ] Lenovo 전용 Signed APK 업데이트 설치 검증
- [ ] 필요 시 `main` 최신 터치 UI 선택 반영
- [ ] 66개 스테이지 태블릿 실플레이 검증

## 브랜치 관계

- `main`: 일반 Android 스마트폰
- `lenovo-k10-pro-android13`: Lenovo K10 Pro / Android 13
- `blackberry-classic`: BlackBerry Classic / BB10

자세한 전용 설정은 `LENOVO_K10_PRO_ANDROID13.md` 참고.
