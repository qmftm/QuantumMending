package io.github.qmftm.quantummending;

import io.papermc.paper.datapack.Datapack;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * Registers a generated datapack that overrides {@code minecraft:mending}
 * with a configurable max level and a level-scaled repair amount.
 */
public final class QuantumMendingBootstrap implements PluginBootstrap {

    private volatile MendingConfig config;

    @Override
    public void bootstrap(final BootstrapContext context) {
        // Runs on server start and on every /minecraft:reload, so config changes are picked up.
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {
            try {
                final MendingConfig config = MendingConfig.load(context.getDataDirectory());
                final Path pack = MendingPack.write(context.getDataDirectory().resolve("datapack"), config);
                event.registrar().discoverPack(pack, "mending", configurer -> configurer
                    .title(Component.text("QuantumMending"))
                    .autoEnableOnServerStart(true)
                    .position(true, Datapack.Position.TOP));
                this.config = config;
                context.getLogger().info("Mending {}", config.describe());
            } catch (final IOException e) {
                throw new UncheckedIOException("Failed to set up QuantumMending datapack", e);
            }
        });
    }

    @Override
    public JavaPlugin createPlugin(final PluginProviderContext context) {
        return new QuantumMendingPlugin(() -> this.config);
    }
}
