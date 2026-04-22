package im5lb.interactiveimage.effects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class TitleEffect {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    public void show(Player player, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        Component t = LEGACY.deserialize(title == null ? "" : title);
        Component s = LEGACY.deserialize(subtitle == null ? "" : subtitle);
        Title.Times times = Title.Times.times(
                Duration.ofMillis(Math.max(0, fadeInTicks) * 50L),
                Duration.ofMillis(Math.max(0, stayTicks) * 50L),
                Duration.ofMillis(Math.max(0, fadeOutTicks) * 50L)
        );
        player.showTitle(Title.title(t, s, times));
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        try {
            player.clearTitle();
        } catch (Throwable ignored) {
            // Fallback for older APIs
            player.showTitle(Title.title(Component.empty(), Component.empty(), Title.Times.times(Duration.ZERO, Duration.ZERO, Duration.ZERO)));
        }
    }
}
