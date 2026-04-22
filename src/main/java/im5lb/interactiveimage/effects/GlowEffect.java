package im5lb.interactiveimage.effects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.entity.ItemFrame;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class GlowEffect {

    private final Map<UUID, Integer> refCounts = new HashMap<>();
    private final Map<UUID, Boolean> originalGlowing = new HashMap<>();
    private final Map<UUID, Color> originalGlowColorOverride = new HashMap<>();
    private final Map<UUID, String> originalTeamName = new HashMap<>();
    private final Map<UUID, String> appliedTeamName = new HashMap<>();

    public void onFocus(ItemFrame frame, String glowColor) {
        UUID uuid = frame.getUniqueId();
        int next = refCounts.getOrDefault(uuid, 0) + 1;
        refCounts.put(uuid, next);
        if (next == 1) {
            originalGlowing.put(uuid, frame.isGlowing());
            originalGlowColorOverride.put(uuid, readGlowColorOverride(frame));
            frame.setGlowing(true);
            applyColor(frame, glowColor);
        }
    }

    public void onUnfocus(ItemFrame frame) {
        UUID uuid = frame.getUniqueId();
        int current = refCounts.getOrDefault(uuid, 0);
        if (current <= 1) {
            refCounts.remove(uuid);
            Boolean original = originalGlowing.remove(uuid);
            if (original != null) {
                frame.setGlowing(original);
            } else {
                frame.setGlowing(false);
            }
            restoreGlowColorOverride(frame, originalGlowColorOverride.remove(uuid));
            restoreTeam(frame);
            return;
        }
        refCounts.put(uuid, current - 1);
    }

    public void reapplyIfFocused(ItemFrame frame, String glowColor) {
        UUID uuid = frame.getUniqueId();
        int current = refCounts.getOrDefault(uuid, 0);
        if (current > 0) {
            if (!frame.isGlowing()) {
                frame.setGlowing(true);
            }
            applyColor(frame, glowColor);
        }
    }

    public void shutdown() {
        refCounts.clear();
        originalGlowing.clear();
        originalGlowColorOverride.clear();
        originalTeamName.clear();
        appliedTeamName.clear();
    }

    private void applyColor(ItemFrame frame, String glowColor) {
        if (!applyGlowColorOverride(frame, glowColor)) {
            applyScoreboardColor(frame, glowColor);
        }
    }

    private static boolean applyGlowColorOverride(org.bukkit.entity.Entity entity, String glowColor) {
        if (entity == null || glowColor == null || glowColor.isBlank()) {
            return true;
        }
        try {
            java.lang.reflect.Method m = entity.getClass().getMethod("setGlowColorOverride", org.bukkit.Color.class);
            org.bukkit.Color color = GlowColors.parse(glowColor);
            if (color != null) {
                m.invoke(entity, color);
            }
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Color readGlowColorOverride(org.bukkit.entity.Entity entity) {
        try {
            java.lang.reflect.Method m = entity.getClass().getMethod("getGlowColorOverride");
            Object v = m.invoke(entity);
            if (v instanceof Color c) {
                return c;
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void restoreGlowColorOverride(org.bukkit.entity.Entity entity, Color value) {
        try {
            java.lang.reflect.Method m = entity.getClass().getMethod("setGlowColorOverride", org.bukkit.Color.class);
            m.invoke(entity, value);
        } catch (Throwable ignored) {
        }
    }

    private void applyScoreboardColor(ItemFrame frame, String glowColor) {
        ChatColor chatColor = GlowColors.toChatColor(glowColor);
        if (chatColor == null) {
            return;
        }

        var manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        Scoreboard scoreboard = manager.getMainScoreboard();

        String teamName = ("ii_" + chatColor.name().toLowerCase(Locale.ROOT));
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        try {
            team.setColor(chatColor);
        } catch (Throwable ignored) {
        }

        String entry = frame.getUniqueId().toString();
        Team current = scoreboard.getEntryTeam(entry);
        UUID uuid = frame.getUniqueId();
        if (!originalTeamName.containsKey(uuid)) {
            originalTeamName.put(uuid, current == null ? null : current.getName());
        }

        if (current != null && !current.getName().equals(teamName)) {
            current.removeEntry(entry);
        }
        team.addEntry(entry);
        appliedTeamName.put(uuid, teamName);
    }

    private void restoreTeam(ItemFrame frame) {
        var manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        Scoreboard scoreboard = manager.getMainScoreboard();
        UUID uuid = frame.getUniqueId();
        String entry = uuid.toString();

        String applied = appliedTeamName.remove(uuid);
        if (applied != null) {
            Team team = scoreboard.getTeam(applied);
            if (team != null) {
                team.removeEntry(entry);
            }
        }

        String original = originalTeamName.remove(uuid);
        if (original != null) {
            Team team = scoreboard.getTeam(original);
            if (team != null) {
                team.addEntry(entry);
            }
        }
    }
}
