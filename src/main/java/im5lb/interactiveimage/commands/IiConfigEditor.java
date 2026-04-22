package im5lb.interactiveimage.commands;

import im5lb.interactiveimage.InteractiveImage;
import im5lb.interactiveimage.config.InteractiveImageConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class IiConfigEditor {

    private IiConfigEditor() {}

    public static boolean ensureImageFrameRuleExists(InteractiveImage plugin, String mapKey) {
        Objects.requireNonNull(mapKey, "mapKey");
        Optional<InteractiveImageConfig.MapRule> existing = plugin.getRuleStore().findImageFrameRule(mapKey);
        if (existing.isPresent()) {
            return false;
        }
        plugin.getRuleStore().upsertImageFrameRule(mapKey, defaultRule(mapKey));
        return true;
    }

    public static void setImageFrameTitle(InteractiveImage plugin, String mapKey, String title) {
        update(plugin, mapKey, rule -> new InteractiveImageConfig.MapRule(
                title,
                rule.cooldownTicks(),
                rule.cancelInteract(),
                rule.onRightClick(),
                rule.onLeftClick(),
                rule.activation(),
                rule.effects()
        ));
    }

    public static void setCooldownTicks(InteractiveImage plugin, String mapKey, int ticks) {
        update(plugin, mapKey, rule -> new InteractiveImageConfig.MapRule(
                rule.title(),
                Math.max(0, ticks),
                rule.cancelInteract(),
                rule.onRightClick(),
                rule.onLeftClick(),
                rule.activation(),
                rule.effects()
        ));
    }

    public static void setCancelInteract(InteractiveImage plugin, String mapKey, boolean cancel) {
        update(plugin, mapKey, rule -> new InteractiveImageConfig.MapRule(
                rule.title(),
                rule.cooldownTicks(),
                cancel,
                rule.onRightClick(),
                rule.onLeftClick(),
                rule.activation(),
                rule.effects()
        ));
    }

    public static void addSideAction(InteractiveImage plugin, String mapKey, String side, String action) {
        update(plugin, mapKey, rule -> {
            boolean right = "right".equalsIgnoreCase(side);
            List<String> list = new ArrayList<>(right ? rule.onRightClick() : rule.onLeftClick());
            list.add(action);
            return new InteractiveImageConfig.MapRule(
                    rule.title(),
                    rule.cooldownTicks(),
                    rule.cancelInteract(),
                    right ? List.copyOf(list) : rule.onRightClick(),
                    right ? rule.onLeftClick() : List.copyOf(list),
                    rule.activation(),
                    rule.effects()
            );
        });
    }

    public static void clearSide(InteractiveImage plugin, String mapKey, String side) {
        update(plugin, mapKey, rule -> {
            boolean right = "right".equalsIgnoreCase(side);
            return new InteractiveImageConfig.MapRule(
                    rule.title(),
                    rule.cooldownTicks(),
                    rule.cancelInteract(),
                    right ? List.of() : rule.onRightClick(),
                    right ? rule.onLeftClick() : List.of(),
                    rule.activation(),
                    rule.effects()
            );
        });
    }

    public static boolean removeLastSideAction(InteractiveImage plugin, String mapKey, String side) {
        InteractiveImageConfig.MapRule rule = getOrCreate(plugin, mapKey);
        boolean right = "right".equalsIgnoreCase(side);
        List<String> list = new ArrayList<>(right ? rule.onRightClick() : rule.onLeftClick());
        if (list.isEmpty()) {
            return false;
        }
        list.remove(list.size() - 1);
        plugin.getRuleStore().upsertImageFrameRule(mapKey, new InteractiveImageConfig.MapRule(
                rule.title(),
                rule.cooldownTicks(),
                rule.cancelInteract(),
                right ? List.copyOf(list) : rule.onRightClick(),
                right ? rule.onLeftClick() : List.copyOf(list),
                rule.activation(),
                rule.effects()
        ));
        return true;
    }

    public static boolean toggleBooleanNoInherit(InteractiveImage plugin, String mapKey, String relativePath, boolean globalDefault) {
        InteractiveImageConfig.MapRule rule = getOrCreate(plugin, mapKey);
        TriState current = getOptionalBoolean(rule, relativePath);
        boolean effective = switch (current) {
            case INHERIT -> globalDefault;
            case ON -> true;
            case OFF -> false;
        };

        boolean next = !effective;
        InteractiveImageConfig.MapRule updated = setOptionalBoolean(rule, relativePath, next ? Boolean.TRUE : Boolean.FALSE);
        plugin.getRuleStore().upsertImageFrameRule(mapKey, updated);
        return next;
    }

    public static void setOptionalString(InteractiveImage plugin, String mapKey, String relativePath, String valueOrNullToInherit) {
        InteractiveImageConfig.MapRule rule = getOrCreate(plugin, mapKey);
        InteractiveImageConfig.MapRule updated = setOptionalString(rule, relativePath, valueOrNullToInherit);
        plugin.getRuleStore().upsertImageFrameRule(mapKey, updated);
    }

    public static void setOptionalInt(InteractiveImage plugin, String mapKey, String relativePath, Integer valueOrNullToInherit) {
        InteractiveImageConfig.MapRule rule = getOrCreate(plugin, mapKey);
        InteractiveImageConfig.MapRule updated = setOptionalInt(rule, relativePath, valueOrNullToInherit);
        plugin.getRuleStore().upsertImageFrameRule(mapKey, updated);
    }

    public static void setOptionalDouble(InteractiveImage plugin, String mapKey, String relativePath, Double valueOrNullToInherit) {
        InteractiveImageConfig.MapRule rule = getOrCreate(plugin, mapKey);
        InteractiveImageConfig.MapRule updated = setOptionalDouble(rule, relativePath, valueOrNullToInherit);
        plugin.getRuleStore().upsertImageFrameRule(mapKey, updated);
    }

    private static void update(InteractiveImage plugin, String mapKey, java.util.function.Function<InteractiveImageConfig.MapRule, InteractiveImageConfig.MapRule> fn) {
        InteractiveImageConfig.MapRule rule = getOrCreate(plugin, mapKey);
        plugin.getRuleStore().upsertImageFrameRule(mapKey, fn.apply(rule));
    }

    private static InteractiveImageConfig.MapRule getOrCreate(InteractiveImage plugin, String mapKey) {
        return plugin.getRuleStore().findImageFrameRule(mapKey)
                .orElseGet(() -> {
                    InteractiveImageConfig.MapRule created = defaultRule(mapKey);
                    plugin.getRuleStore().upsertImageFrameRule(mapKey, created);
                    return created;
                });
    }

    private static InteractiveImageConfig.MapRule defaultRule(String mapKey) {
        var effects = new InteractiveImageConfig.MapEffects(
                new InteractiveImageConfig.OptionalBoolean(true),
                "WHITE",
                "BLOCK",
                InteractiveImageConfig.OptionalBoolean.inherit(),
                new InteractiveImageConfig.ActionBarOverride(new InteractiveImageConfig.OptionalBoolean(true), mapKey, 10),
                new InteractiveImageConfig.TitleOverride(new InteractiveImageConfig.OptionalBoolean(false), mapKey, mapKey, 5, 20, 5),
                new InteractiveImageConfig.BossBarOverride(new InteractiveImageConfig.OptionalBoolean(false), mapKey, "YELLOW", "SOLID", 1.0, 10)
        );
        return new InteractiveImageConfig.MapRule(
                mapKey,
                10,
                true,
                List.of(),
                List.of(),
                InteractiveImageConfig.MapActivation.inherit(),
                effects
        );
    }

    // Paths support: effects.glow, effects.frameVisible, effects.actionBar.enabled, effects.title.enabled, effects.bossBar.enabled, activation.click.requireHover
    private static TriState getOptionalBoolean(InteractiveImageConfig.MapRule rule, String path) {
        if ("effects.glow".equalsIgnoreCase(path)) {
            return toTri(rule.effects().glow());
        }
        if ("effects.frameVisible".equalsIgnoreCase(path)) {
            return toTri(rule.effects().frameVisible());
        }
        if ("effects.actionBar.enabled".equalsIgnoreCase(path)) {
            return toTri(rule.effects().actionBar().enabled());
        }
        if ("effects.title.enabled".equalsIgnoreCase(path)) {
            return toTri(rule.effects().title().enabled());
        }
        if ("effects.bossBar.enabled".equalsIgnoreCase(path)) {
            return toTri(rule.effects().bossBar().enabled());
        }
        if ("activation.click.requireHover".equalsIgnoreCase(path)) {
            return toTri(rule.activation().requireHoverForClick());
        }
        return TriState.INHERIT;
    }

    private static InteractiveImageConfig.MapRule setOptionalBoolean(InteractiveImageConfig.MapRule rule, String path, Boolean valueOrNull) {
        InteractiveImageConfig.OptionalBoolean ob = new InteractiveImageConfig.OptionalBoolean(valueOrNull);
        if ("effects.glow".equalsIgnoreCase(path)) {
            var effects = new InteractiveImageConfig.MapEffects(ob, rule.effects().glowColor(), rule.effects().glowMode(), rule.effects().frameVisible(), rule.effects().actionBar(), rule.effects().title(), rule.effects().bossBar());
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        if ("effects.frameVisible".equalsIgnoreCase(path)) {
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), rule.effects().glowColor(), rule.effects().glowMode(), ob, rule.effects().actionBar(), rule.effects().title(), rule.effects().bossBar());
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        if ("effects.actionBar.enabled".equalsIgnoreCase(path)) {
            var ab = new InteractiveImageConfig.ActionBarOverride(ob, rule.effects().actionBar().format(), rule.effects().actionBar().refreshTicks());
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), rule.effects().glowColor(), rule.effects().glowMode(), rule.effects().frameVisible(), ab, rule.effects().title(), rule.effects().bossBar());
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        if ("effects.title.enabled".equalsIgnoreCase(path)) {
            var title = new InteractiveImageConfig.TitleOverride(ob, rule.effects().title().title(), rule.effects().title().subtitle(), rule.effects().title().fadeInTicks(), rule.effects().title().stayTicks(), rule.effects().title().fadeOutTicks());
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), rule.effects().glowColor(), rule.effects().glowMode(), rule.effects().frameVisible(), rule.effects().actionBar(), title, rule.effects().bossBar());
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        if ("effects.bossBar.enabled".equalsIgnoreCase(path)) {
            var boss = new InteractiveImageConfig.BossBarOverride(ob, rule.effects().bossBar().text(), rule.effects().bossBar().color(), rule.effects().bossBar().style(), rule.effects().bossBar().progress(), rule.effects().bossBar().refreshTicks());
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), rule.effects().glowColor(), rule.effects().glowMode(), rule.effects().frameVisible(), rule.effects().actionBar(), rule.effects().title(), boss);
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        if ("activation.click.requireHover".equalsIgnoreCase(path)) {
            var act = new InteractiveImageConfig.MapActivation(rule.activation().hoverRequiredTicks(), rule.activation().hoverMaxDistance(), rule.activation().clickMaxDistance(), ob);
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), act, rule.effects());
        }
        return rule;
    }

    // Paths support: effects.actionBar.format, effects.title.title, effects.title.subtitle, effects.bossBar.text, effects.bossBar.color, effects.bossBar.style
    private static InteractiveImageConfig.MapRule setOptionalString(InteractiveImageConfig.MapRule rule, String path, String valueOrNull) {
        if ("effects.glow.color".equalsIgnoreCase(path)) {
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), valueOrNull, rule.effects().glowMode(), rule.effects().frameVisible(), rule.effects().actionBar(), rule.effects().title(), rule.effects().bossBar());
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        if ("effects.glow.mode".equalsIgnoreCase(path)) {
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), rule.effects().glowColor(), valueOrNull, rule.effects().frameVisible(), rule.effects().actionBar(), rule.effects().title(), rule.effects().bossBar());
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        if ("effects.actionBar.format".equalsIgnoreCase(path)) {
            var ab = new InteractiveImageConfig.ActionBarOverride(rule.effects().actionBar().enabled(), valueOrNull, rule.effects().actionBar().refreshTicks());
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), rule.effects().glowColor(), rule.effects().glowMode(), rule.effects().frameVisible(), ab, rule.effects().title(), rule.effects().bossBar());
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        if ("effects.title.title".equalsIgnoreCase(path)) {
            var title = new InteractiveImageConfig.TitleOverride(rule.effects().title().enabled(), valueOrNull, rule.effects().title().subtitle(), rule.effects().title().fadeInTicks(), rule.effects().title().stayTicks(), rule.effects().title().fadeOutTicks());
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), rule.effects().glowColor(), rule.effects().glowMode(), rule.effects().frameVisible(), rule.effects().actionBar(), title, rule.effects().bossBar());
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        if ("effects.title.subtitle".equalsIgnoreCase(path)) {
            var title = new InteractiveImageConfig.TitleOverride(rule.effects().title().enabled(), rule.effects().title().title(), valueOrNull, rule.effects().title().fadeInTicks(), rule.effects().title().stayTicks(), rule.effects().title().fadeOutTicks());
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), rule.effects().glowColor(), rule.effects().glowMode(), rule.effects().frameVisible(), rule.effects().actionBar(), title, rule.effects().bossBar());
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        if ("effects.bossBar.text".equalsIgnoreCase(path)) {
            var boss = new InteractiveImageConfig.BossBarOverride(rule.effects().bossBar().enabled(), valueOrNull, rule.effects().bossBar().color(), rule.effects().bossBar().style(), rule.effects().bossBar().progress(), rule.effects().bossBar().refreshTicks());
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), rule.effects().glowColor(), rule.effects().glowMode(), rule.effects().frameVisible(), rule.effects().actionBar(), rule.effects().title(), boss);
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        if ("effects.bossBar.color".equalsIgnoreCase(path)) {
            var boss = new InteractiveImageConfig.BossBarOverride(rule.effects().bossBar().enabled(), rule.effects().bossBar().text(), valueOrNull, rule.effects().bossBar().style(), rule.effects().bossBar().progress(), rule.effects().bossBar().refreshTicks());
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), rule.effects().glowColor(), rule.effects().glowMode(), rule.effects().frameVisible(), rule.effects().actionBar(), rule.effects().title(), boss);
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        if ("effects.bossBar.style".equalsIgnoreCase(path)) {
            var boss = new InteractiveImageConfig.BossBarOverride(rule.effects().bossBar().enabled(), rule.effects().bossBar().text(), rule.effects().bossBar().color(), valueOrNull, rule.effects().bossBar().progress(), rule.effects().bossBar().refreshTicks());
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), rule.effects().glowColor(), rule.effects().glowMode(), rule.effects().frameVisible(), rule.effects().actionBar(), rule.effects().title(), boss);
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        return rule;
    }

    // Paths support: activation.hover.requiredTicks, effects.actionBar.refreshTicks, effects.title.fadeInTicks, effects.title.stayTicks, effects.title.fadeOutTicks, effects.bossBar.refreshTicks
    private static InteractiveImageConfig.MapRule setOptionalInt(InteractiveImageConfig.MapRule rule, String path, Integer valueOrNull) {
        if ("activation.hover.requiredTicks".equalsIgnoreCase(path)) {
            var act = new InteractiveImageConfig.MapActivation(valueOrNull, rule.activation().hoverMaxDistance(), rule.activation().clickMaxDistance(), rule.activation().requireHoverForClick());
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), act, rule.effects());
        }
        if ("effects.actionBar.refreshTicks".equalsIgnoreCase(path)) {
            var ab = new InteractiveImageConfig.ActionBarOverride(rule.effects().actionBar().enabled(), rule.effects().actionBar().format(), valueOrNull);
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), rule.effects().glowColor(), rule.effects().glowMode(), rule.effects().frameVisible(), ab, rule.effects().title(), rule.effects().bossBar());
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        if ("effects.title.fadeInTicks".equalsIgnoreCase(path)) {
            var title = new InteractiveImageConfig.TitleOverride(rule.effects().title().enabled(), rule.effects().title().title(), rule.effects().title().subtitle(), valueOrNull, rule.effects().title().stayTicks(), rule.effects().title().fadeOutTicks());
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), rule.effects().glowColor(), rule.effects().glowMode(), rule.effects().frameVisible(), rule.effects().actionBar(), title, rule.effects().bossBar());
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        if ("effects.title.stayTicks".equalsIgnoreCase(path)) {
            var title = new InteractiveImageConfig.TitleOverride(rule.effects().title().enabled(), rule.effects().title().title(), rule.effects().title().subtitle(), rule.effects().title().fadeInTicks(), valueOrNull, rule.effects().title().fadeOutTicks());
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), rule.effects().glowColor(), rule.effects().glowMode(), rule.effects().frameVisible(), rule.effects().actionBar(), title, rule.effects().bossBar());
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        if ("effects.title.fadeOutTicks".equalsIgnoreCase(path)) {
            var title = new InteractiveImageConfig.TitleOverride(rule.effects().title().enabled(), rule.effects().title().title(), rule.effects().title().subtitle(), rule.effects().title().fadeInTicks(), rule.effects().title().stayTicks(), valueOrNull);
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), rule.effects().glowColor(), rule.effects().glowMode(), rule.effects().frameVisible(), rule.effects().actionBar(), title, rule.effects().bossBar());
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        if ("effects.bossBar.refreshTicks".equalsIgnoreCase(path)) {
            var boss = new InteractiveImageConfig.BossBarOverride(rule.effects().bossBar().enabled(), rule.effects().bossBar().text(), rule.effects().bossBar().color(), rule.effects().bossBar().style(), rule.effects().bossBar().progress(), valueOrNull);
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), rule.effects().glowColor(), rule.effects().glowMode(), rule.effects().frameVisible(), rule.effects().actionBar(), rule.effects().title(), boss);
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        return rule;
    }

    // Paths support: activation.hover.maxDistance, activation.click.maxDistance, effects.bossBar.progress
    private static InteractiveImageConfig.MapRule setOptionalDouble(InteractiveImageConfig.MapRule rule, String path, Double valueOrNull) {
        if ("activation.hover.maxDistance".equalsIgnoreCase(path)) {
            var act = new InteractiveImageConfig.MapActivation(rule.activation().hoverRequiredTicks(), valueOrNull, rule.activation().clickMaxDistance(), rule.activation().requireHoverForClick());
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), act, rule.effects());
        }
        if ("activation.click.maxDistance".equalsIgnoreCase(path)) {
            var act = new InteractiveImageConfig.MapActivation(rule.activation().hoverRequiredTicks(), rule.activation().hoverMaxDistance(), valueOrNull, rule.activation().requireHoverForClick());
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), act, rule.effects());
        }
        if ("effects.bossBar.progress".equalsIgnoreCase(path)) {
            var boss = new InteractiveImageConfig.BossBarOverride(rule.effects().bossBar().enabled(), rule.effects().bossBar().text(), rule.effects().bossBar().color(), rule.effects().bossBar().style(), valueOrNull, rule.effects().bossBar().refreshTicks());
            var effects = new InteractiveImageConfig.MapEffects(rule.effects().glow(), rule.effects().glowColor(), rule.effects().glowMode(), rule.effects().frameVisible(), rule.effects().actionBar(), rule.effects().title(), boss);
            return new InteractiveImageConfig.MapRule(rule.title(), rule.cooldownTicks(), rule.cancelInteract(), rule.onRightClick(), rule.onLeftClick(), rule.activation(), effects);
        }
        return rule;
    }

    private static TriState toTri(InteractiveImageConfig.OptionalBoolean optionalBoolean) {
        if (optionalBoolean == null || optionalBoolean.isInherit()) {
            return TriState.INHERIT;
        }
        return optionalBoolean.value() ? TriState.ON : TriState.OFF;
    }

    private enum TriState {
        INHERIT,
        ON,
        OFF
    }
}

