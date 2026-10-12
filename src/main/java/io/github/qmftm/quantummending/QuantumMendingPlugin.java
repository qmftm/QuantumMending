package io.github.qmftm.quantummending;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Supplier;

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
