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
1. `mvn package` (JDK 25 필요)
2. `target/QuantumMending-<버전>.jar` 를 서버의 `plugins/` 폴더에 넣고 서버를 시작합니다.

## 설정 (`plugins/QuantumMending/config.yml`)
```yaml
max-level: 5          # 수선 최대 레벨 (1 ~ 255)
repair-per-xp:
  base: 2.0           # I 레벨의 경험치당 수리량
  per-level: 2.0      # 레벨이 1 오를 때마다 늘어나는 수리량
anvil-cost: 4         # 모루 비용 계수 (바닐라 4, 책으로 붙이면 절반)
loot:
  max-level: 1        # 구조물 상자·주민 거래·낚시에서 나오는 최대 레벨
enchanting-table:
  enabled: false      # 인챈트 테이블에서 수선이 뜨는지
  weight: 2           # 뜰 확률 가중치 (1 ~ 1024)
  max-level: 1        # 인챈트 테이블에서 나오는 최대 레벨
```
## 명령어
| 명령어 | 권한 | 설명 |
|--------|------|------|
| `/quantummending reload` (`/qm reload`) | `quantummending.reload` (기본 OP) | config.yml 을 다시 불러옵니다 |

리로드로 바로 적용되는 설정: `loot.max-level`, `enchanting-table.enabled`, `enchanting-table.max-level`(상한)

서버를 **재시작해야** 적용되는 설정: `max-level`, `repair-per-xp`, `anvil-cost`, `enchanting-table.weight`
(인챈트 정의는 마인크래프트가 서버 시작 시에만 읽기 때문입니다. 리로드 시 재시작이 필요한 항목을 알려줍니다.)

## 모루 합성
같은 레벨끼리 합치면 한 단계 올라갑니다 (I + I → II, II + II → III, … `max-level` 까지).
인챈트 비용은 `anvil-cost × 결과 레벨` 이며 책으로 붙이면 절반이고, 여기에 바닐라 작업 패널티가 더해집니다.

## 구조물·주민 거래·낚시
`loot.max-level` 을 넘는 레벨이 나오면 1 ~ `loot.max-level` 사이에서 다시 뽑습니다.
상자 책·사서 거래는 원래 레벨을 균등하게 뽑으므로, 예를 들어 2로 두면 I과 II가 반반씩 나옵니다.

## 인챈트 테이블
`enchanting-table.enabled: true` 이면 수선이 인챈트 테이블 후보에 추가되고,
레벨 30 테이블에서 `enchanting-table.max-level` 까지 나오도록 비용이 조정됩니다.
테이블 레벨이 낮으면 낮은 레벨이 나옵니다.

`weight` 는 다른 인챈트와의 상대 가중치라 아이템마다 실제 확률이 다릅니다.
아래는 Paper 26.3에서 아이템당 20,000번 인챈트해 측정한, 결과에 수선이 붙을 확률입니다.

| weight | 책 (30) | 다이아 곡괭이 (30) | 다이아 흉갑 (30) | 철 검 (30) | 다이아 곡괭이 (15) |
|--------|---------|--------------------|------------------|------------|--------------------|
| 1      | 1.5%    | 18%                | 23%              | 8%         | 10%                |
| 2      | 3.0%    | 28%                | 32%              | 15%        | 18%                |
| 5      | 7.2%    | 47%                | 50%              | 30%        | 34%                |
| 10     | 14%     | 63%                | 63%              | 46%        | 50%                |

참고로 바닐라 가중치는 보호 10, 효율 10, 내구성 5, 섬세한 손길 1 입니다.

## 동작 방식
플러그인이 설정값으로 `minecraft:mending` 인챈트 정의를 덮어쓰는 데이터팩을 생성해 등록합니다
(`plugins/QuantumMending/datapack`). 수리량·경험치 소모 계산은 바닐라 로직이 그대로 처리하므로,
모루 합성(수선 I + 수선 I → 수선 II)도 최대 레벨까지 자연스럽게 적용됩니다.
상자·거래·낚시·인챈트 테이블의 레벨 상한은 이벤트로 처리합니다.
