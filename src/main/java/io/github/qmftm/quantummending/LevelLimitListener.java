package io.github.qmftm.quantummending;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

/**
 * 자연적으로 얻는 아이템의 수선 레벨을 제한합니다.
 * <ul>
 *     <li>구조물 상자, 주민 거래, 낚시: {@code loot.max-level}</li>
 *     <li>인챈트 테이블: {@code enchanting-table.max-level}</li>
 * </ul>
 * 바닐라 전리품은 레벨을 1 ~ 최대 레벨 중에서 뽑기 때문에, 그대로 두면 상자에서 바로 높은 레벨이 나옵니다.
 */
final class LevelLimitListener implements Listener {

    private final Supplier<MendingConfig> config;

    LevelLimitListener(final Supplier<MendingConfig> config) {
        this.config = config;
    }

    // 구조물 상자 등 전리품 테이블로 아이템이 생성될 때
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLootGenerate(final LootGenerateEvent event) {
        final int max = this.config.get().lootMaxLevel();
        for (final ItemStack item : event.getLoot()) {
            limit(item, max, rerollUpTo(max));
        }
    }

    // 주민(떠돌이 상인 포함)이 새 거래를 얻을 때
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVillagerAcquireTrade(final VillagerAcquireTradeEvent event) {
        final int max = this.config.get().lootMaxLevel();
        final MerchantRecipe recipe = event.getRecipe();
        final ItemStack result = recipe.getResult().clone();
        if (!limit(result, max, rerollUpTo(max))) {
            return;
        }

        // 거래 결과 아이템은 바꿀 수 없으므로, 나머지 값을 그대로 복사해 새 거래를 만듭니다.
        // (가격은 원래 뽑힌 레벨 기준으로 유지됩니다)
        final MerchantRecipe limited = new MerchantRecipe(result, recipe.getUses(), recipe.getMaxUses(),
            recipe.hasExperienceReward(), recipe.getVillagerExperience(), recipe.getPriceMultiplier(),
            recipe.getDemand(), recipe.getSpecialPrice(), recipe.shouldIgnoreDiscounts());
        limited.setIngredients(recipe.getIngredients());
        event.setRecipe(limited);
    }

    // 낚시로 아이템을 낚았을 때
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(final PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof final Item caught)) {
            return;
        }
        final int max = this.config.get().lootMaxLevel();
        final ItemStack item = caught.getItemStack();
        if (limit(item, max, rerollUpTo(max))) {
            caught.setItemStack(item);
        }
    }

    // 인챈트 테이블에 아이템을 올렸을 때: 미리보기(힌트)에 표시되는 레벨을 제한
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareEnchant(final PrepareItemEnchantEvent event) {
        final int max = this.config.get().tableMaxLevel();
        for (final EnchantmentOffer offer : event.getOffers()) {
            if (offer != null && offer.getEnchantment().equals(Enchantment.MENDING) && offer.getEnchantmentLevel() > max) {
                offer.setEnchantmentLevel(max);
            }
        }
    }

    // 인챈트 테이블에서 실제로 인챈트할 때: 붙는 레벨을 제한 (넘으면 최대 레벨로 맞춤)
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchant(final EnchantItemEvent event) {
        final int max = this.config.get().tableMaxLevel();
        event.getEnchantsToAdd().computeIfPresent(Enchantment.MENDING, (enchantment, level) -> Math.min(level, max));
    }

    /**
     * 상한을 넘은 레벨을 1 ~ max 사이에서 다시 뽑습니다.
     * 그냥 max 로 깎으면 max 가 너무 자주 나오므로, 상자 책·사서 거래처럼 균등하게 뽑는 경우
     * 결과도 1 ~ max 균등 분포가 되도록 합니다.
     */
    private static IntUnaryOperator rerollUpTo(final int max) {
        return level -> ThreadLocalRandom.current().nextInt(1, max + 1);
    }

    /**
     * 아이템의 수선 레벨이 max 를 넘으면 replacement 로 바꿉니다.
     * 일반 아이템의 인챈트와 인챈트된 책의 저장된 인챈트를 모두 확인합니다.
     *
     * @return 아이템이 변경되었는지 여부
     */
    static boolean limit(final ItemStack item, final int max, final IntUnaryOperator replacement) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        final ItemMeta meta = item.getItemMeta();
        boolean changed = false;
        final int level = meta.getEnchantLevel(Enchantment.MENDING);
        if (level > max) {
            meta.addEnchant(Enchantment.MENDING, replacement.applyAsInt(level), true);
            changed = true;
        }
        if (meta instanceof final EnchantmentStorageMeta storage) {
            final int stored = storage.getStoredEnchantLevel(Enchantment.MENDING);
            if (stored > max) {
                storage.addStoredEnchant(Enchantment.MENDING, replacement.applyAsInt(stored), true);
                changed = true;
            }
        }
        if (changed) {
            item.setItemMeta(meta);
        }
        return changed;
    }
}
