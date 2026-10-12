package io.github.qmftm.quantummending;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Writes a datapack overriding the vanilla mending enchantment definition.
 * Everything except {@code max_level}, {@code anvil_cost} and the {@code repair_with_xp} factor matches vanilla 26.x.
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
            "base": 75,
            "per_level_above_first": 25
          },
          "max_level": %d,
          "min_cost": {
            "base": 25,
            "per_level_above_first": 25
          },
          "slots": [
            "any"
          ],
          "supported_items": "#minecraft:enchantable/durability",
          "weight": 2
        }
        """;

    private MendingPack() {
    }

    static Path write(final Path root, final MendingConfig config) throws IOException {
        final Path enchantmentDir = root.resolve("data/minecraft/enchantment");
        Files.createDirectories(enchantmentDir);
        Files.writeString(root.resolve("pack.mcmeta"), PACK_MCMETA.formatted(MIN_FORMAT, MAX_FORMAT));
        Files.writeString(enchantmentDir.resolve("mending.json"), String.format(Locale.ROOT, MENDING_JSON, config.anvilCost(),
            config.repairBase(), config.repairPerLevel(), config.maxLevel()));
        return root;
    }
}
