package im5lb.interactiveimage.listeners;

import im5lb.interactiveimage.actions.ActionExecutor;
import im5lb.interactiveimage.api.event.InteractiveFrameClickEvent;
import im5lb.interactiveimage.config.InteractiveImageConfig;
import im5lb.interactiveimage.editor.EditorManager;
import im5lb.interactiveimage.hooks.TargetResolver;
import im5lb.interactiveimage.hooks.imageframe.ImageFrameResolver;
import im5lb.interactiveimage.model.ResolvedTarget;
import org.bukkit.Bukkit;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FrameInteractListener implements Listener {

    private final Supplier<InteractiveImageConfig> configSupplier;
    private final List<TargetResolver> resolvers;
    private final EditorManager editorManager;
    private final Function<UUID, UUID> focusedFrameLookup;

    private final ActionExecutor actionExecutor = new ActionExecutor();

    public FrameInteractListener(
            Plugin plugin,
            Supplier<InteractiveImageConfig> configSupplier,
            List<TargetResolver> resolvers,
            EditorManager editorManager,
            Function<UUID, UUID> focusedFrameLookup
    ) {
        this.configSupplier = configSupplier;
        this.resolvers = List.copyOf(resolvers);
        this.editorManager = editorManager;
        this.focusedFrameLookup = focusedFrameLookup;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!(event.getRightClicked() instanceof ItemFrame frame)) {
            return;
        }
        Player player = event.getPlayer();

        if (editorManager != null && editorManager.isEnabled(player)) {
            InteractiveImageConfig cfg = configSupplier.get();
            Optional<ResolvedTarget> resolvedOpt = resolveForAdmin(frame, player, cfg);
            if (resolvedOpt.isPresent()) {
                ResolvedTarget resolved = resolvedOpt.get();
                if ("imageframe".equalsIgnoreCase(resolved.providerId())) {
                    event.setCancelled(true);
                    editorManager.selectTarget(player, resolved.providerId(), resolved.mapName());
                    editorManager.openEditor(player);
                    return;
                }
            }
        }

        handleClick(event, player, frame, InteractiveFrameClickEvent.ClickType.RIGHT_CLICK);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof ItemFrame frame)) {
            return;
        }

        if (editorManager != null && editorManager.isEnabled(player)) {
            InteractiveImageConfig cfg = configSupplier.get();
            Optional<ResolvedTarget> resolvedOpt = resolveForAdmin(frame, player, cfg);
            if (resolvedOpt.isPresent()) {
                ResolvedTarget resolved = resolvedOpt.get();
                if ("imageframe".equalsIgnoreCase(resolved.providerId())) {
                    event.setCancelled(true);
                    editorManager.selectTarget(player, resolved.providerId(), resolved.mapName());
                    editorManager.openEditor(player);
                    return;
                }
            }
        }

        handleClick(event, player, frame, InteractiveFrameClickEvent.ClickType.LEFT_CLICK);
    }

    private void handleClick(Cancellable cancellable, Player player, ItemFrame frame, InteractiveFrameClickEvent.ClickType clickType) {
        InteractiveImageConfig cfg = configSupplier.get();

        Optional<ResolvedTarget> resolvedOpt = resolve(frame, player, cfg);
        if (resolvedOpt.isEmpty()) {
            return;
        }
        ResolvedTarget resolved = resolvedOpt.get();

        InteractiveImageConfig.MapRule rule = resolved.rule();
        if (clickType == InteractiveFrameClickEvent.ClickType.LEFT_CLICK || (rule != null && rule.cancelInteract())) {
            // Always cancel entity damage for left-clicks to prevent destroying/dropping the interactive item frame,
            // and cancel vanilla interaction for right-clicks if configured.
            cancellable.setCancelled(true);
        }
        if (!withinClickDistance(player, frame, cfg, resolved)) {
            return;
        }
        if (requiresHoverForClick(cfg, resolved) && focusedFrameLookup != null) {
            UUID focused = focusedFrameLookup.apply(player.getUniqueId());
            if (focused == null || !focused.equals(frame.getUniqueId())) {
                return;
            }
        }

        var apiEvent = new InteractiveFrameClickEvent(player, frame, clickType, resolved.providerId(), resolved.mapName(), resolved.title());
        Bukkit.getPluginManager().callEvent(apiEvent);
        if (apiEvent.isCancelled()) {
            return;
        }

        if (rule == null) {
            return;
        }

        // No tick-based cooldown: actions run immediately when clicked.

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
    }

    private Optional<ResolvedTarget> resolve(ItemFrame frame, Player viewer, InteractiveImageConfig cfg) {
        for (TargetResolver resolver : resolvers) {
            Optional<ResolvedTarget> resolved = resolver.resolve(frame, viewer, cfg);
            if (resolved.isPresent()) {
                return resolved;
            }
        }
        return Optional.empty();
    }

    private Optional<ResolvedTarget> resolveForAdmin(ItemFrame frame, Player viewer, InteractiveImageConfig cfg) {
        for (TargetResolver resolver : resolvers) {
            if (resolver instanceof ImageFrameResolver imageFrameResolver) {
                Optional<ResolvedTarget> resolved = imageFrameResolver.resolveForAdmin(frame, viewer, cfg);
                if (resolved.isPresent()) {
                    return resolved;
                }
                continue;
            }
            Optional<ResolvedTarget> resolved = resolver.resolve(frame, viewer, cfg);
            if (resolved.isPresent()) {
                return resolved;
            }
        }
        return Optional.empty();
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

    private static boolean requiresHoverForClick(InteractiveImageConfig cfg, ResolvedTarget target) {
        var rule = target.rule();
        if (rule != null && rule.activation() != null && !rule.activation().requireHoverForClick().isInherit()) {
            return rule.activation().requireHoverForClick().orElse(cfg.activation().click().requireHover());
        }
        return cfg.activation().click().requireHover();
    }
}

