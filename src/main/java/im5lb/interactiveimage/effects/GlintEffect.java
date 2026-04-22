package im5lb.interactiveimage.effects;

import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Applies enchantment glint to the item inside an item frame.
 * This is much more visible than entity outline glow for invisible item frames.
 */
public final class GlintEffect {

    private final Map<UUID, Integer> refCounts = new HashMap<>();
    private final Map<UUID, OriginalGlintState> original = new HashMap<>();

    public void onFocus(ItemFrame frame) {
        UUID uuid = frame.getUniqueId();
        int next = refCounts.getOrDefault(uuid, 0) + 1;
        refCounts.put(uuid, next);
        if (next == 1) {
            original.put(uuid, readOriginal(frame));
            apply(frame);
        }
    }

    public void onUnfocus(ItemFrame frame) {
        UUID uuid = frame.getUniqueId();
        int current = refCounts.getOrDefault(uuid, 0);
        if (current <= 1) {
            refCounts.remove(uuid);
            OriginalGlintState state = original.remove(uuid);
            if (state != null) {
                restore(frame, state);
            } else {
                restore(frame, new OriginalGlintState(false, null));
            }
            return;
        }
        refCounts.put(uuid, current - 1);
    }

    public void reapplyIfFocused(ItemFrame frame) {
        UUID uuid = frame.getUniqueId();
        int current = refCounts.getOrDefault(uuid, 0);
        if (current <= 0) {
            return;
        }
        apply(frame);
    }

    public void shutdown() {
        refCounts.clear();
        original.clear();
    }

    private static void apply(ItemFrame frame) {
        ItemStack item = frame.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        Boolean current = null;
        try {
            if (meta.hasEnchantmentGlintOverride()) {
                current = meta.getEnchantmentGlintOverride();
            }
        } catch (Throwable ignored) {
            // Some server implementations throw if the override isn't present.
            current = null;
        }
        if (Boolean.TRUE.equals(current)) {
            return;
        }
        ItemStack clone = item.clone();
        ItemMeta cloneMeta = clone.getItemMeta();
        if (cloneMeta == null) {
            return;
        }
        try {
            cloneMeta.setEnchantmentGlintOverride(Boolean.TRUE);
        } catch (Throwable ignored) {
            return;
        }
        clone.setItemMeta(cloneMeta);
        frame.setItem(clone, false);
    }

    private static OriginalGlintState readOriginal(ItemFrame frame) {
        ItemStack item = frame.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return new OriginalGlintState(false, null);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return new OriginalGlintState(false, null);
        }
        boolean has;
        try {
            has = meta.hasEnchantmentGlintOverride();
        } catch (Throwable ignored) {
            return new OriginalGlintState(false, null);
        }
        if (!has) {
            return new OriginalGlintState(false, null);
        }
        try {
            return new OriginalGlintState(true, meta.getEnchantmentGlintOverride());
        } catch (Throwable ignored) {
            return new OriginalGlintState(false, null);
        }
    }

    private static void restore(ItemFrame frame, OriginalGlintState state) {
        ItemStack item = frame.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        Boolean current = null;
        try {
            if (meta.hasEnchantmentGlintOverride()) {
                current = meta.getEnchantmentGlintOverride();
            }
        } catch (Throwable ignored) {
            current = null;
        }
        Boolean target = state.hasOverride ? state.value : null;
        if (current == target || (current != null && current.equals(target))) {
            return;
        }

        ItemStack clone = item.clone();
        ItemMeta cloneMeta = clone.getItemMeta();
        if (cloneMeta == null) {
            return;
        }
        try {
            cloneMeta.setEnchantmentGlintOverride(target);
        } catch (Throwable ignored) {
            return;
        }
        clone.setItemMeta(cloneMeta);
        frame.setItem(clone, false);
    }

    private record OriginalGlintState(boolean hasOverride, Boolean value) {
    }
}
