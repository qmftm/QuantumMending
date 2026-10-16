package io.github.qmftm.quantummending;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * GitHub 의 최신 릴리즈를 확인해서, 새 버전이 있으면 콘솔과 접속하는 OP 에게 알립니다.
 */
final class UpdateChecker implements Listener {

    static final String NOTIFY_PERMISSION = "quantummending.update";

    private static final String LATEST_RELEASE_API = "https://api.github.com/repos/qmftm/QuantumMending/releases/latest";

    private final JavaPlugin plugin;
    private final String currentVersion;
    // 새 버전이 있을 때만 값이 들어갑니다. (비동기로 채워지므로 volatile)
    private volatile String latestVersion;
    private volatile String latestUrl;

    UpdateChecker(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getPluginMeta().getVersion();
    }

    /**
     * 서버 메인 스레드를 막지 않도록 비동기로 확인합니다.
     * 네트워크 오류 등으로 실패하면 조용히 넘어갑니다.
     */
    void check() {
        final HttpRequest request = HttpRequest.newBuilder(URI.create(LATEST_RELEASE_API))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "QuantumMending/" + this.currentVersion)
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                if (response.statusCode() != 200) {
                    this.plugin.getSLF4JLogger().debug("Update check failed: HTTP {}", response.statusCode());
                    return;
                }
                final JsonObject release = JsonParser.parseString(response.body()).getAsJsonObject();
                final String tag = release.get("tag_name").getAsString();
                final String latest = tag.startsWith("v") ? tag.substring(1) : tag;
                if (compareVersions(latest, this.currentVersion) > 0) {
                    this.latestUrl = release.get("html_url").getAsString();
                    this.latestVersion = latest;
                    this.plugin.getSLF4JLogger().info("새 버전이 있습니다: {} (현재 {}) - {}", latest, this.currentVersion, this.latestUrl);
                }
            })
            .exceptionally(e -> {
                this.plugin.getSLF4JLogger().debug("Update check failed", e);
                return null;
            });
    }

    // 권한(quantummending.update, 기본 OP)이 있는 플레이어가 접속하면 알림
    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final String latest = this.latestVersion;
        final Player player = event.getPlayer();
        if (latest == null || !player.hasPermission(NOTIFY_PERMISSION)) {
            return;
        }
        player.sendMessage(Component.text("[QuantumMending] ", NamedTextColor.GOLD)
            .append(Component.text("새 버전 " + latest + " 이(가) 있습니다. (현재 " + this.currentVersion + ") ", NamedTextColor.YELLOW))
            .append(Component.text("[다운로드]", NamedTextColor.AQUA, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(this.latestUrl))));
    }

    /**
     * "1.2.10" 과 "1.2.9" 처럼 점으로 구분된 버전을 숫자 단위로 비교합니다.
     * 숫자가 아닌 부분(예: "-beta")은 무시합니다.
     *
     * @return a 가 더 높으면 양수, 같으면 0, 낮으면 음수
     */
    static int compareVersions(final String a, final String b) {
        final String[] left = a.split("\\.");
        final String[] right = b.split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            final int l = i < left.length ? leadingNumber(left[i]) : 0;
            final int r = i < right.length ? leadingNumber(right[i]) : 0;
            if (l != r) {
                return Integer.compare(l, r);
            }
        }
        return 0;
    }

    private static int leadingNumber(final String part) {
        int end = 0;
        while (end < part.length() && Character.isDigit(part.charAt(end))) {
            end++;
        }
        return end == 0 ? 0 : Integer.parseInt(part.substring(0, Math.min(end, 9)));
    }
}
