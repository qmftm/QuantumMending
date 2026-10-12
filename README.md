# QuantumMending
Level-based repair enchantment

수선(Mending)에 레벨을 추가하는 Paper 플러그인입니다. 레벨이 높을수록 경험치 1당 회복하는 내구도가 늘어납니다.

| 레벨 | 경험치 1당 수리량 (기본 설정) |
|------|------------------------------|
| I    | 2 (바닐라와 동일)             |
| II   | 4                            |
| III  | 6                            |
| IV   | 8                            |
| V    | 10                           |

## 지원 버전
- Paper 26.1 ~ 26.3 (Java 25)

## 설치
1. `./gradlew build`
2. `build/libs/QuantumMending-<버전>.jar` 를 서버의 `plugins/` 폴더에 넣고 서버를 시작합니다.

## 설정 (`plugins/QuantumMending/config.yml`)
```yaml
max-level: 5          # 수선 최대 레벨 (1 ~ 255)
repair-per-xp:
  base: 2.0           # I 레벨의 경험치당 수리량
  per-level: 2.0      # 레벨이 1 오를 때마다 늘어나는 수리량
```
변경 후 서버를 재시작하거나 `/minecraft:reload` 를 실행하면 적용됩니다.

## 동작 방식
플러그인이 설정값으로 `minecraft:mending` 인챈트 정의를 덮어쓰는 데이터팩을 생성해 등록합니다
(`plugins/QuantumMending/datapack`). 수리량·경험치 소모 계산은 바닐라 로직이 그대로 처리하므로,
모루 합성(수선 I + 수선 I → 수선 II), 주민 거래, 전리품의 레벨 등도 최대 레벨까지 자연스럽게 적용됩니다.
