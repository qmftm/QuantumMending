package io.github.qmftm.quantummending;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Supplier;

/**
 * 플러그인 본체. 수리량·최대 레벨 등은 데이터팩이 처리하고,
 * 여기서는 상자·거래·낚시·인챈트 테이블의 레벨 상한을 거는 리스너만 등록합니다.
 */
public final class QuantumMendingPlugin extends JavaPlugin {

    private final Supplier<MendingConfig> config;

    QuantumMendingPlugin(final Supplier<MendingConfig> config) {
        this.config = config;
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new LevelLimitListener(this.config), this);
    }
}
