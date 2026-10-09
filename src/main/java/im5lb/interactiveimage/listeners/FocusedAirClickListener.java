package im5lb.interactiveimage.listeners;

import im5lb.interactiveimage.actions.ActionExecutor;
import im5lb.interactiveimage.api.event.InteractiveFrameClickEvent;
import im5lb.interactiveimage.config.InteractiveImageConfig;
import im5lb.interactiveimage.editor.EditorManager;
import im5lb.interactiveimage.focus.FocusScanner;
import im5lb.interactiveimage.focus.FocusState;
import im5lb.interactiveimage.model.ResolvedTarget;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Enables click actions when aiming at interactive frames, supporting both air clicks,
 * block clicks, and arm swings from any range up to the configured click distance.
 */
public final class FocusedAirClickListener implements Listener {

    private final Supplier<InteractiveImageConfig> configSupplier;
    private final FocusScanner focusScanner;
    private final EditorManager editorManager;

    private final ActionExecutor actionExecutor = new ActionExecutor();
    private final Map<UUID, Long> lastLeftClickTick = new ConcurrentHashMap<>();

    public FocusedAirClickListener(
            Supplier<InteractiveImageConfig> configSupplier,
            FocusScanner focusScanner,
            EditorManager editorManager
    ) {
        this.configSupplier = configSupplier;
        this.focusScanner = focusScanner;
        this.editorManager = editorManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAirClick(PlayerInteractEvent event) {
        // Only process the main hand to avoid firing twice (Bukkit fires once per hand).
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        boolean isRight = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        boolean isLeft  = action == Action.LEFT_CLICK_AIR  || action == Action.LEFT_CLICK_BLOCK;
        if (!isRight && !isLeft) {
            return;
        }

        Player player = event.getPlayer();
        if (editorManager != null && editorManager.isEnabled(player)) {
            return;
        }

        InteractiveFrameClickEvent.ClickType clickType = isRight
                ? InteractiveFrameClickEvent.ClickType.RIGHT_CLICK
                : InteractiveFrameClickEvent.ClickType.LEFT_CLICK;

        if (clickType == InteractiveFrameClickEvent.ClickType.LEFT_CLICK) {
            lastLeftClickTick.put(player.getUniqueId(), (long) Bukkit.getCurrentTick());
        }

        if (processClick(player, clickType)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onArmSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }

        Player player = event.getPlayer();
        if (editorManager != null && editorManager.isEnabled(player)) {
            return;
        }

        long currentTick = Bukkit.getCurrentTick();
        Long lastTick = lastLeftClickTick.get(player.getUniqueId());
        if (lastTick != null && lastTick == currentTick) {
            return;
        }
        lastLeftClickTick.put(player.getUniqueId(), currentTick);

        if (processClick(player, InteractiveFrameClickEvent.ClickType.LEFT_CLICK)) {
            event.setCancelled(true);
        }
    }

    private boolean processClick(Player player, InteractiveFrameClickEvent.ClickType clickType) {
        InteractiveImageConfig cfg = configSupplier.get();

        FocusState focus = focusScanner == null ? null : focusScanner.getFocusState(player.getUniqueId());

        ItemFrame frame;
        ResolvedTarget resolved;

        if (focus != null && focus.target() != null && focus.target().rule() != null) {
            World world = Bukkit.getWorld(focus.worldUuid());
            if (world == null) {
                return false;
            }
            var entity = world.getEntity(focus.frameUuid());
            if (!(entity instanceof ItemFrame f) || f.isDead() || !f.isValid()) {
                return false;
            }
            frame = f;
            resolved = focus.target();
        } else {
            if (focusScanner == null) {
                return false;
            }
            var liveResolved = focusScanner.resolveClickTarget(player);
            if (liveResolved.isEmpty() || liveResolved.get().rule() == null) {
                return false;
            }
            resolved = liveResolved.get();
            World world = player.getWorld();
            var entity = world.getEntity(resolved.frameUuid());
            if (!(entity instanceof ItemFrame f) || f.isDead() || !f.isValid()) {
                return false;
            }
            frame = f;
        }

        if (!withinClickDistance(player, frame, cfg, resolved)) {
            return false;
        }

        InteractiveImageConfig.MapRule rule = resolved.rule();
        if (rule == null) {
            return false;
        }

        var apiEvent = new InteractiveFrameClickEvent(player, frame, clickType, resolved.providerId(), resolved.mapName(), resolved.title());
        Bukkit.getPluginManager().callEvent(apiEvent);
        if (apiEvent.isCancelled()) {
            return false;
        }

        Map<String, String> placeholders = Map.of(
                "{player}", player.getName(),
                "{uuid}", player.getUniqueId().toString(),
                "{provider}", resolved.providerId(),
                "{map}", resolved.mapName() == null ? "" : resolved.mapName(),
                "{title}", resolved.title() == null ? "" : resolved.title()
        );

        if (clickType == InteractiveFrameClickEvent.ClickType.RIGHT_CLICK) {
            actionExecutor.run(player, rule.onRightClick(), placeholders);
        } else {
            actionExecutor.run(player, rule.onLeftClick(), placeholders);
        }

        return true;
    }

    private static boolean withinClickDistance(Player player, ItemFrame frame, InteractiveImageConfig cfg, ResolvedTarget target) {
        double max = cfg.activation().click().maxDistance();
        var rule = target.rule();
        if (rule != null && rule.activation() != null && rule.activation().clickMaxDistance() != null) {
            max = rule.activation().clickMaxDistance();
        }
        if (max <= 0.0) {
            return false;
        }
        return player.getEyeLocation().distanceSquared(frame.getLocation()) <= (max * max);
    }
}
