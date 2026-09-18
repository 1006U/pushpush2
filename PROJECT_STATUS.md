# PROJECT STATUS

## 개발 환경

- Windows 11
- Android Studio
- GitHub
- 웹 ChatGPT + GitHub MCP
- WSL2 사용 안 함
- Docker 사용 안 함
- 로컬 MCP 서버 사용 안 함

## 완료

- [x] Android 앱 기본 구조
- [x] Kotlin 박스 밀기 엔진
- [x] 벽 충돌 / 박스 밀기 / 목표 판정
- [x] 터치 방향패드
- [x] 스테이지 재시작
- [x] 스테이지 선택 UI
- [x] 클리어 후 다음 스테이지 해금
- [x] 진행상황 저장
- [x] 원본 SWF 분석
- [x] 원본 스테이지 66개 추출
- [x] 원본 벽 / 목표 / 박스 / 플레이어 위치 이식
- [x] 원본 벽 그래픽 추출
- [x] 원본 목표 그래픽 추출
- [x] 원본 박스 그래픽 추출
- [x] 원본 플레이어 기본 그래픽 추출
- [x] 원본 14×14 그래픽을 Android 화면에 적용
- [x] 원본 5개 사운드 스트림 분석
- [x] Windows용 SWF 사운드 추출 스크립트 추가
- [x] 이동 / 클리어 / 버튼 사운드 재생 코드 연결
- [x] GitHub Actions Android CI 추가

## 현재 상태

원본 SWF의 `stage_map`:

- 프레임 1~66: 실제 스테이지
- 프레임 67: 엔딩
- 프레임 68: 빈 프레임

현재 앱에는 원본 66개 퍼즐 맵이 반영되어 있습니다.

원본 기본 그래픽도 적용되었습니다.

- 벽
- 목표
- 박스
- 플레이어 기본 프레임

플레이어 방향 애니메이션은 아직 미적용입니다.

영상 참고 UI 작업으로 기존 Android 기본 버튼형 D-pad를 제거하고, 하단 원형 4방향 패드와 STAGE/RETRY 소프트키를 추가했습니다.

## 사운드

원본 DefineSound 데이터:

- success: 약 0.78초
- start: 약 11.54초
- move: 약 0.57초
- clear: 약 8.27초
- button: 약 0.31초

`tools/extract_original_audio.py`를 실행하면
`original/game.swf`에서 Android raw 리소스로 추출됩니다.

앱 연결 상태:

- [x] move
- [x] clear
- [x] button
- [ ] success 사용 시점 확인
- [ ] start 사용 시점 확인

## 자동 빌드

GitHub Actions:

```text
.github/workflows/android-ci.yml
```

에서 `main` push 시 Debug APK 빌드를 수행합니다.

성공하면 `pushpush2-debug-apk` Artifact를 업로드합니다.

## 다음 우선순위

- [ ] 플레이어 Sprite 370 방향/프레임 매핑
- [ ] 방향별 이동 애니메이션 적용
- [ ] 박스 애니메이션 적용 여부 확인
- [ ] success / start 사운드 원본 사용 시점 분석
- [~] 원본/영상 참고 레트로 모바일 UI 적용 (게임 화면 + 원형 D-pad + STAGE/RETRY)
- [ ] 터치패드 길게 누르기 반복 입력
- [ ] 66개 스테이지 실제 플레이 검증
- [ ] Android Studio 실기기 테스트
- [ ] APK 릴리즈

## 개발 원칙

- Flash 런타임을 포함하지 않습니다.
- Android/Kotlin으로 재구현합니다.
- 원본 SWF 바이너리는 GitHub에 올리지 않습니다.
- 웹 ChatGPT가 GitHub MCP를 통해 저장소를 수정합니다.
- Windows 11에서 `git pull` 후 Android Studio로 검증합니다.
