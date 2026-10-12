package io.github.qmftm.quantummending;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * 플러그인 본체. 수리량·최대 레벨 등은 데이터팩이 처리하고,
 * 여기서는 상자·거래·낚시·인챈트 테이블의 레벨 상한을 거는 리스너와 리로드 명령어를 등록합니다.
 */
public final class QuantumMendingPlugin extends JavaPlugin {

    static final String RELOAD_PERMISSION = "quantummending.reload";

    private final ConfigHolder config;

    QuantumMendingPlugin(final ConfigHolder config) {
        this.config = config;
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new LevelLimitListener(this.config), this);

        // /quantummending reload (별칭 /qm reload)
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(
            Commands.literal("quantummending")
                .requires(source -> source.getSender().hasPermission(RELOAD_PERMISSION))
                .then(Commands.literal("reload").executes(context -> {
                    this.reload(context.getSource().getSender());
                    return Command.SINGLE_SUCCESS;
                }))
                .build(),
            "QuantumMending 명령어",
            List.of("qm")));
    }

    /**
     * 서버 데이터를 리로드합니다. 리로드 중에 부트스트랩의 데이터팩 등록 단계가 다시 실행되면서
     * config.yml 을 다시 읽고 데이터팩을 새로 만듭니다.
     */
    private void reload(final CommandSender sender) {
        sender.sendMessage(Component.text("QuantumMending 설정을 리로드하는 중...", NamedTextColor.GRAY));
        this.getServer().reloadData();

        final MendingConfig current = this.config.current();
        sender.sendMessage(Component.text("리로드 완료: " + current.describe(), NamedTextColor.GREEN));

        // 인챈트 정의가 바뀌는 설정은 서버 시작 시에만 반영되므로 안내합니다.
        final List<String> restart = this.config.startup().restartRequiredChanges(current);
        if (!restart.isEmpty()) {
            sender.sendMessage(Component.text(
                "다음 설정은 서버를 재시작해야 적용됩니다: " + String.join(", ", restart), NamedTextColor.YELLOW));
        }
    }
}
