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

## 플레이어 Sprite 370 분석 결과

플레이어 Sprite **370**은 총 **76프레임**입니다.

기존에 "방향별 애니메이션"일 가능성을 우선순위로 두었지만,
원본 SWF를 다시 분석한 결과 **1~70프레임은 방향 애니메이션이 아닙니다.**

### 1~70프레임: 대기/눈 깜빡임

사용 Shape:

- Shape 357: 기본 눈
- Shape 358: 중간 눈
- Shape 359: 감긴 눈

10fps 타임라인에서 Shape가 바뀌는 프레임:

- Frame 1 → 357
- Frame 2 → 358
- Frame 3 → 359
- Frame 4~11 → 357
- Frame 12 → 358
- Frame 13 → 359
- Frame 14~37 → 357
- Frame 38 → 358
- Frame 39 → 359
- Frame 40~70 → 357

Frame 70의 ActionScript:

- `GotoFrame(0)`
- `Play`

즉 1~70을 반복하는 원본 대기 애니메이션입니다.

현재 Android 앱은 이 70프레임 루프를 원본과 동일한 **10fps**로 재현합니다.

Shape 358/359는 원본 벡터 Shape를 직접 복원해 사용합니다.

### 71~76프레임: 반응 애니메이션

Frame 71부터 Shape 360~369 및 MorphShape가 사용됩니다.

원본 ActionScript에서 특정 성공 동작 시:

- 대상 박스에 `gotoAndPlay(2)`
- 플레이어 `charater.gotoAndPlay(71)`

호출이 확인됐습니다.

따라서 71~76은 이동 방향 표시가 아니라,
**박스가 목표에 들어갔을 때의 플레이어 반응 애니메이션**으로 보는 것이 원본 동작과 맞습니다.

이 구간은 Android 앱에 원본 10fps 타이밍으로 적용했습니다.

- frame 71~76: 100ms 간격으로 재생
- frame 76 이후: 평상시 frame 1~70 대기 루프로 복귀
- 박스 Sprite 356 성공 애니메이션과 동시에 시작

## 박스 Sprite 356 분석 결과

박스 Sprite **356**은 **13프레임**입니다.

Frame별 주요 구성:

- 1: Shape 350
- 2: Shape 351 + MorphShape 352 + Shape 353
- 3~7: MorphShape 352 ratio 변화
- 8~11: MorphShape 354 ratio 변화
- 12: Shape 355 사용
- 13: Shape 351 + Shape 353

Frame 1과 Frame 13에는 `Stop` Action이 있습니다.

원본 ActionScript에서 성공 동작 시 해당 박스에:

```text
gotoAndPlay(2)
```

가 호출됩니다.

따라서 2~13은 목표 진입 시 재생되는 박스 반응 애니메이션입니다.

Android 앱에는 원본 Shape/MorphShape를 14×14 PNG 프레임으로 직접 복원해 적용했습니다.

- frame 2~13을 10fps로 재생
- frame 13의 Stop 동작을 반영해 목표 위 박스는 마지막 완료 프레임 유지
- 박스가 목표 밖으로 이동하면 기본 frame 1로 복귀
- 여러 박스가 연속으로 목표에 들어가도 각 박스의 애니메이션 시작 시간을 개별 관리

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

대략적인 길이:

- success: **0.78초**
- start: **11.54초**
- move: **0.57초**
- clear: **8.27초**
- button: **0.31초**

### start 사용 시점

초기화 코드에서:

```text
start_snd.start(0)
```

호출을 확인했습니다.

Android 앱에서는 Activity 최초 생성 시 `start` 사운드를 재생하도록 연결했습니다.

### success 사용 시점

박스를 목표 지점에 밀어 넣는 성공 로직에서:

- 성공 상태 설정
- 성공 사운드 재생
- 해당 박스 애니메이션 시작
- 플레이어 반응 애니메이션 시작

이 이어지는 구조를 확인했습니다.

Android 앱에서는 우선 박스가 이동 후 새 위치에서 목표 지점에 들어간 경우
`success` 사운드를 재생하도록 연결했습니다.

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
5. 플레이어 1~70 대기/눈 깜빡임 적용 ✅
6. start / success 사운드 사용 시점 반영 ✅
7. 박스 2~13 목표 진입 애니메이션 ✅
8. 플레이어 71~76 성공 반응 애니메이션 ✅
9. Galaxy S8 / S10 실기기 UI 미세 조정
10. 66개 스테이지 실제 플레이 검증
