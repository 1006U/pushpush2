# 원본 SWF 분석 메모

분석 대상: 사용자가 제공한 `game.swf`

## 기본 메타데이터

- SWF 시그니처: `CWS` (zlib 압축)
- Flash/SWF 버전: **6**
- 압축 해제 크기: **308,309 bytes**
- 원본 무대 크기: **240 × 250 px**
- 프레임 속도: **10 fps**
- 메인 타임라인 프레임 수: **4**

## 원본 게임 스테이지 구조

원본 내부의 `stage_map`은 Sprite ID **390**으로 확인됐고 총 **68프레임**입니다.

- 1~66: 실제 퍼즐 스테이지
- 67: 엔딩 화면
- 68: 빈 프레임

각 스테이지에서 인스턴스 이름과 배치 좌표를 읽어 다음 요소를 복원했습니다.

- `brickN` → 벽
- `houseN` → 목표 지점
- `ballN` → 박스
- `charater` → 플레이어 시작 위치

배치 좌표는 **14 px 간격의 정규 격자**에 정확히 맞았습니다.
66개 스테이지 모두 박스 수와 목표 지점 수가 일치하고 플레이어 시작 위치가 존재합니다.

추출된 데이터는 `StageRepository.kt`에 반영했습니다.

## 확인된 사운드 심볼

- `success.wav`
- `start.wav`
- `move.wav`
- `clear.wav`
- `button.wav`

## 확인된 게임 관련 ActionScript 문자열

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

또한 ActionScript 로직에서 스테이지 번호가 67 이상일 경우 66으로 제한하는 동작을 확인하여,
실제 플레이 스테이지가 66개라는 점과 일치합니다.

## 현재 포팅 전략

SWF를 Android에서 직접 실행하지 않고 네이티브 Android 앱으로 재구현합니다.

1. 원본 스테이지 데이터 이식 ✅
2. Kotlin 이동/충돌/클리어 엔진 ✅
3. 스마트폰 터치 방향패드 ✅
4. 원본 그래픽 추출 및 적용
5. 원본 사운드 추출 및 적용
6. 실제 기기에서 원본 동작 비교

## 다음 분석 작업

- DefineShape / DefineSprite에서 벽, 목표, 박스, 캐릭터 그래픽 추출
- DefineSound에서 원본 사운드 추출
- 원본 캐릭터 애니메이션 방향/프레임 분석
- 원본 이동 사운드 및 클리어 연출 타이밍 비교
