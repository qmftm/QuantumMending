package io.github.qmftm.quantummending;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;

record MendingConfig(int maxLevel, double repairBase, double repairPerLevel) {

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
        return new MendingConfig(maxLevel, base, perLevel);
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
}
