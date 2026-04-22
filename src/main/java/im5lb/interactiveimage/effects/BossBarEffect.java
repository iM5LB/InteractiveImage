package im5lb.interactiveimage.effects;

import im5lb.interactiveimage.config.InteractiveImageConfig;
import im5lb.interactiveimage.model.ResolvedTarget;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class BossBarEffect {

    private final Map<UUID, BossBar> barByPlayer = new HashMap<>();

    public void onFocus(Player player, ResolvedTarget target, InteractiveImageConfig cfg, String text, String color, String style, double progress) {
        BossBar bar = barByPlayer.computeIfAbsent(player.getUniqueId(), uuid -> Bukkit.createBossBar("", BarColor.YELLOW, BarStyle.SOLID));
        bar.setTitle(ChatColor.translateAlternateColorCodes('&', text));
        bar.setColor(parseColor(color));
        bar.setStyle(parseStyle(style));
        bar.setProgress(clamp01(progress));
        bar.addPlayer(player);
        bar.setVisible(true);
    }

    public void onUnfocus(Player player) {
        BossBar bar = barByPlayer.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }

    public void shutdown() {
        for (BossBar bar : barByPlayer.values()) {
            bar.removeAll();
        }
        barByPlayer.clear();
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static BarColor parseColor(String s) {
        if (s == null) {
            return BarColor.YELLOW;
        }
        try {
            return BarColor.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BarColor.YELLOW;
        }
    }

    private static BarStyle parseStyle(String s) {
        if (s == null) {
            return BarStyle.SOLID;
        }
        try {
            return BarStyle.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BarStyle.SOLID;
        }
    }
}

