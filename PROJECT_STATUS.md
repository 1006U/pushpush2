# PROJECT STATUS

## 개발 환경

- Windows 11
- Android Studio
- GitHub
- 웹 ChatGPT + GitHub MCP
- WSL2 사용 안 함
- Docker 사용 안 함
- 로컬 MCP 서버 사용 안 함

## 지원 Android 기기 기준

- 최소 Android 버전: Android 7.0 (API 24)
- Galaxy S8 호환 기준 포함
- Galaxy S10 호환 기준 포함
- minSdk: 24 (Android 7.0 / Galaxy S8 기준)
- compileSdk: 36
- targetSdk: 36 (Android 16)
- AGP: 8.13.2
- Gradle: 8.13
- JDK: 17
- maxSdkVersion 미지정: 이후 Android 설치 차단 없음
- 세로 화면 고정
- 게임 보드는 실제 가용 영역에 맞춰 자동 스케일
- 작은 화면에서도 전체 맵이 잘리지 않도록 축소 허용

## 완료

- [x] Android 앱 기본 구조
- [x] Kotlin 박스 밀기 엔진
- [x] 벽 충돌 / 박스 밀기 / 목표 판정
- [x] 터치 방향패드
- [x] 스테이지 재시작
- [x] 클리어 후 다음 스테이지 해금
- [x] 진행상황 저장
- [x] 원본 SWF 분석
- [x] 원본 스테이지 66개 추출
- [x] 원본 벽 / 목표 / 박스 / 플레이어 위치 이식
- [x] 원본 벽 / 목표 / 박스 / 플레이어 기본 그래픽 추출
- [x] 원본 14×14 그래픽을 Android 화면에 적용
- [x] 원본 5개 사운드 스트림 분석
- [x] Windows용 SWF 사운드 추출 스크립트 추가
- [x] 이동 / 클리어 / 버튼 사운드 재생 코드 연결
- [x] start / success 사운드 사용 시점 분석 및 연결
- [x] GitHub Actions Android CI 추가
- [x] 영상 참고 레트로 모바일 UI 1차 적용
- [x] 원형 4방향 D-pad + STAGE/RETRY 소프트키
- [x] D-pad 길게 누르기 반복 이동
- [x] 누른 방향/소프트키 시각 피드백
- [x] 현대 스마트폰 화면에 맞춘 반응형 게임 보드 스케일링
- [x] 66개 스테이지 선택을 6열 그리드 UI로 변경
- [x] 플레이어 Sprite 370 기본 대기/눈 깜빡임 루프 분석
- [x] 원본 10fps 플레이어 대기 애니메이션 적용
- [x] Galaxy S8 지원을 위해 minSdk 24로 조정
- [x] 원작처럼 스테이지 클리어 팝업/Toast 제거
- [x] 클리어 후 짧은 페이드 뒤 다음 스테이지 자동 진행
- [x] 박스 Sprite 356 frame 2~13 원본 성공 애니메이션 적용
- [x] 플레이어 Sprite 370 frame 71~76 원본 성공 반응 애니메이션 적용
- [x] 박스 목표 진입 시 두 애니메이션 + success 사운드 동기화
- [x] 목표 위 박스는 원본 frame 13 완료 모습 유지
- [x] 66스테이지 클리어 후 원본 stage_map frame 67 엔딩 화면 재현
- [x] 엔딩 크레딧 9개를 원본처럼 페이드 인/아웃하며 순환
- [x] Galaxy S8 최소 지원 유지(minSdk 24)
- [x] Android 16(API 36) compile/target 대응
- [x] AGP 8.13.2 + Gradle 8.13 빌드 체인 업그레이드
- [x] CI에서 lintDebug + assembleDebug 호환성 검사
- [x] 사용자 제공 캐릭터 이미지를 14×14 플레이어 타일로 적용
- [x] 새 캐릭터 기준 눈 깜빡임/성공 반응 프레임 통일

## 현재 상태

원본 SWF의 `stage_map`:

- 프레임 1~66: 실제 스테이지
- 프레임 67: 엔딩
- 프레임 68: 빈 프레임

현재 앱에는 원본 66개 퍼즐 맵과 기본 그래픽이 반영되어 있습니다.

게임 화면은 원본 SWF의 240×250 고정 프레임에 묶지 않습니다.

현대 스마트폰의 실제 게임 영역 크기를 기준으로 각 스테이지 보드를 가능한 크게 확대하고 중앙 정렬합니다.

- 화면 가로/세로 크기에 맞춰 자동 스케일
- 스테이지 종횡비 유지
- 보드가 화면 밖으로 잘리지 않음
- 원본 14×14 픽셀 타일 비율 유지
- 가능한 경우 정수 배율로 확대해 픽셀 그래픽 선명도 유지
- 기기 크기와 해상도가 달라도 자동 대응

하단 조작부는 일반 Android 버튼 대신 원형 D-pad를 사용합니다.

- 짧게 누르기: 1칸 이동
- 길게 누르기: 약 280ms 후 연속 이동
- 반복 간격: 약 110ms
- 드래그로 방향 전환 가능
- 터치 방향 시각 피드백
- STAGE / RETRY 소프트키

스테이지 선택은 66개 항목을 긴 목록으로 보여주지 않고 6열 숫자 그리드로 표시합니다.

- 현재 스테이지 강조
- 해금 스테이지만 숫자 표시/선택 가능
- 잠긴 스테이지는 점으로 표시

## 현재 플레이어 캐릭터

기본 플레이어 그래픽은 사용자가 제공한 캐릭터 이미지를 현재 게임의 14×14 타일 규격에 맞춰 적용했습니다.

- 원본 이미지 비율을 유지해 14×14 안에 축소
- 최근접 보간으로 픽셀 느낌 유지
- 투명 배경 유지
- 기본 / 반쯤 감은 눈 / 감은 눈 3프레임 구성
- 목표 성공 반응 중에도 이전 캐릭터 이미지로 바뀌지 않도록 새 프레임만 사용
- 엔딩 화면의 플레이어에도 동일 캐릭터 사용

소스:

```text
app/src/main/java/com/pushpush2/ui/PlayerCharacterAsset.kt
```

## 플레이어 Sprite 370 분석 결과

원본 Sprite 370은 총 76프레임입니다.

중요한 수정 사항:

- 1~70프레임은 상/하/좌/우 방향 이동 애니메이션이 아님
- 기본 Shape 357, 358, 359를 이용한 대기/눈 깜빡임 루프
- 원본 10fps 기준으로 반복
- 눈 변화 프레임:
  - 2, 12, 38 → Shape 358
  - 3, 13, 39 → Shape 359
  - 그 외 1~70 → Shape 357
- 70프레임에서 처음으로 되돌아가 재생
- 71~76프레임은 별도 반응 애니메이션 구간

앱에는 1~70프레임 대기 애니메이션과 71~76프레임 성공 반응 애니메이션을 모두 적용했습니다.

성공 반응은 박스가 목표에 새로 들어가는 순간 시작하며 10fps로 6프레임을 재생한 뒤 평상시 1~70 루프로 돌아갑니다.

## 박스 Sprite 356 분석 결과

박스 Sprite 356은 총 13프레임입니다.

- 1프레임은 기본 박스
- 2~13프레임은 Shape/MorphShape를 이용한 별도 애니메이션
- 원본 ActionScript에서 특정 이벤트에 `gotoAndPlay(2)`가 호출되는 것을 확인
- 플레이어도 같은 시점에 `charater.gotoAndPlay(71)`가 호출됨

Android 앱에도 원본 10fps 타이밍으로 2~13프레임을 적용했습니다.

- 목표 진입 순간 frame 2부터 시작
- 12프레임 동안 원본 MorphShape 밝기/형태 변화 재현
- frame 13의 Stop 동작에 맞춰 목표 위에서는 마지막 붉은 집 모양 유지
- 목표 밖으로 다시 밀리면 일반 frame 1 파란 박스로 복귀
- 플레이어 frame 71~76과 같은 시점에 시작

## 사운드

원본 DefineSound 데이터:

- success: 약 0.78초
- start: 약 11.54초
- move: 약 0.57초
- clear: 약 8.27초
- button: 약 0.31초

앱 연결 상태:

- [x] move
- [x] clear
- [x] button
- [x] success
  - 박스를 밀어서 새 위치가 목표 지점일 때 재생
- [x] start
  - 앱이 처음 생성될 때 재생

## 원작 실행 영상 참고 기준

주요 참고 영상:

```text
https://youtu.be/MLvyuz7ky8c?t=8
```

앞으로 Android UI/동작을 결정할 때 이 영상을 원작 플레이 감각의 기준으로 사용합니다.

특히 다음 요소를 우선적으로 원작에 맞춥니다.

- 게임 플레이를 가리는 Android식 확인 팝업 최소화
- 스테이지 클리어 후 사용자 확인 없이 자동 다음 스테이지 진행
- 스테이지 전환 템포와 페이드
- 캐릭터/박스의 성공 반응 애니메이션
- 원본 픽셀 그래픽 비율과 배치
- 불필요한 모바일 UI보다 게임 화면 자체를 우선
- 터치 조작부는 스마트폰용으로 추가하되 원작 게임 영역을 방해하지 않음

SWF ActionScript의 `Stage_Clear()` / `stageFade_chk()`를 대조한 결과,
원본은 `stage_map._alpha`를 빠르게 낮춘 뒤 현재 프레임 + 1을 자동 로드합니다.
따라서 Android 앱도 클리어 팝업 없이 약 120ms의 짧은 페이드 후 다음 스테이지로 자동 진행합니다.

66스테이지 이후에는 원본 `stage_map` frame 67의 엔딩 화면으로 자동 진행합니다.

엔딩 재현 요소:

- 원본 frame 67의 벽/박스/플레이어 배치
- 중앙 Shape 371의 노랑→빨강 그라데이션 형태
- `text_ending` Sprite 388의 9개 크레딧 조합
- 원본 ClipAction처럼 약 2초 페이드 인 + 2초 페이드 아웃
- `Flash PUSH II v0.95` 하단 표기
- RETRY 또는 STAGE 선택 시 정상적으로 게임 화면으로 복귀

## Android 버전 호환 정책

호환성 기준은 "Galaxy S8부터 이후 Android 버전까지"입니다.

- 최소 실행 버전은 Galaxy S8의 최초 OS인 Android 7.0(API 24)로 고정
- 최신 Android 동작 검증을 위해 targetSdk는 Android 16(API 36)
- compileSdk도 36을 사용
- maxSdkVersion은 지정하지 않아 이후 Android 버전 설치를 제한하지 않음
- targetSdk를 올려도 minSdk는 독립적으로 유지하므로 S8 지원은 계속 유지
- 이후 Android 17 이상은 플랫폼의 전체 앱 대상 동작 변경을 확인하며 순차 검증

현재 Google Play의 2026년 신규 앱/업데이트 제출 기준도 Android 16(API 36) 이상입니다.

## 자동 빌드

GitHub Actions의:

```text
.github/workflows/android-ci.yml
```

에서 `main` push 및 PR 시 Debug APK 빌드를 수행합니다.

핵심 명령:

```text
./gradlew lintDebug assembleDebug
```

성공하면 `pushpush2-debug-apk` Artifact를 업로드합니다.

## 다음 우선순위

- [x] 원본 stage_map frame 67 엔딩 화면 이식
- [x] 박스 Sprite 356의 2~13프레임 그래픽 복원 및 애니메이션 적용
- [x] 플레이어 Sprite 370의 71~76 반응 애니메이션 적용
- [x] 박스 목표 진입 시 원본 타이밍으로 두 애니메이션 동기화
- [ ] Galaxy S8 실제 화면에서 D-pad 높이/보드 영역 확인
- [ ] Galaxy S10 실제 화면에서 D-pad 높이/보드 영역 확인
- [ ] 66개 스테이지 실제 플레이 검증
- [ ] Android Studio 실기기 테스트
- [ ] APK 릴리즈

## 개발 원칙

- Flash 런타임을 포함하지 않습니다.
- Android/Kotlin으로 재구현합니다.
- 원본 SWF 바이너리는 GitHub에 올리지 않습니다.
- 웹 ChatGPT가 GitHub MCP를 통해 저장소를 수정합니다.
- Windows 11에서 `git pull` 후 Android Studio로 검증합니다.
