package im5lb.interactiveimage.editor;

import im5lb.interactiveimage.InteractiveImage;
import im5lb.interactiveimage.commands.IiConfigEditor;
import im5lb.interactiveimage.config.InteractiveImageConfig;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class EditorListener implements Listener {

    private final InteractiveImage plugin;
    private final EditorManager editorManager;

    public EditorListener(InteractiveImage plugin, EditorManager editorManager) {
        this.plugin = plugin;
        this.editorManager = editorManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof EditorGui.MainHolder holder) {
            handleMainClick(event, player, inventory, holder);
            return;
        }
        if (inventory.getHolder() instanceof EditorGui.EffectsHolder holder) {
            handleEffectsClick(event, player, inventory, holder);
            return;
        }
        if (inventory.getHolder() instanceof EditorGui.ActivationHolder holder) {
            handleActivationClick(event, player, inventory, holder);
        }
    }

    private void handleMainClick(InventoryClickEvent event, Player player, Inventory inventory, EditorGui.MainHolder holder) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        event.setCancelled(true);

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) {
            return;
        }

        String mapName = holder.mapName();

        // Cooldown is always default (no tick setting).
        if (slot == 20) {
            Optional<InteractiveImageConfig.MapRule> ruleOpt = plugin.getRuleStore().findImageFrameRule(mapName);
            boolean currentValue = ruleOpt.map(InteractiveImageConfig.MapRule::cancelInteract).orElse(true);
            IiConfigEditor.setCancelInteract(plugin, mapName, !currentValue);
            player.openInventory(EditorGui.createMain(plugin, mapName));
            return;
        }
        if (slot == 30) {
            editorManager.beginDialogInput(player, EditorInputType.ADD_RIGHT_ACTION);
            return;
        }
        if (slot == 32) {
            editorManager.beginDialogInput(player, EditorInputType.ADD_LEFT_ACTION);
            return;
        }
        if (slot == 22) {
            player.openInventory(EditorGui.createEffects(plugin, mapName));
            return;
        }
        if (slot == 23) {
            player.openInventory(EditorGui.createActivation(plugin, mapName));
            return;
        }
        if (slot == 39) {
            IiConfigEditor.clearSide(plugin, mapName, "right");
            player.openInventory(EditorGui.createMain(plugin, mapName));
            return;
        }
        if (slot == 41) {
            IiConfigEditor.clearSide(plugin, mapName, "left");
            player.openInventory(EditorGui.createMain(plugin, mapName));
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (slot == 53) {
            if (!event.isShiftClick()) {
                player.sendMessage("Shift-click to delete this rule.");
                return;
            }
            boolean deleted = plugin.getRuleStore().deleteImageFrameRule(mapName);
            if (deleted) {
                player.sendMessage("Deleted rule for: " + mapName);
                plugin.clearFocusedImageFrameMap(mapName);
            } else {
                player.sendMessage("No rule found for: " + mapName);
            }
            player.closeInventory();
        }
    }

    private void handleEffectsClick(InventoryClickEvent event, Player player, Inventory inventory, EditorGui.EffectsHolder holder) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        event.setCancelled(true);

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) {
            return;
        }

        String mapName = holder.mapName();
        var tab = holder.tab();

        if (slot == 45) {
            player.openInventory(EditorGui.createMain(plugin, mapName));
            return;
        }

        if (slot == 10) {
            player.openInventory(EditorGui.createEffects(plugin, mapName, EditorGui.EffectTab.GLOW));
            return;
        }
        if (slot == 12) {
            player.openInventory(EditorGui.createEffects(plugin, mapName, EditorGui.EffectTab.ACTIONBAR));
            return;
        }
        if (slot == 14) {
            player.openInventory(EditorGui.createEffects(plugin, mapName, EditorGui.EffectTab.TITLE));
            return;
        }
        if (slot == 16) {
            player.openInventory(EditorGui.createEffects(plugin, mapName, EditorGui.EffectTab.BOSSBAR));
            return;
        }

        if (tab == EditorGui.EffectTab.GLOW) {
            if (slot == 28) {
                boolean next = IiConfigEditor.toggleBooleanNoInherit(plugin, mapName, "effects.glow", plugin.getConfigModel().effects().glow().enabled());
                if (!next) {
                    plugin.clearFocusedImageFrameMap(mapName);
                }
                player.openInventory(EditorGui.createEffects(plugin, mapName, tab));
                return;
            }
            if (slot == 29) {
                editorManager.beginDialogInput(player, EditorInputType.GLOW_COLOR);
                return;
            }
            if (slot == 30) {
                editorManager.beginDialogInput(player, EditorInputType.GLOW_MODE);
                return;
            }
            if (slot == 31) {
                IiConfigEditor.toggleBooleanNoInherit(plugin, mapName, "effects.frameVisible", true);
                player.openInventory(EditorGui.createEffects(plugin, mapName, tab));
            }
            return;
        }

        if (tab == EditorGui.EffectTab.ACTIONBAR) {
            if (slot == 28) {
                IiConfigEditor.toggleBooleanNoInherit(plugin, mapName, "effects.actionBar.enabled", plugin.getConfigModel().effects().actionBar().enabled());
                player.openInventory(EditorGui.createEffects(plugin, mapName, tab));
                return;
            }
            if (slot == 29) {
                editorManager.beginDialogInput(player, EditorInputType.ACTIONBAR_FORMAT);
                return;
            }
            return;
        }

        if (tab == EditorGui.EffectTab.TITLE) {
            if (slot == 28) {
                boolean next = IiConfigEditor.toggleBooleanNoInherit(plugin, mapName, "effects.title.enabled", plugin.getConfigModel().effects().title().enabled());
                if (!next) {
                    plugin.clearFocusedImageFrameMap(mapName);
                }
                player.openInventory(EditorGui.createEffects(plugin, mapName, tab));
                return;
            }
            if (slot == 29) {
                editorManager.beginDialogInput(player, EditorInputType.TITLE_TITLE);
                return;
            }
            if (slot == 30) {
                editorManager.beginDialogInput(player, EditorInputType.TITLE_SUBTITLE);
                return;
            }
            return;
        }

        if (tab == EditorGui.EffectTab.BOSSBAR) {
            if (slot == 28) {
                IiConfigEditor.toggleBooleanNoInherit(plugin, mapName, "effects.bossBar.enabled", plugin.getConfigModel().effects().bossBar().enabled());
                player.openInventory(EditorGui.createEffects(plugin, mapName, tab));
                return;
            }
            if (slot == 29) {
                editorManager.beginDialogInput(player, EditorInputType.BOSSBAR_TEXT);
                return;
            }
            if (slot == 30) {
                editorManager.beginDialogInput(player, EditorInputType.BOSSBAR_PROGRESS);
                return;
            }
            if (slot == 32) {
                editorManager.beginDialogInput(player, EditorInputType.BOSSBAR_COLOR);
                return;
            }
            if (slot == 33) {
                editorManager.beginDialogInput(player, EditorInputType.BOSSBAR_STYLE);
            }
        }
    }

    private void handleActivationClick(InventoryClickEvent event, Player player, Inventory inventory, EditorGui.ActivationHolder holder) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        event.setCancelled(true);

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) {
            return;
        }

        String mapName = holder.mapName();

        if (slot == 45) {
            player.openInventory(EditorGui.createMain(plugin, mapName));
            return;
        }

        if (slot == 10) {
            editorManager.beginDialogInput(player, EditorInputType.HOVER_MAX_DISTANCE);
            return;
        }
        if (slot == 11) {
            editorManager.beginDialogInput(player, EditorInputType.CLICK_MAX_DISTANCE);
            return;
        }
        if (slot == 12) {
            IiConfigEditor.toggleBooleanNoInherit(plugin, mapName, "activation.click.requireHover", plugin.getConfigModel().activation().click().requireHover());
            player.openInventory(EditorGui.createActivation(plugin, mapName));
        }
    }
}

