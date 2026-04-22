package im5lb.interactiveimage.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import im5lb.interactiveimage.InteractiveImage;
import im5lb.interactiveimage.config.InteractiveImageConfig;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public final class JsonRuleStore implements RuleStore {

    private static final int VERSION = 1;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final InteractiveImage plugin;
    private final Path path;

    private final Map<String, InteractiveImageConfig.MapRule> imageFrameRules = new ConcurrentHashMap<>();

    public JsonRuleStore(InteractiveImage plugin, Path path) {
        this.plugin = plugin;
        this.path = path;
    }

    public void load() {
        imageFrameRules.clear();
        if (!Files.exists(path)) {
            return;
        }
        boolean changed = false;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            RulesFile file = GSON.fromJson(reader, RulesFile.class);
            if (file == null || file.imageframe == null || file.imageframe.maps == null) {
                return;
            }
            for (var entry : file.imageframe.maps.entrySet()) {
                String key = normalizeKey(entry.getKey());
                if (key == null) {
                    continue;
                }
                InteractiveImageConfig.MapRule normalized = normalizeRule(key, entry.getValue());
                imageFrameRules.put(key, normalized);
                if (!Objects.equals(entry.getValue(), normalized)) {
                    changed = true;
                }
            }
        } catch (JsonSyntaxException e) {
            plugin.getLogger().severe("[interactiveimage] Failed to parse iiamge.json: " + e.getMessage());
        } catch (IOException e) {
            plugin.getLogger().severe("[interactiveimage] Failed to read iiamge.json: " + e.getMessage());
        }

        if (changed) {
            save();
        }
    }

    @Override
    public Optional<InteractiveImageConfig.MapRule> findImageFrameRule(String mapName) {
        String key = normalizeKey(mapName);
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(imageFrameRules.get(key));
    }

    @Override
    public Optional<InteractiveImageConfig.MapRule> findImageFrameRuleOrWildcard(String mapName) {
        String key = normalizeKey(mapName);
        if (key == null) {
            return Optional.empty();
        }
        InteractiveImageConfig.MapRule direct = imageFrameRules.get(key);
        if (direct != null) {
            return Optional.of(direct);
        }
        return Optional.ofNullable(imageFrameRules.get("*"));
    }

    @Override
    public InteractiveImageConfig.MapRule upsertImageFrameRule(String mapName, InteractiveImageConfig.MapRule rule) {
        String key = normalizeKey(mapName);
        if (key == null) {
            throw new IllegalArgumentException("mapName");
        }
        InteractiveImageConfig.MapRule normalized = normalizeRule(key, rule);
        imageFrameRules.put(key, normalized);
        save();
        return normalized;
    }

    @Override
    public boolean deleteImageFrameRule(String mapName) {
        String key = normalizeKey(mapName);
        if (key == null) {
            return false;
        }
        InteractiveImageConfig.MapRule removed = imageFrameRules.remove(key);
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    @Override
    public void save() {
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            plugin.getLogger().warning("[interactiveimage] Failed to create data folder for iiamge.json: " + e.getMessage());
            return;
        }

        RulesFile file = new RulesFile(
                VERSION,
                new ProviderSection(new TreeMap<>(imageFrameRules))
        );

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(file, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("[interactiveimage] Failed to write iiamge.json: " + e.getMessage());
        }
    }

    private static String normalizeKey(String key) {
        if (key == null) {
            return null;
        }
        String trimmed = key.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private InteractiveImageConfig.MapRule normalizeRule(String mapKey, InteractiveImageConfig.MapRule rule) {
        InteractiveImageConfig global = plugin.getConfigModel();

        InteractiveImageConfig.MapRule base = rule == null
                ? new InteractiveImageConfig.MapRule(
                mapKey,
                10,
                true,
                List.of(),
                List.of(),
                InteractiveImageConfig.MapActivation.inherit(),
                InteractiveImageConfig.MapEffects.empty()
        )
                : rule;

        String title = base.title() == null || base.title().isBlank() ? mapKey : base.title();
        int cooldownTicks = Math.max(0, base.cooldownTicks());
        boolean cancelInteract = base.cancelInteract();

        List<String> right = base.onRightClick() == null ? List.of() : List.copyOf(base.onRightClick());
        List<String> left = base.onLeftClick() == null ? List.of() : List.copyOf(base.onLeftClick());

        InteractiveImageConfig.MapActivation activation = base.activation() == null ? InteractiveImageConfig.MapActivation.inherit() : base.activation();
        int hoverTicks = activation.hoverRequiredTicks() == null ? global.activation().hover().requiredTicks() : activation.hoverRequiredTicks();
        double hoverMax = activation.hoverMaxDistance() == null ? global.activation().hover().maxDistance() : activation.hoverMaxDistance();
        double clickMax = activation.clickMaxDistance() == null ? global.activation().click().maxDistance() : activation.clickMaxDistance();
        boolean requireHover = activation.requireHoverForClick() == null || activation.requireHoverForClick().isInherit()
                ? global.activation().click().requireHover()
                : activation.requireHoverForClick().orElse(global.activation().click().requireHover());
        InteractiveImageConfig.MapActivation activationNormalized = new InteractiveImageConfig.MapActivation(
                hoverTicks,
                hoverMax,
                clickMax,
                new InteractiveImageConfig.OptionalBoolean(requireHover)
        );

        InteractiveImageConfig.MapEffects effects = base.effects() == null ? InteractiveImageConfig.MapEffects.empty() : base.effects();
        boolean glow = effects.glow() == null || effects.glow().isInherit() ? global.effects().glow().enabled() : effects.glow().orElse(global.effects().glow().enabled());
        String glowColor = effects.glowColor() == null ? "WHITE" : effects.glowColor();
        String glowMode = effects.glowMode() == null ? "BLOCK" : effects.glowMode();
        InteractiveImageConfig.OptionalBoolean frameVisible = effects.frameVisible() == null ? InteractiveImageConfig.OptionalBoolean.inherit() : effects.frameVisible();

        boolean abEnabled = effects.actionBar() == null || effects.actionBar().enabled() == null || effects.actionBar().enabled().isInherit()
                ? global.effects().actionBar().enabled()
                : effects.actionBar().enabled().orElse(global.effects().actionBar().enabled());
        String abFormat = effects.actionBar() == null || effects.actionBar().format() == null ? global.effects().actionBar().format() : effects.actionBar().format();
        int abRefresh = effects.actionBar() == null || effects.actionBar().refreshTicks() == null ? global.effects().actionBar().refreshTicks() : effects.actionBar().refreshTicks();
        InteractiveImageConfig.ActionBarOverride actionBarNormalized = new InteractiveImageConfig.ActionBarOverride(
                new InteractiveImageConfig.OptionalBoolean(abEnabled),
                abFormat,
                abRefresh
        );

        boolean titleEnabled = effects.title() == null || effects.title().enabled() == null || effects.title().enabled().isInherit()
                ? global.effects().title().enabled()
                : effects.title().enabled().orElse(global.effects().title().enabled());
        String tTitle = effects.title() == null || effects.title().title() == null ? global.effects().title().title() : effects.title().title();
        String tSubtitle = effects.title() == null || effects.title().subtitle() == null ? global.effects().title().subtitle() : effects.title().subtitle();
        int tFadeIn = effects.title() == null || effects.title().fadeInTicks() == null ? global.effects().title().fadeInTicks() : effects.title().fadeInTicks();
        int tStay = effects.title() == null || effects.title().stayTicks() == null ? global.effects().title().stayTicks() : effects.title().stayTicks();
        int tFadeOut = effects.title() == null || effects.title().fadeOutTicks() == null ? global.effects().title().fadeOutTicks() : effects.title().fadeOutTicks();
        InteractiveImageConfig.TitleOverride titleNormalized = new InteractiveImageConfig.TitleOverride(
                new InteractiveImageConfig.OptionalBoolean(titleEnabled),
                tTitle,
                tSubtitle,
                tFadeIn,
                tStay,
                tFadeOut
        );

        boolean bossEnabled = effects.bossBar() == null || effects.bossBar().enabled() == null || effects.bossBar().enabled().isInherit()
                ? global.effects().bossBar().enabled()
                : effects.bossBar().enabled().orElse(global.effects().bossBar().enabled());
        String bbText = effects.bossBar() == null || effects.bossBar().text() == null ? global.effects().bossBar().text() : effects.bossBar().text();
        String bbColor = effects.bossBar() == null || effects.bossBar().color() == null ? global.effects().bossBar().color() : effects.bossBar().color();
        String bbStyle = effects.bossBar() == null || effects.bossBar().style() == null ? global.effects().bossBar().style() : effects.bossBar().style();
        double bbProgress = effects.bossBar() == null || effects.bossBar().progress() == null ? global.effects().bossBar().progress() : effects.bossBar().progress();
        int bbRefresh = effects.bossBar() == null || effects.bossBar().refreshTicks() == null ? global.effects().bossBar().refreshTicks() : effects.bossBar().refreshTicks();
        InteractiveImageConfig.BossBarOverride bossNormalized = new InteractiveImageConfig.BossBarOverride(
                new InteractiveImageConfig.OptionalBoolean(bossEnabled),
                bbText,
                bbColor,
                bbStyle,
                bbProgress,
                bbRefresh
        );

        InteractiveImageConfig.MapEffects effectsNormalized = new InteractiveImageConfig.MapEffects(
                new InteractiveImageConfig.OptionalBoolean(glow),
                glowColor,
                glowMode,
                frameVisible,
                actionBarNormalized,
                titleNormalized,
                bossNormalized
        );

        return new InteractiveImageConfig.MapRule(title, cooldownTicks, cancelInteract, right, left, activationNormalized, effectsNormalized);
    }

    private record RulesFile(int version, ProviderSection imageframe) {
    }

    private record ProviderSection(Map<String, InteractiveImageConfig.MapRule> maps) {
    }
}

