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
 * Limits the mending level of naturally generated items: structure loot, villager trades and
 * fishing use {@code loot.max-level}, the enchanting table uses {@code enchanting-table.max-level}.
 */
final class LevelLimitListener implements Listener {

    private final Supplier<MendingConfig> config;

    LevelLimitListener(final Supplier<MendingConfig> config) {
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLootGenerate(final LootGenerateEvent event) {
        final int max = this.config.get().lootMaxLevel();
        for (final ItemStack item : event.getLoot()) {
            limit(item, max, rerollUpTo(max));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVillagerAcquireTrade(final VillagerAcquireTradeEvent event) {
        final int max = this.config.get().lootMaxLevel();
        final MerchantRecipe recipe = event.getRecipe();
        final ItemStack result = recipe.getResult().clone();
        if (!limit(result, max, rerollUpTo(max))) {
            return;
        }

        final MerchantRecipe limited = new MerchantRecipe(result, recipe.getUses(), recipe.getMaxUses(),
            recipe.hasExperienceReward(), recipe.getVillagerExperience(), recipe.getPriceMultiplier(),
            recipe.getDemand(), recipe.getSpecialPrice(), recipe.shouldIgnoreDiscounts());
        limited.setIngredients(recipe.getIngredients());
        event.setRecipe(limited);
    }

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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareEnchant(final PrepareItemEnchantEvent event) {
        final int max = this.config.get().tableMaxLevel();
        for (final EnchantmentOffer offer : event.getOffers()) {
            if (offer != null && offer.getEnchantment().equals(Enchantment.MENDING) && offer.getEnchantmentLevel() > max) {
                offer.setEnchantmentLevel(max);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchant(final EnchantItemEvent event) {
        final int max = this.config.get().tableMaxLevel();
        event.getEnchantsToAdd().computeIfPresent(Enchantment.MENDING, (enchantment, level) -> Math.min(level, max));
    }

    /**
     * Re-rolls an over-the-limit level uniformly within the limit, so a uniformly random source
     * (chest books, librarian trades) stays uniform over 1..max instead of piling up on max.
     */
    private static IntUnaryOperator rerollUpTo(final int max) {
        return level -> ThreadLocalRandom.current().nextInt(1, max + 1);
    }

    /**
     * @return whether the item was changed
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
