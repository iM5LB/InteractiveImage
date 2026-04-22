package im5lb.interactiveimage.editor;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import im5lb.interactiveimage.InteractiveImage;
import im5lb.interactiveimage.commands.IiConfigEditor;
import im5lb.interactiveimage.config.InteractiveImageConfig;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EditorManager {

    private final InteractiveImage plugin;
    private final Map<UUID, EditorSession> sessions = new ConcurrentHashMap<>();

    public EditorManager(InteractiveImage plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled(Player player) {
        return sessions.getOrDefault(player.getUniqueId(), EditorSession.disabled()).enabled();
    }

    public void setEnabled(Player player, boolean enabled) {
        sessions.compute(player.getUniqueId(), (k, current) -> {
            EditorSession base = current == null ? EditorSession.disabled() : current;
            return base.withEnabled(enabled).clearAwaiting();
        });
    }

    public Optional<EditorSession> get(Player player) {
        return Optional.ofNullable(sessions.get(player.getUniqueId()));
    }

    public void selectTarget(Player player, String providerId, String mapName) {
        sessions.compute(player.getUniqueId(), (k, current) -> {
            EditorSession base = current == null ? EditorSession.disabled().withEnabled(true) : current;
            return base.withTarget(providerId, mapName).clearAwaiting();
        });
    }

    public void openEditor(Player player) {
        EditorSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.enabled() || session.mapName() == null || session.providerId() == null) {
            player.sendMessage("No selection. Look at an ImageFrame and right-click it in edit mode.");
            return;
        }
        if (!"imageframe".equalsIgnoreCase(session.providerId())) {
            player.sendMessage("Only ImageFrame is supported by the editor right now.");
            return;
        }

        openMainForMap(player, session.mapName());
    }

    public void openEffects(Player player) {
        EditorSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.enabled() || session.mapName() == null) {
            openEditor(player);
            return;
        }
        openEffectsForMap(player, session.mapName());
    }

    public void openActivation(Player player) {
        EditorSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.enabled() || session.mapName() == null) {
            openEditor(player);
            return;
        }
        openActivationForMap(player, session.mapName());
    }

    public void beginDialogInput(Player player, EditorInputType type) {
        EditorSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.enabled() || session.mapName() == null) {
            player.sendMessage("No selection. Right-click an ImageFrame item frame in edit mode first.");
            return;
        }
        sessions.computeIfPresent(player.getUniqueId(), (k, cur) -> cur.awaiting(type));

        player.closeInventory();

        String map = session.mapName();
        String initial = initialValueFor(map, type);
        String title = titleFor(type);

        switch (type) {
            case BOSSBAR_PROGRESS -> showNumberRangeDialog(player, type, map, title, 0, 1, 0.05f, initial, 1);
            case HOVER_MAX_DISTANCE, CLICK_MAX_DISTANCE -> showNumberRangeDialog(player, type, map, title, 1, 32, 0.5f, initial, 6);
            case BOSSBAR_COLOR -> showSingleOptionDialog(player, type, map, title, initial, enumNames(BarColor.values()));
            case BOSSBAR_STYLE -> showSingleOptionDialog(player, type, map, title, initial, enumNames(BarStyle.values()));
            case GLOW_COLOR -> showSingleOptionDialog(player, type, map, title, initial, glowColorNames());
            case GLOW_MODE -> showSingleOptionDialog(player, type, map, title, initial, List.of("BLOCK", "FRAME"));
            default -> showTextInputDialog(player, type, map, title, initial);
        }
    }

    private static List<String> glowColorNames() {
        return List.of(
                "WHITE",
                "YELLOW",
                "RED",
                "GREEN",
                "BLUE",
                "AQUA",
                "PURPLE",
                "ORANGE",
                "PINK",
                "GRAY",
                "DARK_GRAY",
                "DARK_RED",
                "DARK_GREEN",
                "DARK_BLUE",
                "BLACK"
        );
    }

    public void shutdown() {
    }

    public Optional<InteractiveImageConfig.MapRule> getRule(String mapName) {
        return plugin.getRuleStore().findImageFrameRuleOrWildcard(mapName);
    }

    private void showTextInputDialog(Player player, EditorInputType type, String mapName, String title, String initial) {
        UUID playerId = player.getUniqueId();

        var input = DialogInput.text("value", Component.text("Value"))
                .width(420)
                .labelVisible(true)
                .initial(initial == null ? "" : initial)
                .maxLength(512)
                .build();

        String bodyText = switch (type) {
            case ADD_RIGHT_ACTION, ADD_LEFT_ACTION -> "Enter an action.\nKeywords: clear, remove-last/undo.";
            default -> "Enter a value.";
        };

        var base = DialogBase.builder(Component.text("interactiveimage - " + title))
                .canCloseWithEscape(false)
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                .body(List.<DialogBody>of(DialogBody.plainMessage(Component.text(bodyText), 420)))
                .inputs(List.of(input))
                .build();

        ClickCallback.Options options = ClickCallback.Options.builder()
                .uses(1)
                .lifetime(Duration.ofMinutes(5))
                .build();

        DialogAction saveAction = DialogAction.customClick((response, audience) -> {
            Player p = Bukkit.getPlayer(playerId);
            if (p == null) {
                return;
            }
            String value = response.getText("value");
            Bukkit.getScheduler().runTask(plugin, () -> handleInput(p, type, mapName, value == null ? "" : value));
        }, options);

        DialogAction cancelAction = DialogAction.customClick((response, audience) -> {
            Player p = Bukkit.getPlayer(playerId);
            if (p == null) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                sessions.computeIfPresent(playerId, (k, cur) -> cur.clearAwaiting());
                openMenuForType(p, type, mapName);
            });
        }, options);

        var yes = ActionButton.builder(Component.text("Save"))
                .width(200)
                .action(saveAction)
                .build();
        var no = ActionButton.builder(Component.text("Cancel"))
                .width(200)
                .action(cancelAction)
                .build();

        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(base)
                .type(DialogType.confirmation(yes, no)));

        Bukkit.getScheduler().runTask(plugin, () -> player.showDialog(dialog));
    }

    private void showNumberRangeDialog(
            Player player,
            EditorInputType type,
            String mapName,
            String title,
            float start,
            float end,
            float step,
            String initialRaw,
            float fallbackInitial
    ) {
        UUID playerId = player.getUniqueId();

        float initial = parseFloatOr(initialRaw, fallbackInitial);

        var inputs = List.of(DialogInput.numberRange("value", Component.text("Value"), start, end)
                .width(420)
                .labelFormat("%s: %s")
                .initial(initial)
                .step(step)
                .build());

        var base = DialogBase.builder(Component.text("interactiveimage - " + title))
                .canCloseWithEscape(false)
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                .body(List.<DialogBody>of(DialogBody.plainMessage(Component.text("Adjust the value."), 420)))
                .inputs(inputs)
                .build();

        ClickCallback.Options options = ClickCallback.Options.builder()
                .uses(1)
                .lifetime(Duration.ofMinutes(5))
                .build();

        DialogAction saveAction = DialogAction.customClick((response, audience) -> {
            Player p = Bukkit.getPlayer(playerId);
            if (p == null) {
                return;
            }
            Float value = response.getFloat("value");
            String valueStr = value == null ? "" : String.valueOf(value);
            Bukkit.getScheduler().runTask(plugin, () -> handleInput(p, type, mapName, valueStr));
        }, options);

        DialogAction cancelAction = DialogAction.customClick((response, audience) -> {
            Player p = Bukkit.getPlayer(playerId);
            if (p == null) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                sessions.computeIfPresent(playerId, (k, cur) -> cur.clearAwaiting());
                openMenuForType(p, type, mapName);
            });
        }, options);

        var yes = ActionButton.builder(Component.text("Save"))
                .width(200)
                .action(saveAction)
                .build();
        var no = ActionButton.builder(Component.text("Cancel"))
                .width(200)
                .action(cancelAction)
                .build();

        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(base)
                .type(DialogType.confirmation(yes, no)));

        Bukkit.getScheduler().runTask(plugin, () -> player.showDialog(dialog));
    }

    private void showSingleOptionDialog(
            Player player,
            EditorInputType type,
            String mapName,
            String title,
            String initialRaw,
            List<String> optionsList
    ) {
        UUID playerId = player.getUniqueId();

        String initialId = initialRaw == null ? "" : initialRaw.toUpperCase();
        var entries = new ArrayList<SingleOptionDialogInput.OptionEntry>();
        for (String opt : optionsList) {
            String id = opt.toUpperCase();
            entries.add(SingleOptionDialogInput.OptionEntry.create(id, Component.text(id), id.equalsIgnoreCase(initialId)));
        }

        var input = DialogInput.singleOption("value", Component.text("Value"), entries)
                .width(420)
                .labelVisible(true)
                .build();

        var base = DialogBase.builder(Component.text("interactiveimage - " + title))
                .canCloseWithEscape(false)
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                .body(List.<DialogBody>of(DialogBody.plainMessage(Component.text("Select a value."), 420)))
                .inputs(List.of(input))
                .build();

        ClickCallback.Options options = ClickCallback.Options.builder()
                .uses(1)
                .lifetime(Duration.ofMinutes(5))
                .build();

        DialogAction saveAction = DialogAction.customClick((response, audience) -> {
            Player p = Bukkit.getPlayer(playerId);
            if (p == null) {
                return;
            }
            String selected = response.getText("value");
            Bukkit.getScheduler().runTask(plugin, () -> handleInput(p, type, mapName, selected == null ? "" : selected));
        }, options);

        DialogAction cancelAction = DialogAction.customClick((response, audience) -> {
            Player p = Bukkit.getPlayer(playerId);
            if (p == null) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                sessions.computeIfPresent(playerId, (k, cur) -> cur.clearAwaiting());
                openMenuForType(p, type, mapName);
            });
        }, options);

        var yes = ActionButton.builder(Component.text("Save"))
                .width(200)
                .action(saveAction)
                .build();
        var no = ActionButton.builder(Component.text("Cancel"))
                .width(200)
                .action(cancelAction)
                .build();

        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(base)
                .type(DialogType.confirmation(yes, no)));

        Bukkit.getScheduler().runTask(plugin, () -> player.showDialog(dialog));
    }

    private static float parseFloatOr(String raw, float fallback) {
        if (raw == null) {
            return fallback;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return fallback;
        }
        try {
            return Float.parseFloat(t);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static List<String> enumNames(Enum<?>[] values) {
        var out = new ArrayList<String>(values.length);
        for (Enum<?> v : values) {
            out.add(v.name());
        }
        return List.copyOf(out);
    }

    private String titleFor(EditorInputType type) {
        return switch (type) {
            case TITLE -> "Title";
            case ADD_RIGHT_ACTION -> "Add Right Action";
            case ADD_LEFT_ACTION -> "Add Left Action";
            case GLOW_COLOR -> "Glow Color";
            case GLOW_MODE -> "Glow Mode";
            case ACTIONBAR_FORMAT -> "ActionBar Format";
            case TITLE_TITLE -> "Title Line";
            case TITLE_SUBTITLE -> "Subtitle Line";
            case BOSSBAR_TEXT -> "BossBar Text";
            case BOSSBAR_COLOR -> "BossBar Color";
            case BOSSBAR_STYLE -> "BossBar Style";
            case BOSSBAR_PROGRESS -> "BossBar Progress";
            case HOVER_MAX_DISTANCE -> "Hover Range";
            case CLICK_MAX_DISTANCE -> "Click Range";
        };
    }

    private String initialValueFor(String mapName, EditorInputType type) {
        Optional<InteractiveImageConfig.MapRule> ruleOpt = plugin.getRuleStore().findImageFrameRule(mapName);
        InteractiveImageConfig.MapRule rule = ruleOpt.orElse(null);
        InteractiveImageConfig global = plugin.getConfigModel();
        return switch (type) {
            case TITLE -> rule == null ? mapName : rule.title();
            case ADD_RIGHT_ACTION -> "console:say hello";
            case ADD_LEFT_ACTION -> "player:say hello";
            case GLOW_COLOR -> rule == null || rule.effects().glowColor() == null ? "WHITE" : rule.effects().glowColor();
            case GLOW_MODE -> rule == null || rule.effects().glowMode() == null ? "BLOCK" : rule.effects().glowMode();
            case ACTIONBAR_FORMAT -> rule == null || rule.effects().actionBar().format() == null ? global.effects().actionBar().format() : rule.effects().actionBar().format();
            case TITLE_TITLE -> rule == null || rule.effects().title().title() == null ? global.effects().title().title() : rule.effects().title().title();
            case TITLE_SUBTITLE -> rule == null || rule.effects().title().subtitle() == null ? global.effects().title().subtitle() : rule.effects().title().subtitle();
            case BOSSBAR_TEXT -> rule == null || rule.effects().bossBar().text() == null ? global.effects().bossBar().text() : rule.effects().bossBar().text();
            case BOSSBAR_COLOR -> rule == null || rule.effects().bossBar().color() == null ? global.effects().bossBar().color() : rule.effects().bossBar().color();
            case BOSSBAR_STYLE -> rule == null || rule.effects().bossBar().style() == null ? global.effects().bossBar().style() : rule.effects().bossBar().style();
            case BOSSBAR_PROGRESS -> String.valueOf(rule == null || rule.effects().bossBar().progress() == null ? global.effects().bossBar().progress() : rule.effects().bossBar().progress());
            case HOVER_MAX_DISTANCE -> String.valueOf(rule == null || rule.activation().hoverMaxDistance() == null ? global.activation().hover().maxDistance() : rule.activation().hoverMaxDistance());
            case CLICK_MAX_DISTANCE -> String.valueOf(rule == null || rule.activation().clickMaxDistance() == null ? global.activation().click().maxDistance() : rule.activation().clickMaxDistance());
        };
    }

    private void handleInput(Player player, EditorInputType type, String mapName, String message) {
        UUID playerId = player.getUniqueId();
        EditorSession session = sessions.get(playerId);
        if (session == null || !session.enabled()) {
            return;
        }
        sessions.computeIfPresent(playerId, (k, cur) -> cur.clearAwaiting());

        String trimmed = message == null ? "" : message.trim();
        if (trimmed.equalsIgnoreCase("cancel")) {
            openMenuForType(player, type, mapName);
            return;
        }
        boolean clear = trimmed.equalsIgnoreCase("clear");
        boolean removeLast = trimmed.equalsIgnoreCase("remove-last") || trimmed.equalsIgnoreCase("removelast") || trimmed.equalsIgnoreCase("undo");

        if (type == EditorInputType.TITLE) {
            if (trimmed.isEmpty()) {
                player.sendMessage("Title cannot be empty.");
                openMainForMap(player, mapName);
                return;
            }
            IiConfigEditor.setImageFrameTitle(plugin, mapName, trimmed);
        } else if (type == EditorInputType.ADD_RIGHT_ACTION) {
            if (clear) {
                IiConfigEditor.clearSide(plugin, mapName, "right");
            } else if (removeLast) {
                IiConfigEditor.removeLastSideAction(plugin, mapName, "right");
            } else if (!trimmed.isEmpty()) {
                IiConfigEditor.addSideAction(plugin, mapName, "right", trimmed);
            }
        } else if (type == EditorInputType.ADD_LEFT_ACTION) {
            if (clear) {
                IiConfigEditor.clearSide(plugin, mapName, "left");
            } else if (removeLast) {
                IiConfigEditor.removeLastSideAction(plugin, mapName, "left");
            } else if (!trimmed.isEmpty()) {
                IiConfigEditor.addSideAction(plugin, mapName, "left", trimmed);
            }
        } else if (type == EditorInputType.ACTIONBAR_FORMAT) {
            IiConfigEditor.setOptionalString(plugin, mapName, "effects.actionBar.format", trimmed);
        } else if (type == EditorInputType.TITLE_TITLE) {
            IiConfigEditor.setOptionalString(plugin, mapName, "effects.title.title", trimmed);
        } else if (type == EditorInputType.TITLE_SUBTITLE) {
            IiConfigEditor.setOptionalString(plugin, mapName, "effects.title.subtitle", trimmed);
        } else if (type == EditorInputType.BOSSBAR_TEXT) {
            IiConfigEditor.setOptionalString(plugin, mapName, "effects.bossBar.text", trimmed);
        } else if (type == EditorInputType.BOSSBAR_COLOR) {
            IiConfigEditor.setOptionalString(plugin, mapName, "effects.bossBar.color", trimmed);
        } else if (type == EditorInputType.BOSSBAR_STYLE) {
            IiConfigEditor.setOptionalString(plugin, mapName, "effects.bossBar.style", trimmed);
        } else if (type == EditorInputType.BOSSBAR_PROGRESS) {
            double value;
            try {
                value = Double.parseDouble(trimmed);
            } catch (NumberFormatException e) {
                player.sendMessage("Not a number.");
                openEffectsForMap(player, mapName);
                return;
            }
            if (value < 0.0 || value > 1.0) {
                player.sendMessage("Progress must be between 0.0 and 1.0.");
                openEffectsForMap(player, mapName);
                return;
            }
            IiConfigEditor.setOptionalDouble(plugin, mapName, "effects.bossBar.progress", value);
        } else if (type == EditorInputType.GLOW_COLOR) {
            if (trimmed.isEmpty()) {
                openEffectsForMap(player, mapName, EditorGui.EffectTab.GLOW);
                return;
            }
            IiConfigEditor.setOptionalString(plugin, mapName, "effects.glow.color", trimmed.toUpperCase());
        } else if (type == EditorInputType.GLOW_MODE) {
            if (trimmed.isEmpty()) {
                openEffectsForMap(player, mapName, EditorGui.EffectTab.GLOW);
                return;
            }
            IiConfigEditor.setOptionalString(plugin, mapName, "effects.glow.mode", trimmed.toUpperCase());
            plugin.clearFocusedImageFrameMap(mapName);
        } else if (type == EditorInputType.HOVER_MAX_DISTANCE) {
            double value = parseDoublePositive(player, trimmed);
            if (value <= 0.0) {
                openActivationForMap(player, mapName);
                return;
            }
            IiConfigEditor.setOptionalDouble(plugin, mapName, "activation.hover.maxDistance", value);
        } else if (type == EditorInputType.CLICK_MAX_DISTANCE) {
            double value = parseDoublePositive(player, trimmed);
            if (value <= 0.0) {
                openActivationForMap(player, mapName);
                return;
            }
            IiConfigEditor.setOptionalDouble(plugin, mapName, "activation.click.maxDistance", value);
        }

        openMenuForType(player, type, mapName);
    }

    private void openMenuForType(Player player, EditorInputType type, String mapName) {
        if (type == EditorInputType.GLOW_COLOR) {
            openEffectsForMap(player, mapName, EditorGui.EffectTab.GLOW);
            return;
        }
        if (type == EditorInputType.GLOW_MODE) {
            openEffectsForMap(player, mapName, EditorGui.EffectTab.GLOW);
            return;
        }
        if (type == EditorInputType.ACTIONBAR_FORMAT) {
            openEffectsForMap(player, mapName, EditorGui.EffectTab.ACTIONBAR);
            return;
        }
        if (type == EditorInputType.TITLE_TITLE || type == EditorInputType.TITLE_SUBTITLE) {
            openEffectsForMap(player, mapName, EditorGui.EffectTab.TITLE);
            return;
        }
        if (type == EditorInputType.BOSSBAR_TEXT
                || type == EditorInputType.BOSSBAR_COLOR
                || type == EditorInputType.BOSSBAR_STYLE
                || type == EditorInputType.BOSSBAR_PROGRESS) {
            openEffectsForMap(player, mapName, EditorGui.EffectTab.BOSSBAR);
            return;
        }
        if (type == EditorInputType.HOVER_MAX_DISTANCE
                || type == EditorInputType.CLICK_MAX_DISTANCE) {
            openActivationForMap(player, mapName);
            return;
        }
        openMainForMap(player, mapName);
    }

    private void openMainForMap(Player player, String mapName) {
        IiConfigEditor.ensureImageFrameRuleExists(plugin, mapName);
        player.openInventory(EditorGui.createMain(plugin, mapName));
    }

    private void openEffectsForMap(Player player, String mapName) {
        IiConfigEditor.ensureImageFrameRuleExists(plugin, mapName);
        player.openInventory(EditorGui.createEffects(plugin, mapName));
    }

    private void openEffectsForMap(Player player, String mapName, EditorGui.EffectTab tab) {
        IiConfigEditor.ensureImageFrameRuleExists(plugin, mapName);
        player.openInventory(EditorGui.createEffects(plugin, mapName, tab));
    }

    private void openActivationForMap(Player player, String mapName) {
        IiConfigEditor.ensureImageFrameRuleExists(plugin, mapName);
        player.openInventory(EditorGui.createActivation(plugin, mapName));
    }

    private static int parseInt(Player player, String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            // Paper dialog numberRange returns floats like "20.0".
            // Accept those for integer fields (ticks, etc).
            try {
                double d = Double.parseDouble(input);
                if (!Double.isFinite(d)) {
                    player.sendMessage("Not a number.");
                    return Integer.MIN_VALUE;
                }
                if (d > Integer.MAX_VALUE || d < Integer.MIN_VALUE) {
                    player.sendMessage("Number out of range.");
                    return Integer.MIN_VALUE;
                }
                return (int) Math.round(d);
            } catch (NumberFormatException ignored) {
                player.sendMessage("Not a number.");
                return Integer.MIN_VALUE;
            }
        }
    }

    private static double parseDoublePositive(Player player, String input) {
        double value;
        try {
            value = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            player.sendMessage("Not a number.");
            return -1.0;
        }
        if (value <= 0.0) {
            player.sendMessage("Value must be > 0.");
            return -1.0;
        }
        return value;
    }

    private void setNonNegativeInt(Player player, String mapName, String relativePath, String trimmed) {
        int ticks = parseInt(player, trimmed);
        if (ticks == Integer.MIN_VALUE) {
            return;
        }
        if (ticks < 0) {
            player.sendMessage("Ticks must be >= 0.");
            return;
        }
        IiConfigEditor.setOptionalInt(plugin, mapName, relativePath, ticks);
    }
}

