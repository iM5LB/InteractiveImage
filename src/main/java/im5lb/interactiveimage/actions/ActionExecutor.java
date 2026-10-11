package im5lb.interactiveimage.actions;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.Map;

public final class ActionExecutor {

    private final Plugin plugin;

    public ActionExecutor(Plugin plugin) {
        this.plugin = plugin;
    }

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

            if (expanded.regionMatches(true, 0, "server:", 0, "server:".length())) {
                String target = expanded.substring("server:".length()).trim();
                if (!target.isEmpty()) {
                    sendToServer(player, target);
                }
                continue;
            }

            dispatch(player, expanded);
        }
    }

    private static void dispatch(CommandSender sender, String command) {
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        if (sender instanceof Player player) {
            player.performCommand(cmd);
        } else {
            Bukkit.dispatchCommand(sender, cmd);
        }
    }

    /**
     * Sends the player to another server on the proxy network via the
     * BungeeCord plugin message channel. Works on BungeeCord and on
     * Velocity with the legacy plugin message channel enabled.
     */
    private void sendToServer(Player player, String server) {
        String target = server.startsWith("/") ? server.substring(1) : server;
        int space = target.indexOf(' ');
        if (space != -1) {
            target = target.substring(0, space);
        }
        if (target.isEmpty()) {
            return;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("Connect");
            out.writeUTF(target);
            player.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning("[interactiveimage] Failed to send " + player.getName()
                    + " to server '" + target + "': " + e.getMessage());
        }
    }

    private static String applyPlaceholders(String input, Map<String, String> placeholders) {
        String out = input;
        for (var entry : placeholders.entrySet()) {
            out = out.replace(entry.getKey(), entry.getValue());
        }
        return out;
    }
}
