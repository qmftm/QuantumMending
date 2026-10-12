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
 * 플러그인 부트스트랩 (서버 시작 초기, 월드·데이터팩 로드 전에 실행됨).
 * <p>
 * config.yml 값으로 {@code minecraft:mending} 을 덮어쓰는 데이터팩을 만들어 서버에 등록합니다.
 */
public final class QuantumMendingBootstrap implements PluginBootstrap {

    private final ConfigHolder config = new ConfigHolder();

    @Override
    public void bootstrap(final BootstrapContext context) {
        // 서버 시작 시와 리로드(/qm reload, /minecraft:reload) 때마다 실행됩니다.
        // 단, 인챈트 정의(최대 레벨·수리량·모루 비용·가중치)는 서버 시작 시에만 읽히므로
        // 리로드로는 태그(인챈트 테이블 등장 여부)와 레벨 상한 설정만 바로 바뀝니다.
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {
            try {
                final MendingConfig config = MendingConfig.load(context.getDataDirectory());
                final Path pack = MendingPack.write(context.getDataDirectory().resolve("datapack"), config);
                // 바닐라 데이터보다 나중에(위에) 로드되어야 덮어쓸 수 있으므로 TOP 에 고정합니다.
                event.registrar().discoverPack(pack, "mending", configurer -> configurer
                    .title(Component.text("QuantumMending"))
                    .autoEnableOnServerStart(true)
                    .position(true, Datapack.Position.TOP));
                this.config.update(config);
                context.getLogger().info("Mending {}", config.describe());
            } catch (final IOException e) {
                throw new UncheckedIOException("Failed to set up QuantumMending datapack", e);
            }
        });
    }

    // 플러그인 본체와 설정 보관함을 공유합니다.
    @Override
    public JavaPlugin createPlugin(final PluginProviderContext context) {
        return new QuantumMendingPlugin(this.config);
    }
}
