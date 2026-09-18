# 원본 SWF 분석 메모

분석 대상: 사용자가 제공한 `game.swf`

> 원본 SWF 바이너리는 공개 저장소에는 커밋하지 않고 로컬 참조 파일로 관리합니다.

## 확인된 기본 메타데이터

- SWF 시그니처: `CWS` (zlib 압축)
- Flash/SWF 버전: **6**
- 압축 해제 크기: 약 **308 KB**
- 원본 무대 크기: **240 × 250 px**
- 프레임 속도: **10 fps**
- 메인 타임라인 프레임 수: **4**

## 확인된 사운드 심볼

- `success.wav`
- `start.wav`
- `move.wav`
- `clear.wav`
- `button.wav`

## 확인된 게임 관련 문자열

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

원본에는 키보드 입력, 스테이지 선택, 박스/목표/벽 판정, 사운드 제어 로직이 포함된 것으로 확인됩니다.

## 포팅 전략

SWF를 Android에서 직접 실행하지 않고 게임을 네이티브로 재구현합니다.

1. 퍼즐 규칙을 순수 Kotlin 엔진으로 재구현
2. 스마트폰 터치 방향패드 연결
3. 실제 스테이지 데이터를 SWF에서 추출해 이식
4. 원본 이미지/사운드를 Android 리소스로 변환
5. 클리어 진행상황을 Android 로컬 저장소에 저장

## 다음 분석 작업

- DoAction(ActionScript 2 bytecode)에서 실제 스테이지 데이터 추적
- DefineSprite / PlaceObject 기반 그래픽 구조 분석
- 내장 사운드 추출
- 원본 스테이지 수 파악
- 원본 이동 및 클리어 규칙과 Kotlin 엔진 비교
