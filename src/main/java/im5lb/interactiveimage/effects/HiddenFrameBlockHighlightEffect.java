package im5lb.interactiveimage.effects;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemFrame;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Highlights the attached block using a glowing BlockDisplay.
 */
public final class HiddenFrameBlockHighlightEffect {

    private final Map<UUID, Integer> refCounts = new HashMap<>();
    private final Map<UUID, UUID> displayByFrame = new HashMap<>();

    public void onFocus(ItemFrame frame, String glowColor) {
        UUID frameUuid = frame.getUniqueId();
        int next = refCounts.getOrDefault(frameUuid, 0) + 1;
        refCounts.put(frameUuid, next);
        if (next == 1) {
            spawn(frame, glowColor);
        } else {
            reapplyIfFocused(frame, glowColor);
        }
    }

    public void onUnfocus(ItemFrame frame) {
        UUID frameUuid = frame.getUniqueId();
        int current = refCounts.getOrDefault(frameUuid, 0);
        if (current <= 1) {
            refCounts.remove(frameUuid);
            removeDisplay(frame.getWorld(), frameUuid);
            return;
        }
        refCounts.put(frameUuid, current - 1);
    }

    public void reapplyIfFocused(ItemFrame frame, String glowColor) {
        UUID frameUuid = frame.getUniqueId();
        int current = refCounts.getOrDefault(frameUuid, 0);
        if (current <= 0) {
            return;
        }

        UUID displayUuid = displayByFrame.get(frameUuid);
        if (displayUuid == null) {
            spawn(frame, glowColor);
            return;
        }

        var entity = frame.getWorld().getEntity(displayUuid);
        if (entity instanceof BlockDisplay display) {
            if (!display.isGlowing()) {
                display.setGlowing(true);
            }
            applyGlowColorOverride(display, glowColor);
            return;
        }

        spawn(frame, glowColor);
    }

    public void shutdown(World world) {
        for (UUID frameUuid : new ArrayList<>(displayByFrame.keySet())) {
            removeDisplay(world, frameUuid);
        }
        refCounts.clear();
        displayByFrame.clear();
    }

    private void spawn(ItemFrame frame, String glowColor) {
        Block attached = getAttachedBlock(frame);
        if (attached == null) {
            return;
        }

        UUID frameUuid = frame.getUniqueId();
        UUID old = displayByFrame.get(frameUuid);
        if (old != null) {
            var oldEntity = frame.getWorld().getEntity(old);
            if (oldEntity != null) {
                oldEntity.remove();
            }
        }

        // Spawn at the exact block position; BlockDisplay origin is the block corner.
        Location loc = attached.getLocation();
        BlockDisplay display = frame.getWorld().spawn(loc, BlockDisplay.class, d -> {
            d.setBlock(attached.getBlockData());
            d.setGlowing(true);
            d.setPersistent(false);
            d.setBrightness(new Display.Brightness(15, 15));
            applyGlowColorOverride(d, glowColor);

            Vector dir = frame.getFacing().getDirection();
            float eps = 0.01f;
            float pad = -0.005f;
            Vector3f translation = new Vector3f(
                    pad + (float) dir.getX() * eps,
                    pad + (float) dir.getY() * eps,
                    pad + (float) dir.getZ() * eps
            );
            d.setTransformation(new Transformation(
                    translation,
                    new org.joml.Quaternionf(),
                    new Vector3f(1.01f, 1.01f, 1.01f),
                    new org.joml.Quaternionf()
            ));
        });

        displayByFrame.put(frameUuid, display.getUniqueId());
    }

    private void removeDisplay(World world, UUID frameUuid) {
        UUID displayUuid = displayByFrame.remove(frameUuid);
        if (displayUuid == null) {
            return;
        }
        var entity = world.getEntity(displayUuid);
        if (entity != null) {
            entity.remove();
        }
    }

    private static Block getAttachedBlock(ItemFrame frame) {
        var face = frame.getAttachedFace();
        return frame.getLocation().getBlock().getRelative(face);
    }

    private static void applyGlowColorOverride(org.bukkit.entity.Entity entity, String glowColor) {
        if (entity == null || glowColor == null || glowColor.isBlank()) {
            return;
        }
        try {
            java.lang.reflect.Method m = entity.getClass().getMethod("setGlowColorOverride", org.bukkit.Color.class);
            org.bukkit.Color color = GlowColors.parse(glowColor);
            if (color != null) {
                m.invoke(entity, color);
            }
        } catch (Throwable ignored) {
        }
    }
}
