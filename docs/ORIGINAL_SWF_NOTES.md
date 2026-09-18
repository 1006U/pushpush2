# 원본 SWF 분석 메모

분석 대상: 사용자가 제공한 `game.swf`

## 기본 메타데이터

- SWF 시그니처: `CWS` (zlib 압축)
- Flash/SWF 버전: **6**
- 압축 해제 크기: **308,309 bytes**
- 원본 무대 크기: **240 × 250 px**
- 프레임 속도: **10 fps**
- 메인 타임라인 프레임 수: **4**

## 스테이지 구조

원본 내부의 `stage_map`은 Sprite ID **390**이며 총 **68프레임**입니다.

- 1~66: 실제 퍼즐 스테이지
- 67: 엔딩 화면
- 68: 빈 프레임

각 스테이지의 인스턴스 이름과 위치를 읽어 다음 요소를 복원했습니다.

- `brickN` → 벽
- `houseN` → 목표 지점
- `ballN` → 박스
- `charater` → 플레이어 시작 위치

좌표는 **14 px 간격의 정규 격자**에 맞아 있습니다.

66개 스테이지 모두:

- 플레이어 시작 위치 존재
- 박스 수 = 목표 수

조건을 만족했습니다.

추출된 데이터는:

```text
app/src/main/java/com/pushpush2/game/StageRepository.kt
```

에 반영되어 있습니다.

## 그래픽 구조

스테이지에서 사용되는 주요 Sprite ID:

- 벽: Sprite **346**
- 목표: Sprite **349**
- 박스: Sprite **356**
- 플레이어: Sprite **370**

기본 그래픽의 실제 Shape:

- 벽 기본 Shape: **345**
- 목표 기본 Shape: **348**
- 박스 기본 Shape: **350**
- 플레이어 기본 Shape: **357**

이 4개 기본 Shape의 경계는 모두:

```text
0 ~ 280 twips
```

즉 실제 크기로:

```text
14 × 14 px
```

입니다.

SWF의 벡터 Shape 데이터를 직접 파싱해 PNG로 변환했고,
다음 Android 리소스로 추가했습니다.

```text
app/src/main/res/drawable-nodpi/
├── tile_brick.png
├── tile_goal.png
├── tile_box.png
└── tile_player.png
```

`GameView.kt`는 이 PNG를 픽셀 필터링 없이 확대하여 렌더링합니다.

## 플레이어 애니메이션 구조

플레이어 Sprite **370**은 총 **76프레임**입니다.

기본 프레임에서 사용하는 Shape ID:

- 357
- 358
- 359

후반부 프레임에서는 추가 Shape / MorphShape가 사용됩니다.

현재 앱에는 우선 기본 Shape **357**만 적용했습니다.

다음 단계에서 ActionScript의 `gotoAndStop` / 방향 입력 로직과
Sprite 370의 프레임 관계를 분석하여 방향별 프레임을 정확히 매핑할 예정입니다.

## 박스 애니메이션 구조

박스 Sprite **356**은 **13프레임**입니다.

기본 프레임 Shape는 **350**이며,
후속 프레임에는 351~355 Shape / MorphShape가 사용됩니다.

현재 앱은 기본 프레임만 사용합니다.

## 원본 사운드

ExportAssets에서 다음 사운드 심볼을 확인했습니다.

- ID 1: `success.wav`
- ID 2: `start.wav`
- ID 3: `move.wav`
- ID 4: `clear.wav`
- ID 5: `button.wav`

실제 DefineSound 포맷은 모두:

```text
SoundFormat = 2 (MP3)
```

입니다.

MP3 스트림을 직접 추출하여 재생 가능 여부를 확인했습니다.

대략적인 길이:

- success: **0.78초**
- start: **11.54초**
- move: **0.57초**
- clear: **8.27초**
- button: **0.31초**

## 확인된 ActionScript 문자열

- `Choice_stage`
- `Stage_Clear`
- `Stage_early`
- `stage_map`
- `stage_number`
- `stage_password`
- `keyListener`
- `myOnKeyDown`
- `myOnKeyUp`
- `ballMove_chk`
- `unitMove_chk`
- `house_chk`
- `brick_unit`
- `ball_unit`

ActionScript 로직에서 스테이지 번호가 67 이상일 경우 66으로 제한하는 동작도 확인했습니다.

## 현재 포팅 전략

SWF를 Android에서 직접 실행하지 않습니다.

대신:

1. 원본 66개 스테이지 이식 ✅
2. Kotlin 이동 / 충돌 / 클리어 엔진 ✅
3. 스마트폰 터치 방향패드 ✅
4. 벽 / 목표 / 박스 / 플레이어 기본 그래픽 적용 ✅
5. 방향별 캐릭터 애니메이션 분석
6. 원본 사운드 Android 연결
7. 원본 240×250 화면 레이아웃 재현
8. 실기기 비교 테스트
