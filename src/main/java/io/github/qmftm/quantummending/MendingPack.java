package io.github.qmftm.quantummending;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * 바닐라 수선 인챈트 정의를 덮어쓰는 데이터팩을 생성합니다.
 * {@code max_level}, {@code anvil_cost}, {@code weight}, 비용(min/max_cost),
 * {@code repair_with_xp} 배율을 제외한 나머지는 바닐라 26.x 와 동일합니다.
 * <p>
 * 수리량은 {@code repair_with_xp} 효과의 배율을 레벨에 비례(linear)하게 바꿔서 구현합니다.
 * 경험치 소모 계산까지 바닐라가 그대로 처리하므로 별도 이벤트 처리가 필요 없습니다.
 * <p>
 * 인챈트 테이블이 켜져 있으면 수선을 {@code #minecraft:in_enchanting_table} 태그에 추가하고,
 * 테이블 최대 레벨까지 나올 수 있도록 비용을 낮춥니다
 * (30레벨 테이블의 인챈트 파워는 대략 26~50).
 */
final class MendingPack {

    // 데이터팩 포맷 범위: 26.1 = 101, 26.3 = 121
    private static final int MIN_FORMAT = 101;
    private static final int MAX_FORMAT = 121;

    // 데이터팩 메타 정보
    private static final String PACK_MCMETA = """
        {
          "pack": {
            "description": "QuantumMending: level-based mending",
            "min_format": %d,
            "max_format": %d
          }
        }
        """;

    // 수선 인챈트 정의 (data/minecraft/enchantment/mending.json). %s/%d 자리에 설정값이 들어갑니다.
    private static final String MENDING_JSON = """
        {
          "anvil_cost": %d,
          "description": {
            "translate": "enchantment.minecraft.mending"
          },
          "effects": {
            "minecraft:repair_with_xp": [
              {
                "effect": {
                  "type": "minecraft:multiply",
                  "factor": {
                    "type": "minecraft:linear",
                    "base": %s,
                    "per_level_above_first": %s
                  }
                }
              }
            ]
          },
          "max_cost": {
            "base": %d,
            "per_level_above_first": %d
          },
          "max_level": %d,
          "min_cost": {
            "base": %d,
            "per_level_above_first": %d
          },
          "slots": [
            "any"
          ],
          "supported_items": "#minecraft:enchantable/durability",
          "weight": %d
        }
        """;

    // 인챈트 테이블 후보 태그. replace 가 없으므로 바닐라 목록에 수선이 "추가"됩니다.
    private static final String IN_ENCHANTING_TABLE_TAG = """
        {
          "values": [
            "minecraft:mending"
          ]
        }
        """;

    // 바닐라 비용: 최소 25 + 레벨당 25, 최대 75 + 레벨당 25
    private static final int VANILLA_MIN_COST = 25;
    private static final int VANILLA_COST_PER_LEVEL = 25;
    private static final int VANILLA_MAX_COST = 75;

    // 테이블용 비용: 파워 10 이상이면 I, 파워 30 이상이면 테이블 최대 레벨이 나오도록 레벨 간격을 나눔.
    // 인챈트는 '최소 비용 <= 파워 <= 최대 비용' 인 가장 높은 레벨로 뽑히므로 최대 비용은 넉넉하게 잡습니다.
    private static final int TABLE_MIN_COST = 10;
    private static final int TABLE_TOP_COST = 30;
    private static final int TABLE_COST_WINDOW = 50;

    private MendingPack() {
    }

    /**
     * 설정으로 수선 인챈트 정의 JSON 을 만듭니다.
     * 두 설정의 결과가 다르면 인챈트 정의가 바뀐 것이므로 서버 재시작이 필요합니다.
     */
    static String mendingJson(final MendingConfig config) {
        // 인챈트 테이블 사용 여부에 따라 비용을 정합니다.
        final int minCost;
        final int maxCost;
        final int costPerLevel;
        if (config.tableEnabled()) {
            minCost = TABLE_MIN_COST;
            maxCost = TABLE_MIN_COST + TABLE_COST_WINDOW;
            costPerLevel = config.tableMaxLevel() > 1
                ? Math.max(1, (TABLE_TOP_COST - TABLE_MIN_COST) / (config.tableMaxLevel() - 1))
                : VANILLA_COST_PER_LEVEL;
        } else {
            minCost = VANILLA_MIN_COST;
            maxCost = VANILLA_MAX_COST;
            costPerLevel = VANILLA_COST_PER_LEVEL;
        }
        return String.format(Locale.ROOT, MENDING_JSON, config.anvilCost(),
            config.repairBase(), config.repairPerLevel(), maxCost, costPerLevel, config.maxLevel(),
            minCost, costPerLevel, config.tableWeight());
    }

    /**
     * 데이터팩 파일들을 root 아래에 (덮어)씁니다.
     *
     * @return 데이터팩 루트 경로
     */
    static Path write(final Path root, final MendingConfig config) throws IOException {
        final Path enchantmentDir = root.resolve("data/minecraft/enchantment");
        Files.createDirectories(enchantmentDir);
        Files.writeString(root.resolve("pack.mcmeta"), PACK_MCMETA.formatted(MIN_FORMAT, MAX_FORMAT));
        Files.writeString(enchantmentDir.resolve("mending.json"), mendingJson(config));

        // 테이블을 끄면 이전에 만들어 둔 태그 파일을 지워서 후보에서 빠지게 합니다.
        final Path tag = root.resolve("data/minecraft/tags/enchantment/in_enchanting_table.json");
        if (config.tableEnabled()) {
            Files.createDirectories(tag.getParent());
            Files.writeString(tag, IN_ENCHANTING_TABLE_TAG);
        } else {
            Files.deleteIfExists(tag);
        }
        return root;
    }
}
