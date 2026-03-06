package me.earth.earthhack.impl.util.thread;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import me.earth.earthhack.api.util.interfaces.Globals;
import me.earth.earthhack.impl.Earthhack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;

/**
 * Utility for {@link Enchantment}s.
 */
public class EnchantmentUtil implements Globals
{
    /**
     * The part of minecraft's EnchantmentHelper needed
     * to calculate Explosion Damage for AutoCrystal etc..
     * But implemented in a way that allows you to access
     * it from multiple threads at the same time. Still not
     * safe regarding the list of item stacks.
     *
     * @param stacks the stacks to check.
     * @param source the damage source.
     */
    public static int getEnchantmentModifierDamage(Iterable<ItemStack> stacks, DamageSource source) {
        int modifier = 0;
        boolean explosion = source != null && source.isIn(DamageTypeTags.IS_EXPLOSION);
        boolean fire = source != null && source.isIn(DamageTypeTags.IS_FIRE);
        boolean fall = source != null && source.isIn(DamageTypeTags.IS_FALL);
        boolean projectile = source != null && source.isIn(DamageTypeTags.IS_PROJECTILE);

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }

            // Vanilla-style EPF approximation without server-world access.
            modifier += getLevel(Enchantments.PROTECTION, stack);

            if (explosion) {
                modifier += getLevel(Enchantments.BLAST_PROTECTION, stack) * 2;
            }

            if (fire) {
                modifier += getLevel(Enchantments.FIRE_PROTECTION, stack) * 2;
            }

            if (fall) {
                modifier += getLevel(Enchantments.FEATHER_FALLING, stack) * 3;
            }

            if (projectile) {
                modifier += getLevel(Enchantments.PROJECTILE_PROTECTION, stack) * 2;
            }
        }

        return Math.min(modifier, 20);
    }

    /**
     * Enchants the given stack with the enchantment represented
     * by the given enchantment, and the given level.
     *
     * @param stack the stack to enchant.
     * @param enchantment the enchantment to add.
     * @param level the level for the enchantment.
     * @throws NullPointerException if no Enchantment for the id is found.
     */
    public static void addEnchantment(ItemStack stack, RegistryKey<Enchantment> enchantment, int level)
    {
        RegistryEntry<Enchantment> entry = convertEnchantmentKeyToEntry(enchantment);
        if (entry != null) {
            stack.addEnchantment(entry, level);
            return;
        }
        Earthhack.getLogger().error("Failed to apply enchantment: " + enchantment.getValue().toString());
    }

    /**
     * Converts {@link RegistryKey<Enchantment>} to a {@link RegistryEntry<Enchantment>}.
     */
    public static RegistryEntry<Enchantment> convertEnchantmentKeyToEntry(RegistryKey<Enchantment> key) {
        if (mc.world == null) return null;
        return mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getEntry(key.getValue()).orElse(null);
    }

    /**
     * Thanks cattyn again
     * <a href="https://github.com/mioclient/oyvey-ported/blob/master/src/main/java/me/alpha432/oyvey/util/EnchantmentUtil.java">...</a>
     */
    public static int getLevel(RegistryKey<Enchantment> key, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> enchantment : stack.getEnchantments().getEnchantmentEntries()) {
            if (enchantment.getKey().matchesKey(key)) return enchantment.getIntValue();
        }
        return 0;
    }

    public static boolean has(RegistryKey<Enchantment> key, EquipmentSlot slot, LivingEntity entity) {
        return getLevel(key, entity.getEquippedStack(slot)) > 0;
    }

}
