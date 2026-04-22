package im5lb.interactiveimage.editor;

import im5lb.interactiveimage.InteractiveImage;
import im5lb.interactiveimage.config.InteractiveImageConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EditorGui {

    public static final String TITLE_PREFIX = ChatColor.DARK_AQUA + "iimage: " + ChatColor.AQUA;

    private static final int SIZE = 54;
    private static final int SLOT_HEADER = 4;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_EXIT = 49;
    private static final int SLOT_DELETE = 53;

    private EditorGui() {
    }

    public enum EffectTab {
        GLOW,
        ACTIONBAR,
        TITLE,
        BOSSBAR
    }

    public static Inventory createMain(InteractiveImage plugin, String mapName) {
        Inventory inventory = Bukkit.createInventory(new MainHolder(mapName), SIZE, TITLE_PREFIX + mapName);

        Optional<InteractiveImageConfig.MapRule> ruleOpt = plugin.getRuleStore().findImageFrameRule(mapName);
        InteractiveImageConfig.MapRule rule = ruleOpt.orElse(null);
        drawFrame(inventory);
        inventory.setItem(SLOT_HEADER, header(mapName));

        boolean cancelInteract = rule == null || rule.cancelInteract();

        inventory.setItem(20, item(Material.BARRIER, ChatColor.YELLOW + "Cancel Interact",
                lore("Current: ", String.valueOf(cancelInteract)),
                hint("Click to toggle")
        ));

        inventory.setItem(22, item(Material.COMPARATOR, ChatColor.GOLD + "Effects",
                hint("Open effects settings")
        ));
        inventory.setItem(23, item(Material.REPEATER, ChatColor.GOLD + "Activation",
                hint("Open activation settings")
        ));

        List<String> rightActions = rule == null ? List.of() : rule.onRightClick();
        List<String> leftActions = rule == null ? List.of() : rule.onLeftClick();

        inventory.setItem(30, item(Material.LIME_DYE, ChatColor.GREEN + "Right-click Actions",
                lore("Count: ", String.valueOf(rightActions.size())),
                sampleActions(rightActions),
                hint("Click to add")
        ));
        inventory.setItem(32, item(Material.LIGHT_BLUE_DYE, ChatColor.AQUA + "Left-click Actions",
                lore("Count: ", String.valueOf(leftActions.size())),
                sampleActions(leftActions),
                hint("Click to add")
        ));

        inventory.setItem(39, item(Material.RED_DYE, ChatColor.RED + "Clear Right Actions",
                hint("Removes all right-click actions")
        ));
        inventory.setItem(41, item(Material.RED_DYE, ChatColor.RED + "Clear Left Actions",
                hint("Removes all left-click actions")
        ));

        inventory.setItem(SLOT_EXIT, item(Material.STRUCTURE_VOID, ChatColor.GRAY + "Exit",
                hint("Close this menu")
        ));
        inventory.setItem(SLOT_DELETE, item(Material.LAVA_BUCKET, ChatColor.RED + "Delete Rule",
                hint("Shift-click to delete this map rule")
        ));

        return inventory;
    }

    public static Inventory createEffects(InteractiveImage plugin, String mapName) {
        return createEffects(plugin, mapName, EffectTab.GLOW);
    }

    public static Inventory createEffects(InteractiveImage plugin, String mapName, EffectTab tab) {
        Inventory inventory = Bukkit.createInventory(new EffectsHolder(mapName, tab), SIZE, TITLE_PREFIX + mapName + ChatColor.DARK_GRAY + " » Effects");

        Optional<InteractiveImageConfig.MapRule> ruleOpt = plugin.getRuleStore().findImageFrameRule(mapName);
        InteractiveImageConfig.MapRule rule = ruleOpt.orElse(null);
        InteractiveImageConfig.MapEffects effects = rule == null ? InteractiveImageConfig.MapEffects.empty() : rule.effects();
        InteractiveImageConfig global = plugin.getConfigModel();

        drawFrame(inventory);
        inventory.setItem(SLOT_HEADER, header(mapName));

        boolean glowEnabled = effects.glow().orElse(global.effects().glow().enabled());
        boolean actionBarEnabled = effects.actionBar().enabled().orElse(global.effects().actionBar().enabled());
        boolean titleEnabled = effects.title().enabled().orElse(global.effects().title().enabled());
        boolean bossEnabled = effects.bossBar().enabled().orElse(global.effects().bossBar().enabled());

        inventory.setItem(10, navTab(Material.GLOW_INK_SAC, "Glow", tab == EffectTab.GLOW, glowEnabled));
        inventory.setItem(12, navTab(Material.OAK_SIGN, "ActionBar", tab == EffectTab.ACTIONBAR, actionBarEnabled));
        inventory.setItem(14, navTab(Material.PAPER, "Title", tab == EffectTab.TITLE, titleEnabled));
        inventory.setItem(16, navTab(Material.DRAGON_BREATH, "BossBar", tab == EffectTab.BOSSBAR, bossEnabled));

        if (tab == EffectTab.GLOW) {
            String glowColor = effects.glowColor() == null ? "WHITE" : effects.glowColor();
            String glowMode = effects.glowMode() == null ? "BLOCK" : effects.glowMode();
            boolean frameVisible = effects.frameVisible() == null || effects.frameVisible().isInherit() || effects.frameVisible().orElse(true);
            inventory.setItem(28, item(Material.GLOW_INK_SAC, ChatColor.YELLOW + "Enabled",
                    lore("Current: ", String.valueOf(glowEnabled)),
                    hint("Click to toggle")
            ));
            inventory.setItem(29, item(Material.PAINTING, ChatColor.AQUA + "Glow Color",
                    lore("Current: ", glowColor),
                    hint("Click to select")
            ));
            inventory.setItem(30, item(Material.ITEM_FRAME, ChatColor.AQUA + "Glow Mode",
                    lore("Current: ", glowMode),
                    hint("Click to select")
            ));
            inventory.setItem(31, item(Material.LEAD, ChatColor.AQUA + "ItemFrame Visible",
                    lore("Current: ", String.valueOf(frameVisible)),
                    hint("Click to toggle")
            ));
        }

        if (tab == EffectTab.ACTIONBAR) {
            String format = effects.actionBar().format() == null ? global.effects().actionBar().format() : effects.actionBar().format();
            inventory.setItem(28, item(Material.OAK_SIGN, ChatColor.YELLOW + "Enabled",
                    lore("Current: ", String.valueOf(actionBarEnabled)),
                    hint("Click to toggle")
            ));
            inventory.setItem(29, item(Material.WRITABLE_BOOK, ChatColor.AQUA + "Text",
                    lore("Current: ", format == null ? "" : format),
                    hint("Click to edit")
            ));
        }

        if (tab == EffectTab.TITLE) {
            String title = effects.title().title() == null ? global.effects().title().title() : effects.title().title();
            String subtitle = effects.title().subtitle() == null ? global.effects().title().subtitle() : effects.title().subtitle();

            inventory.setItem(28, item(Material.PAPER, ChatColor.YELLOW + "Enabled",
                    lore("Current: ", String.valueOf(titleEnabled)),
                    hint("Click to toggle")
            ));
            inventory.setItem(29, item(Material.WRITABLE_BOOK, ChatColor.AQUA + "Title Text",
                    lore("Current: ", title == null ? "" : title),
                    hint("Click to edit")
            ));
            inventory.setItem(30, item(Material.WRITABLE_BOOK, ChatColor.AQUA + "Subtitle Text",
                    lore("Current: ", subtitle == null ? "" : subtitle),
                    hint("Click to edit")
            ));
        }

        if (tab == EffectTab.BOSSBAR) {
            String text = effects.bossBar().text() == null ? global.effects().bossBar().text() : effects.bossBar().text();
            double progress = effects.bossBar().progress() == null ? global.effects().bossBar().progress() : effects.bossBar().progress();
            String color = effects.bossBar().color() == null ? global.effects().bossBar().color() : effects.bossBar().color();
            String style = effects.bossBar().style() == null ? global.effects().bossBar().style() : effects.bossBar().style();

            inventory.setItem(28, item(Material.DRAGON_BREATH, ChatColor.YELLOW + "Enabled",
                    lore("Current: ", String.valueOf(bossEnabled)),
                    hint("Click to toggle")
            ));
            inventory.setItem(29, item(Material.WRITABLE_BOOK, ChatColor.AQUA + "Text",
                    lore("Current: ", text == null ? "" : text),
                    hint("Click to edit")
            ));
            inventory.setItem(30, item(Material.EXPERIENCE_BOTTLE, ChatColor.AQUA + "Progress",
                    lore("Current: ", String.valueOf(progress)),
                    hint("Click to edit")
            ));
            inventory.setItem(32, item(Material.YELLOW_DYE, ChatColor.AQUA + "Color",
                    lore("Current: ", color),
                    hint("Click to select")
            ));
            inventory.setItem(33, item(Material.IRON_BARS, ChatColor.AQUA + "Style",
                    lore("Current: ", style),
                    hint("Click to select")
            ));
        }

        inventory.setItem(SLOT_BACK, item(Material.ARROW, ChatColor.GRAY + "Back", hint("Return to main menu")));
        return inventory;
    }

    public static Inventory createActivation(InteractiveImage plugin, String mapName) {
        Inventory inventory = Bukkit.createInventory(new ActivationHolder(mapName), SIZE, TITLE_PREFIX + mapName + ChatColor.DARK_GRAY + " » Activation");

        Optional<InteractiveImageConfig.MapRule> ruleOpt = plugin.getRuleStore().findImageFrameRule(mapName);
        InteractiveImageConfig.MapRule rule = ruleOpt.orElse(null);
        InteractiveImageConfig.MapActivation act = rule == null ? InteractiveImageConfig.MapActivation.inherit() : rule.activation();
        InteractiveImageConfig global = plugin.getConfigModel();

        drawFrame(inventory);
        inventory.setItem(SLOT_HEADER, header(mapName));

        double hoverMax = act.hoverMaxDistance() == null ? global.activation().hover().maxDistance() : act.hoverMaxDistance();
        double clickMax = act.clickMaxDistance() == null ? global.activation().click().maxDistance() : act.clickMaxDistance();

        inventory.setItem(10, item(Material.SPYGLASS, ChatColor.YELLOW + "Hover Max Distance",
                lore("Current: ", String.valueOf(hoverMax)),
                hint("Click to edit")
        ));
        inventory.setItem(11, item(Material.LEAD, ChatColor.YELLOW + "Click Max Distance",
                lore("Current: ", String.valueOf(clickMax)),
                hint("Click to edit")
        ));
        inventory.setItem(12, boolToggle(Material.TARGET, ChatColor.YELLOW + "Require Hover For Click",
                act.requireHoverForClick(),
                global.activation().click().requireHover()
        ));

        inventory.setItem(SLOT_BACK, item(Material.ARROW, ChatColor.GRAY + "Back", hint("Return to main menu")));
        return inventory;
    }

    private static ItemStack navTab(Material material, String name, boolean selected, boolean enabled) {
        String title = (selected ? ChatColor.GOLD : ChatColor.YELLOW) + name;
        return item(material, title,
                lore("Enabled: ", String.valueOf(enabled)),
                hint("Click to open")
        );
    }

    @SafeVarargs
    private static ItemStack item(Material material, String name, List<String>... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        List<String> lore = new ArrayList<>();
        for (List<String> lines : loreLines) {
            lore.addAll(lines);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack header(String mapName) {
        return item(Material.FILLED_MAP, ChatColor.AQUA + "Selected",
                lore("Map: ", mapName),
                hint("Edit settings below")
        );
    }

    private static ItemStack boolToggle(Material material, String name, InteractiveImageConfig.OptionalBoolean value, boolean fallback) {
        boolean state = value == null || value.isInherit() ? fallback : value.value();
        return item(material, name,
                lore("Current: ", String.valueOf(state)),
                hint("Click to toggle")
        );
    }

    private static List<String> lore(String key, String value) {
        return List.of(ChatColor.GRAY + key + ChatColor.WHITE + value);
    }

    private static List<String> hint(String line) {
        return List.of(ChatColor.DARK_GRAY + line);
    }

    private static List<String> sampleActions(List<String> actions) {
        List<String> lore = new ArrayList<>();
        int limit = Math.min(3, actions.size());
        for (int i = 0; i < limit; i++) {
            lore.add(ChatColor.GRAY + "- " + ChatColor.WHITE + actions.get(i));
        }
        if (actions.size() > limit) {
            lore.add(ChatColor.DARK_GRAY + "... (" + actions.size() + " total)");
        }
        return lore;
    }

    private static void drawFrame(Inventory inventory) {
        ItemStack border = pane(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack corners = pane(Material.CYAN_STAINED_GLASS_PANE);

        int size = inventory.getSize();
        int rows = size / 9;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 9; c++) {
                boolean edge = r == 0 || r == rows - 1 || c == 0 || c == 8;
                if (!edge) {
                    continue;
                }
                int slot = r * 9 + c;
                boolean corner = (r == 0 || r == rows - 1) && (c == 0 || c == 8);
                inventory.setItem(slot, corner ? corners : border);
            }
        }
    }

    private static ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    public record MainHolder(String mapName) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record EffectsHolder(String mapName, EffectTab tab) implements InventoryHolder {
        public EffectsHolder(String mapName) {
            this(mapName, EffectTab.GLOW);
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record ActivationHolder(String mapName) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}

