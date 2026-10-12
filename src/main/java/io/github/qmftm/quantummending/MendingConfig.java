package io.github.qmftm.quantummending;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * config.yml 설정값.
 *
 * @param maxLevel       수선 최대 레벨
 * @param repairBase     레벨 I 의 경험치 1당 수리량
 * @param repairPerLevel 레벨이 1 오를 때마다 늘어나는 수리량
 * @param anvilCost      모루 비용 계수
 * @param lootMaxLevel   구조물 상자·주민 거래·낚시에서 나오는 최대 레벨
 * @param tableEnabled   인챈트 테이블에서 수선이 뜨는지 여부
 * @param tableWeight    인챈트 테이블 가중치
 * @param tableMaxLevel  인챈트 테이블에서 나오는 최대 레벨
 */
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

    // 마인크래프트 인챈트 레벨의 최대값
    static final int MAX_ENCHANTMENT_LEVEL = 255;

    /**
     * 데이터 폴더의 config.yml 을 읽습니다. 파일이 없으면 jar 안의 기본 config.yml 을 복사합니다.
     * 범위를 벗어난 값은 허용 범위로 보정합니다.
     */
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

    /**
     * 서버 재시작이 있어야 적용되는 항목 중, other 와 값이 다른 설정 키 목록.
     * (인챈트 정의는 서버 시작 시에만 읽히기 때문)
     */
    List<String> restartRequiredChanges(final MendingConfig other) {
        final List<String> changed = new ArrayList<>();
        if (this.maxLevel != other.maxLevel) {
            changed.add("max-level");
        }
        if (this.repairBase != other.repairBase || this.repairPerLevel != other.repairPerLevel) {
            changed.add("repair-per-xp");
        }
        if (this.anvilCost != other.anvilCost) {
            changed.add("anvil-cost");
        }
        if (this.tableWeight != other.tableWeight) {
            changed.add("enchanting-table.weight");
        }
        // 테이블 on/off·최대 레벨은 인챈트 비용도 바꾸므로, 비용이 달라졌다면 재시작이 필요합니다.
        if (changed.isEmpty() && !MendingPack.mendingJson(this).equals(MendingPack.mendingJson(other))) {
            changed.add("enchanting-table (레벨 분포)");
        }
        return changed;
    }

    /** 해당 레벨의 경험치 1당 수리량 */
    double repairAt(final int level) {
        return this.repairBase + this.repairPerLevel * (level - 1);
    }

    /** 로그용: 레벨별 수리량 (최대 10레벨까지만 표시) */
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

    /** 로그용: 전체 설정 요약 */
    String describe() {
        return String.format(Locale.ROOT,
            "max level %d, anvil cost %d, loot max level %d, enchanting table %s, repair per XP: %s",
            this.maxLevel, this.anvilCost, this.lootMaxLevel,
            this.tableEnabled ? "on (weight " + this.tableWeight + ", max level " + this.tableMaxLevel + ")" : "off",
            this.describeLevels());
    }
}
