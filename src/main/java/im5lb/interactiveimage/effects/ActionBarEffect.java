package im5lb.interactiveimage.effects;

import im5lb.interactiveimage.config.InteractiveImageConfig;
import im5lb.interactiveimage.model.ResolvedTarget;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public final class ActionBarEffect {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    public void onFocus(Player player, ResolvedTarget target, InteractiveImageConfig cfg, String formatOverride) {
        String text = formatOverride != null ? formatOverride : cfg.effects().actionBar().format();
        player.sendActionBar(LEGACY.deserialize(text == null ? "" : text));
    }

    public void onUnfocus(Player player, InteractiveImageConfig cfg, boolean enabled) {
        if (!enabled) {
            return;
        }
        player.sendActionBar(Component.empty());
    }

    // No placeholders: use explicit text per map instead.
}

