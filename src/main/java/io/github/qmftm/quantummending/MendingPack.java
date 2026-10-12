package io.github.qmftm.quantummending;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Writes a datapack overriding the vanilla mending enchantment definition.
 * Everything except {@code max_level}, {@code anvil_cost}, {@code weight}, the costs and the
 * {@code repair_with_xp} factor matches vanilla 26.x.
 * <p>
 * When the enchanting table is enabled, mending is added to {@code #minecraft:in_enchanting_table}
 * and its costs are lowered so that every level up to the table max level is reachable
 * (a level 30 table rolls roughly 26-50 enchantment power).
 */
final class MendingPack {

    // Data pack formats: 26.1 = 101, 26.3 = 121
    private static final int MIN_FORMAT = 101;
    private static final int MAX_FORMAT = 121;

    private static final String PACK_MCMETA = """
        {
          "pack": {
            "description": "QuantumMending: level-based mending",
            "min_format": %d,
            "max_format": %d
          }
        }
        """;

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

    private static final String IN_ENCHANTING_TABLE_TAG = """
        {
          "values": [
            "minecraft:mending"
          ]
        }
        """;

    // Vanilla costs: min 25 + 25/level, max 75 + 25/level
    private static final int VANILLA_MIN_COST = 25;
    private static final int VANILLA_COST_PER_LEVEL = 25;
    private static final int VANILLA_MAX_COST = 75;

    // Enchanting table costs: level 1 from power 10, table max level at power 30
    private static final int TABLE_MIN_COST = 10;
    private static final int TABLE_TOP_COST = 30;
    private static final int TABLE_COST_WINDOW = 50;

    private MendingPack() {
    }

    static Path write(final Path root, final MendingConfig config) throws IOException {
        final Path enchantmentDir = root.resolve("data/minecraft/enchantment");
        Files.createDirectories(enchantmentDir);
        Files.writeString(root.resolve("pack.mcmeta"), PACK_MCMETA.formatted(MIN_FORMAT, MAX_FORMAT));
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
        Files.writeString(enchantmentDir.resolve("mending.json"), String.format(Locale.ROOT, MENDING_JSON, config.anvilCost(),
            config.repairBase(), config.repairPerLevel(), maxCost, costPerLevel, config.maxLevel(),
            minCost, costPerLevel, config.tableWeight()));

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
