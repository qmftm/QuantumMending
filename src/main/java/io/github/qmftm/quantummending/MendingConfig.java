package io.github.qmftm.quantummending;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;

record MendingConfig(
    int maxLevel,
    double repairBase,
    double repairPerLevel,
    int anvilCost,
    int lootMaxLevel,
    boolean tableEnabled,
    int tableWeight,
    int tableMaxLevel
) {

    static final int MAX_ENCHANTMENT_LEVEL = 255;

    static MendingConfig load(final Path dataDirectory) throws IOException {
        final Path file = dataDirectory.resolve("config.yml");
        if (Files.notExists(file)) {
            Files.createDirectories(dataDirectory);
            try (InputStream in = Objects.requireNonNull(MendingConfig.class.getResourceAsStream("/config.yml"))) {
                Files.copy(in, file);
            }
        }

        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        final int maxLevel = Math.clamp(yaml.getInt("max-level", 5), 1, MAX_ENCHANTMENT_LEVEL);
        final double base = Math.max(0.0, yaml.getDouble("repair-per-xp.base", 2.0));
        final double perLevel = Math.max(0.0, yaml.getDouble("repair-per-xp.per-level", 2.0));
        final int anvilCost = Math.max(0, yaml.getInt("anvil-cost", 4));
        final int lootMaxLevel = Math.clamp(yaml.getInt("loot.max-level", 1), 1, maxLevel);
        final boolean tableEnabled = yaml.getBoolean("enchanting-table.enabled", false);
        final int tableWeight = Math.clamp(yaml.getInt("enchanting-table.weight", 2), 1, 1024);
        final int tableMaxLevel = Math.clamp(yaml.getInt("enchanting-table.max-level", 1), 1, maxLevel);
        return new MendingConfig(maxLevel, base, perLevel, anvilCost, lootMaxLevel, tableEnabled, tableWeight, tableMaxLevel);
    }

    double repairAt(final int level) {
        return this.repairBase + this.repairPerLevel * (level - 1);
    }

    String describeLevels() {
        final StringJoiner joiner = new StringJoiner(", ");
        for (int level = 1; level <= Math.min(this.maxLevel, 10); level++) {
            joiner.add(String.format(Locale.ROOT, "%d=%s", level, this.repairAt(level)));
        }
        if (this.maxLevel > 10) {
            joiner.add("...");
        }
        return joiner.toString();
    }

    String describe() {
        return String.format(Locale.ROOT,
            "max level %d, anvil cost %d, loot max level %d, enchanting table %s, repair per XP: %s",
            this.maxLevel, this.anvilCost, this.lootMaxLevel,
            this.tableEnabled ? "on (weight " + this.tableWeight + ", max level " + this.tableMaxLevel + ")" : "off",
            this.describeLevels());
    }
}
