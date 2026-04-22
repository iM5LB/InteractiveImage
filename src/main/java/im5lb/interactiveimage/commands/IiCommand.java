package im5lb.interactiveimage.commands;

import im5lb.interactiveimage.InteractiveImage;
import im5lb.interactiveimage.editor.EditorManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class IiCommand implements CommandExecutor, TabCompleter {

    private final InteractiveImage plugin;
    private final EditorManager editorManager;

    public IiCommand(InteractiveImage plugin, EditorManager editorManager) {
        this.plugin = plugin;
        this.editorManager = editorManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && "reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("interactiveimage.admin")) {
                sender.sendMessage("No permission.");
                return true;
            }
            plugin.reloadAndRestart();
            sender.sendMessage("InteractiveImage reloaded.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Usage: /" + label + " reload");
            return true;
        }
        if (!sender.hasPermission("interactiveimage.admin")) {
            sender.sendMessage("No permission.");
            return true;
        }
        if (editorManager == null) {
            player.sendMessage("Editor is not available.");
            return true;
        }

        if (args.length == 0) {
            boolean enabled = !editorManager.isEnabled(player);
            editorManager.setEnabled(player, enabled);
            player.sendMessage("Editor mode: " + (enabled ? "ON" : "OFF"));
            if (enabled) {
                player.sendMessage("Right-click an ImageFrame item frame to open the editor GUI.");
            }
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("on".equals(sub) || "off".equals(sub)) {
            editorManager.setEnabled(player, "on".equals(sub));
            player.sendMessage("Editor mode: " + ("on".equals(sub) ? "ON" : "OFF"));
            if ("on".equals(sub)) {
                player.sendMessage("Right-click an ImageFrame item frame to open the editor GUI.");
            }
            return true;
        }

        player.sendMessage("Usage: /" + label + " [on|off] | /" + label + " reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partial(args[0], List.of("on", "off", "reload"));
        }
        return List.of();
    }

    private static List<String> partial(String token, List<String> options) {
        if (token == null || token.isEmpty()) {
            return options;
        }
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(opt);
            }
        }
        return out;
    }

}
