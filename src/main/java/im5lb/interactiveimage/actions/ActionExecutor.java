package im5lb.interactiveimage.actions;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class ActionExecutor {

    public void run(Player player, List<String> actions, Map<String, String> placeholders) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        for (String raw : actions) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String expanded = applyPlaceholders(raw.trim(), placeholders);

            if (expanded.regionMatches(true, 0, "console:", 0, "console:".length())) {
                String command = expanded.substring("console:".length()).trim();
                if (!command.isEmpty()) {
                    dispatch(Bukkit.getConsoleSender(), command);
                }
                continue;
            }

            if (expanded.regionMatches(true, 0, "player:", 0, "player:".length())) {
                String command = expanded.substring("player:".length()).trim();
                if (!command.isEmpty()) {
                    dispatch(player, command);
                }
                continue;
            }

            dispatch(player, expanded);
        }
    }

    private static void dispatch(CommandSender sender, String command) {
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        Bukkit.dispatchCommand(sender, cmd);
    }

    private static String applyPlaceholders(String input, Map<String, String> placeholders) {
        String out = input;
        for (var entry : placeholders.entrySet()) {
            out = out.replace(entry.getKey(), entry.getValue());
        }
        return out;
    }
}
