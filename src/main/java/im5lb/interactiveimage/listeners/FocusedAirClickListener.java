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
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Enables "click actions" beyond vanilla entity interaction reach by using the currently focused target and
 * listening for air clicks.
 *
 * Note: We only handle AIR clicks to reduce conflicts with normal block interactions.
 */
public final class FocusedAirClickListener implements Listener {

    private final Supplier<InteractiveImageConfig> configSupplier;
    private final FocusScanner focusScanner;
    private final EditorManager editorManager;

    private final ActionExecutor actionExecutor = new ActionExecutor();

    public FocusedAirClickListener(
            Supplier<InteractiveImageConfig> configSupplier,
            FocusScanner focusScanner,
            EditorManager editorManager
    ) {
        this.configSupplier = configSupplier;
        this.focusScanner = focusScanner;
        this.editorManager = editorManager;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onAirClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.LEFT_CLICK_AIR) {
            return;
        }

        Player player = event.getPlayer();
        if (editorManager != null && editorManager.isEnabled(player)) {
            return;
        }

        FocusState focus = focusScanner == null ? null : focusScanner.getFocusState(player.getUniqueId());
        if (focus == null) {
            return;
        }
        ResolvedTarget resolved = focus.target();
        if (resolved == null || resolved.rule() == null) {
            return;
        }

        InteractiveImageConfig cfg = configSupplier.get();

        World world = Bukkit.getWorld(focus.worldUuid());
        if (world == null) {
            return;
        }
        var entity = world.getEntity(focus.frameUuid());
        if (!(entity instanceof ItemFrame frame) || frame.isDead() || !frame.isValid()) {
            return;
        }

        if (!withinClickDistance(player, frame, cfg, resolved)) {
            return;
        }

        InteractiveFrameClickEvent.ClickType clickType =
                action == Action.RIGHT_CLICK_AIR
                        ? InteractiveFrameClickEvent.ClickType.RIGHT_CLICK
                        : InteractiveFrameClickEvent.ClickType.LEFT_CLICK;

        var apiEvent = new InteractiveFrameClickEvent(player, frame, clickType, resolved.providerId(), resolved.mapName(), resolved.title());
        Bukkit.getPluginManager().callEvent(apiEvent);
        if (apiEvent.isCancelled()) {
            return;
        }

        // No tick-based cooldown: actions run immediately when clicked.
        InteractiveImageConfig.MapRule rule = resolved.rule();

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

        // Prevent accidental item use when interacting at a distance.
        event.setCancelled(true);
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

