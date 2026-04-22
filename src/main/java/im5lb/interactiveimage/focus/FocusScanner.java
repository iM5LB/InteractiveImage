package im5lb.interactiveimage.focus;

import im5lb.interactiveimage.InteractiveImage;
import im5lb.interactiveimage.config.InteractiveImageConfig;
import im5lb.interactiveimage.effects.EffectManager;
import im5lb.interactiveimage.hooks.TargetResolver;
import im5lb.interactiveimage.model.ResolvedTarget;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.World;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Supplier;

public final class FocusScanner {

    private static final double MAX_RAYTRACE_DISTANCE = 32.0;
    private static final double IMAGE_GROUP_NEIGHBOR_RADIUS = 1.6;
    private static final int MAX_GROUP_FRAMES = 2048;
    private static final long HUD_REFRESH_NANOS = 250_000_000L; // 0.25s

    private final InteractiveImage plugin;
    private final Supplier<InteractiveImageConfig> configSupplier;
    private final List<TargetResolver> resolvers;
    private final EffectManager effectManager;

    private final Map<UUID, FocusState> focusByPlayer = new HashMap<>();
    private final Map<UUID, HoverState> hoverByPlayer = new HashMap<>();
    private int taskId = -1;
    private int glowTaskId = -1;

    public FocusScanner(
            InteractiveImage plugin,
            Supplier<InteractiveImageConfig> configSupplier,
            List<TargetResolver> resolvers,
            EffectManager effectManager
    ) {
        this.plugin = plugin;
        this.configSupplier = configSupplier;
        this.resolvers = List.copyOf(resolvers);
        this.effectManager = effectManager;
    }

    public void start() {
        stop();
        InteractiveImageConfig cfg = configSupplier.get();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 1L, cfg.scan().intervalTicks());
        // Some plugins (or ImageFrame itself) may rewrite entity flags constantly.
        // Enforce glow every tick only for currently-focused frames.
        glowTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::glowTick, 1L, 1L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        if (glowTaskId != -1) {
            Bukkit.getScheduler().cancelTask(glowTaskId);
            glowTaskId = -1;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearFocus(player);
        }
        for (FocusState state : focusByPlayer.values()) {
            World world = Bukkit.getWorld(state.worldUuid());
            for (UUID frameUuid : state.affectedFrameUuids()) {
                ItemFrame frame = getFrame(frameUuid, world);
                if (frame != null) {
                    effectManager.releaseGlow(frame);
                }
            }
            effectManager.shutdownWorld(world);
        }
        focusByPlayer.clear();
        hoverByPlayer.clear();
        effectManager.shutdown();
    }

    private void glowTick() {
        if (focusByPlayer.isEmpty()) {
            return;
        }
        InteractiveImageConfig cfg = configSupplier.get();
        for (FocusState state : focusByPlayer.values()) {
            World world = Bukkit.getWorld(state.worldUuid());
            for (UUID frameUuid : state.affectedFrameUuids()) {
                ItemFrame frame = getFrame(frameUuid, world);
                if (frame == null) {
                    continue;
                }
                effectManager.reapplyGlow(frame, state.target(), cfg);
            }
        }
    }

    private void tick() {
        InteractiveImageConfig cfg = configSupplier.get();
        pruneOfflinePlayers(cfg);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline() || player.isDead()) {
                clearFocus(player);
                hoverByPlayer.remove(player.getUniqueId());
                continue;
            }

            Optional<ResolvedTarget> target = resolveLookingAt(player, cfg);
            FocusState current = focusByPlayer.get(player.getUniqueId());

            if (target.isEmpty()) {
                if (current != null) {
                    clearFocus(player);
                }
                hoverByPlayer.remove(player.getUniqueId());
                continue;
            }

            ResolvedTarget resolved = target.get();
            if (current != null && isSameImage(current.target(), resolved)) {
                FocusState updated = updateFocusedFrameIfNeeded(player, current, resolved, cfg);
                maybeReapplyGlow(player, resolved, cfg, updated);
                refreshWhileFocused(player, resolved, cfg, updated);
                hoverByPlayer.remove(player.getUniqueId());
                continue;
            }

            if (current != null && current.frameUuid().equals(resolved.frameUuid())) {
                maybeReapplyGlow(player, resolved, cfg, current);
                refreshWhileFocused(player, resolved, cfg, current);
                hoverByPlayer.remove(player.getUniqueId());
                continue;
            }

            int requiredTicks = requiredHoverTicks(cfg, resolved);
            if (requiredTicks <= 0) {
                if (current != null) {
                    clearFocus(player);
                }
                applyFocus(player, resolved, cfg);
                hoverByPlayer.remove(player.getUniqueId());
                continue;
            }

            int interval = Math.max(1, cfg.scan().intervalTicks());
            HoverState hover = hoverByPlayer.get(player.getUniqueId());
            if (hover != null && hover.frameUuid().equals(resolved.frameUuid())) {
                int nextTicks = hover.accumulatedTicks() + interval;
                if (nextTicks >= requiredTicks) {
                    if (current != null) {
                        clearFocus(player);
                    }
                    applyFocus(player, resolved, cfg);
                    hoverByPlayer.remove(player.getUniqueId());
                } else {
                    hoverByPlayer.put(player.getUniqueId(), new HoverState(resolved.frameUuid(), resolved, nextTicks));
                }
                continue;
            }

            // New hovered frame
            hoverByPlayer.put(player.getUniqueId(), new HoverState(resolved.frameUuid(), resolved, interval));
        }
    }

    public FocusState getFocusState(UUID playerUuid) {
        return focusByPlayer.get(playerUuid);
    }

    private void pruneOfflinePlayers(InteractiveImageConfig cfg) {
        Iterator<Map.Entry<UUID, FocusState>> it = focusByPlayer.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, FocusState> entry = it.next();
            UUID playerUuid = entry.getKey();
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                continue;
            }

            FocusState state = entry.getValue();
            World world = Bukkit.getWorld(state.worldUuid());
            for (UUID frameUuid : state.affectedFrameUuids()) {
                ItemFrame frame = getFrame(frameUuid, world);
                if (frame != null) {
                    effectManager.releaseGlow(frame);
                }
            }
            effectManager.shutdownWorld(world);
            it.remove();
            hoverByPlayer.remove(playerUuid);
        }
    }

    private void maybeReapplyGlow(Player player, ResolvedTarget resolved, InteractiveImageConfig cfg, FocusState state) {
        World world = Bukkit.getWorld(state.worldUuid());
        if (world == null) {
            clearFocus(player);
            return;
        }
        ItemFrame frame = getFrame(state.frameUuid(), world);
        if (frame == null) {
            clearFocus(player);
            return;
        }
        effectManager.reapplyGlow(frame, resolved, cfg);
    }

    private void refreshWhileFocused(Player player, ResolvedTarget resolved, InteractiveImageConfig cfg, FocusState state) {
        // Keep HUD effects active while focused, but throttle updates to avoid spam.
        World world = Bukkit.getWorld(state.worldUuid());
        if (world == null) {
            clearFocus(player);
            return;
        }
        ItemFrame frame = getFrame(state.frameUuid(), world);
        if (frame == null) {
            clearFocus(player);
            return;
        }

        long now = System.nanoTime();

        if (state.lastHudNanos() == 0L || now - state.lastHudNanos() >= HUD_REFRESH_NANOS) {
            effectManager.refresh(player, frame, resolved, cfg);
            focusByPlayer.put(player.getUniqueId(), new FocusState(state.worldUuid(), state.frameUuid(), state.affectedFrameUuids(), resolved, now));
        }
    }

    private Optional<ResolvedTarget> resolveLookingAt(Player player, InteractiveImageConfig cfg) {
        // Ray trace beyond per-frame limits, then apply exact per-frame distance checks afterwards.
        // This allows maps to have hover distances larger than the default without needing global settings.
        ItemFrame frame = rayTraceItemFrame(player, MAX_RAYTRACE_DISTANCE);
        if (frame == null) {
            return Optional.empty();
        }

        for (TargetResolver resolver : resolvers) {
            Optional<ResolvedTarget> resolved = resolver.resolve(frame, player, cfg);
            if (resolved.isPresent()) {
                ResolvedTarget target = resolved.get();
                if (!withinHoverDistance(player, frame, cfg, target)) {
                    return Optional.empty();
                }
                return Optional.of(target);
            }
        }
        return Optional.empty();
    }

    private ItemFrame rayTraceItemFrame(Player player, double maxDistance) {
        World world = player.getWorld();
        var eye = player.getEyeLocation();
        Vector direction = eye.getDirection();

        RayTraceResult result = world.rayTrace(
                eye,
                direction,
                maxDistance,
                FluidCollisionMode.NEVER,
                true,
                0.0,
                entity -> entity instanceof ItemFrame
        );

        if (result == null || result.getHitEntity() == null) {
            return null;
        }
        return (ItemFrame) result.getHitEntity();
    }

    private void applyFocus(Player player, ResolvedTarget resolved, InteractiveImageConfig cfg) {
        ItemFrame frame = getFrame(resolved.frameUuid(), player.getWorld());
        if (frame == null) {
            return;
        }

        List<UUID> affected = findRelatedFrames(player, frame, resolved, cfg);
        // Apply visuals to all frames in the image, HUD only once.
        effectManager.onFocus(player, frame, resolved, cfg);
        for (UUID frameUuid : affected) {
            if (frameUuid.equals(frame.getUniqueId())) {
                continue;
            }
            ItemFrame other = getFrame(frameUuid, player.getWorld());
            if (other != null) {
                effectManager.onFocusVisuals(other, resolved, cfg);
            }
        }

        long now = System.nanoTime();
        focusByPlayer.put(player.getUniqueId(), new FocusState(player.getWorld().getUID(), resolved.frameUuid(), affected, resolved, now));
    }

    private List<UUID> findRelatedFrames(Player viewer, ItemFrame focusedFrame, ResolvedTarget resolved, InteractiveImageConfig cfg) {
        // For ImageFrame, one "image" can be multiple item frames.
        // Highlight all connected frames that resolve to the same provider + mapName.
        // Use a flood-fill style search (adjacent frames) so large images work reliably.
        if (resolved.mapName() == null || resolved.providerId() == null) {
            return List.of(focusedFrame.getUniqueId());
        }

        Set<UUID> visited = new HashSet<>();
        Deque<ItemFrame> queue = new ArrayDeque<>();
        visited.add(focusedFrame.getUniqueId());
        queue.add(focusedFrame);

        while (!queue.isEmpty() && visited.size() < MAX_GROUP_FRAMES) {
            ItemFrame current = queue.removeFirst();

            for (var entity : current.getWorld().getNearbyEntities(
                    current.getLocation(),
                    IMAGE_GROUP_NEIGHBOR_RADIUS,
                    IMAGE_GROUP_NEIGHBOR_RADIUS,
                    IMAGE_GROUP_NEIGHBOR_RADIUS,
                    e -> e instanceof ItemFrame
            )) {
                if (!(entity instanceof ItemFrame candidate)) {
                    continue;
                }
                UUID candId = candidate.getUniqueId();
                if (visited.contains(candId)) {
                    continue;
                }

                if (!isAdjacentInPlane(current, candidate)) {
                    continue;
                }

                if (isSameImage(resolved, resolveCandidate(candidate, viewer, cfg))) {
                    visited.add(candId);
                    queue.addLast(candidate);
                }
            }
        }

        return List.copyOf(visited);
    }

    private ResolvedTarget resolveCandidate(ItemFrame frame, Player viewer, InteractiveImageConfig cfg) {
        for (TargetResolver resolver : resolvers) {
            Optional<ResolvedTarget> cand = resolver.resolve(frame, viewer, cfg);
            if (cand.isPresent()) {
                return cand.get();
            }
        }
        return null;
    }

    private static boolean isSameImage(ResolvedTarget a, ResolvedTarget b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.providerId() == null || a.mapName() == null || b.providerId() == null || b.mapName() == null) {
            return false;
        }
        return a.providerId().equalsIgnoreCase(b.providerId()) && a.mapName().equalsIgnoreCase(b.mapName());
    }

    private static boolean isAdjacentInPlane(ItemFrame a, ItemFrame b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getFacing() != b.getFacing()) {
            return false;
        }

        var la = a.getLocation();
        var lb = b.getLocation();
        int dx = lb.getBlockX() - la.getBlockX();
        int dy = lb.getBlockY() - la.getBlockY();
        int dz = lb.getBlockZ() - la.getBlockZ();

        // Only direct neighbors (no diagonals).
        switch (a.getFacing()) {
            case NORTH, SOUTH -> {
                return dz == 0 && (Math.abs(dx) + Math.abs(dy) == 1);
            }
            case EAST, WEST -> {
                return dx == 0 && (Math.abs(dz) + Math.abs(dy) == 1);
            }
            case UP, DOWN -> {
                return dy == 0 && (Math.abs(dx) + Math.abs(dz) == 1);
            }
            default -> {
                return false;
            }
        }
    }

    private FocusState updateFocusedFrameIfNeeded(Player player, FocusState currentState, ResolvedTarget resolved, InteractiveImageConfig cfg) {
        if (currentState.frameUuid().equals(resolved.frameUuid())) {
            return currentState;
        }

        World world = Bukkit.getWorld(currentState.worldUuid());
        if (world == null) {
            clearFocus(player);
            return currentState;
        }

        ItemFrame newFrame = getFrame(resolved.frameUuid(), world);
        if (newFrame == null) {
            clearFocus(player);
            return currentState;
        }

        List<UUID> nextAffected = findRelatedFrames(player, newFrame, resolved, cfg);
        Set<UUID> prev = new HashSet<>(currentState.affectedFrameUuids());
        Set<UUID> next = new HashSet<>(nextAffected);

        for (UUID removed : prev) {
            if (next.contains(removed)) {
                continue;
            }
            ItemFrame frame = getFrame(removed, world);
            if (frame != null) {
                effectManager.onUnfocusVisuals(frame, currentState.target(), cfg);
            }
        }

        for (UUID added : next) {
            if (prev.contains(added)) {
                continue;
            }
            ItemFrame frame = getFrame(added, world);
            if (frame != null) {
                effectManager.onFocusVisuals(frame, resolved, cfg);
            }
        }

        FocusState updated = new FocusState(world.getUID(), resolved.frameUuid(), List.copyOf(nextAffected), resolved, currentState.lastHudNanos());
        focusByPlayer.put(player.getUniqueId(), updated);
        return updated;
    }


    private void clearFocus(Player player) {
        FocusState state = focusByPlayer.remove(player.getUniqueId());
        if (state == null) {
            return;
        }
        InteractiveImageConfig cfg = configSupplier.get();
        World world = Bukkit.getWorld(state.worldUuid());
        for (UUID frameUuid : state.affectedFrameUuids()) {
            ItemFrame frame = getFrame(frameUuid, world);
            if (frame != null) {
                effectManager.onUnfocusVisuals(frame, state.target(), cfg);
            }
        }
        effectManager.onUnfocus(player, null, state.target(), cfg, true);
    }

    public UUID getFocusedFrameUuid(UUID playerUuid) {
        FocusState state = focusByPlayer.get(playerUuid);
        return state == null ? null : state.frameUuid();
    }

    public void clearFocusedMap(String providerId, String mapName) {
        if (providerId == null || mapName == null) {
            return;
        }
        for (var entry : new ArrayList<>(focusByPlayer.entrySet())) {
            UUID playerUuid = entry.getKey();
            FocusState state = entry.getValue();
            ResolvedTarget target = state.target();
            if (target == null || target.providerId() == null || target.mapName() == null) {
                continue;
            }
            if (!providerId.equalsIgnoreCase(target.providerId())) {
                continue;
            }
            if (!mapName.equalsIgnoreCase(target.mapName())) {
                continue;
            }
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                clearFocus(player);
            } else {
                focusByPlayer.remove(playerUuid);
            }
        }
    }

    private static int requiredHoverTicks(InteractiveImageConfig cfg, ResolvedTarget target) {
        // No tick-based hover delay: effects activate immediately when focused.
        return 0;
    }

    private static boolean withinHoverDistance(Player player, ItemFrame frame, InteractiveImageConfig cfg, ResolvedTarget target) {
        double max = cfg.activation().hover().maxDistance();
        var rule = target.rule();
        if (rule != null && rule.activation() != null && rule.activation().hoverMaxDistance() != null) {
            max = rule.activation().hoverMaxDistance();
        }
        if (max <= 0.0) {
            return false;
        }
        double maxSq = max * max;
        return player.getEyeLocation().distanceSquared(frame.getLocation()) <= maxSq;
    }

    private ItemFrame getFrame(UUID uuid, World world) {
        if (world == null) {
            return null;
        }
        var entity = world.getEntity(uuid);
        if (!(entity instanceof ItemFrame frame)) {
            return null;
        }
        if (frame.isDead() || !frame.isValid()) {
            return null;
        }
        return frame;
    }
}

