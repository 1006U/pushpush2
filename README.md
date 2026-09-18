# PushPush 2 Android Port

Android 스마트폰에서 플레이할 수 있도록 기존 Flash(SWF) 기반 **푸시푸시**를 네이티브 Android 앱으로 재구현하는 프로젝트입니다.

## 목표

- 게임 화면 중심의 Android 포팅
- 스마트폰 터치 방향패드
- 박스 밀기(Sokoban) 규칙 재현
- 스테이지 재시작
- 클리어 진행상황 저장
- 해금된 스테이지 선택
- 원본 그래픽·사운드·스테이지를 단계적으로 이식
- 최종 APK 빌드

## 현재 구현

현재 초기 프로토타입에는 다음이 들어 있습니다.

- 순수 Kotlin 이동/박스 밀기 엔진
- 벽 충돌 / 목표 판정
- Canvas 기반 임시 게임 화면
- 터치 D-pad
- 스테이지 선택 UI
- 클리어 후 다음 스테이지 해금
- SharedPreferences 진행상황 저장
- 개발용 테스트 스테이지 3개

아직 원본 그래픽과 실제 스테이지는 적용하지 않았습니다.

## WSL2에서 받기

```bash
git clone https://github.com/1006U/pushpush2.git
cd pushpush2
```

Android Studio에서 이 폴더를 열어 Gradle Sync 후 실행하면 됩니다.

CLI에서 Android SDK/JDK가 준비되어 있다면:

```bash
./gradlew assembleDebug
```

APK는 성공 시 다음 경로에 생성됩니다.

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 원본 SWF 사용

원본 `game.swf`는 저장소에 올리지 않고 로컬 참조용으로 사용합니다.

```text
pushpush2/
└── original/
    └── game.swf
```

`.gitignore`에서 `original/*`를 제외하도록 설정했습니다.

현재 업로드된 원본 SWF에서 확인한 정보는 `docs/ORIGINAL_SWF_NOTES.md`에 기록했습니다.

## 개발 단계

1. 기본 Android 프로젝트 + Sokoban 엔진 ✅
2. 터치 방향패드 / 재시작 / 스테이지 선택 ✅
3. 클리어 진행상황 저장 ✅
4. 원본 SWF의 스테이지 데이터 분석·이식
5. 원본 이미지/사운드 적용
6. 원본 240×250 화면 비율과 조작감 조정
7. 실기기 테스트 및 APK 릴리즈

현재 진행상황은 `PROJECT_STATUS.md`를 참고하세요.
