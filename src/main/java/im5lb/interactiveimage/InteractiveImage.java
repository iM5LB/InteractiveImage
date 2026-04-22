package im5lb.interactiveimage;

import im5lb.interactiveimage.commands.IiCommand;
import im5lb.interactiveimage.config.InteractiveImageConfig;
import im5lb.interactiveimage.editor.EditorListener;
import im5lb.interactiveimage.editor.EditorManager;
import im5lb.interactiveimage.effects.ActionBarEffect;
import im5lb.interactiveimage.effects.BossBarEffect;
import im5lb.interactiveimage.effects.EffectManager;
import im5lb.interactiveimage.effects.GlintEffect;
import im5lb.interactiveimage.effects.GlowEffect;
import im5lb.interactiveimage.effects.HiddenFrameBlockHighlightEffect;
import im5lb.interactiveimage.effects.TitleEffect;
import im5lb.interactiveimage.focus.FocusScanner;
import im5lb.interactiveimage.hooks.TargetResolver;
import im5lb.interactiveimage.hooks.imageframe.ImageFrameResolver;
import im5lb.interactiveimage.listeners.FrameInteractListener;
import im5lb.interactiveimage.listeners.FocusedAirClickListener;
import im5lb.interactiveimage.store.JsonRuleStore;
import im5lb.interactiveimage.store.RuleStore;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InteractiveImage extends JavaPlugin {

    private volatile InteractiveImageConfig configModel;
    private FocusScanner focusScanner;
    private FrameInteractListener frameInteractListener;
    private EditorManager editorManager;
    private JsonRuleStore ruleStore;

    private final List<TargetResolver> resolvers = new ArrayList<>();

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("ImageFrame") == null) {
            getLogger().severe("[interactiveimage] ImageFrame is required but not installed. Disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        migrateDataIfNeeded();
        if (!getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdirs();
        }

        // No global settings files (no settings.json, no config.yml).
        // Only per-frame rules are stored in a single JSON data file.
        Path settingsPath = Path.of(getDataFolder().getAbsolutePath(), "settings.json");
        try {
            if (Files.exists(settingsPath)) {
                Files.delete(settingsPath);
                getLogger().warning("[interactiveimage] Deleted settings.json (global settings are not supported).");
            }
        } catch (IOException e) {
            getLogger().warning("[interactiveimage] Failed to delete settings.json: " + e.getMessage());
        }
        reloadInteractiveImageConfig();

        ruleStore = new JsonRuleStore(this, Path.of(getDataFolder().getAbsolutePath(), "iiamge.json"));
        ruleStore.load();

        var effectManager = new EffectManager(
                new GlowEffect(),
                new GlintEffect(),
                new HiddenFrameBlockHighlightEffect(),
                new ActionBarEffect(),
                new TitleEffect(),
                new BossBarEffect()
        );

        resolvers.add(new ImageFrameResolver(this, ruleStore));

        focusScanner = new FocusScanner(this, () -> configModel, resolvers, effectManager);
        editorManager = new EditorManager(this);
        frameInteractListener = new FrameInteractListener(this, () -> configModel, resolvers, editorManager, focusScanner::getFocusedFrameUuid);

        Bukkit.getPluginManager().registerEvents(frameInteractListener, this);
        Bukkit.getPluginManager().registerEvents(new FocusedAirClickListener(() -> configModel, focusScanner, editorManager), this);
        Bukkit.getPluginManager().registerEvents(new EditorListener(this, editorManager), this);

        focusScanner.start();

        var command = new IiCommand(this, editorManager);
        Objects.requireNonNull(getCommand("ii"), "ii command missing from plugin.yml")
                .setExecutor(command);
        Objects.requireNonNull(getCommand("ii"), "ii command missing from plugin.yml")
                .setTabCompleter(command);

        Bukkit.getServicesManager().register(InteractiveImage.class, this, this, ServicePriority.Normal);
    }

    @Override
    public void onDisable() {
        if (focusScanner != null) {
            focusScanner.stop();
        }
        if (editorManager != null) {
            editorManager.shutdown();
        }
        Bukkit.getServicesManager().unregister(InteractiveImage.class, this);
    }

    public void reloadInteractiveImageConfig() {
        configModel = InteractiveFrameDefaults.defaults();
        if (ruleStore != null) {
            ruleStore.load();
        }
    }

    public void reloadAndRestart() {
        reloadInteractiveImageConfig();
        if (focusScanner != null) {
            focusScanner.start();
        }
    }

    public void clearFocusedImageFrameMap(String mapName) {
        if (focusScanner == null) {
            return;
        }
        focusScanner.clearFocusedMap("imageframe", mapName);
    }

    public InteractiveImageConfig getConfigModel() {
        return configModel;
    }

    public List<TargetResolver> getResolvers() {
        return List.copyOf(resolvers);
    }

    public EditorManager getEditorManager() {
        return editorManager;
    }

    public RuleStore getRuleStore() {
        return ruleStore;
    }

    private static final class InteractiveFrameDefaults {
        private static InteractiveImageConfig defaults() {
            return new InteractiveImageConfig(
                    new InteractiveImageConfig.Scan(2, 6.0),
                    new InteractiveImageConfig.Activation(
                            new InteractiveImageConfig.Hover(0, 6.0),
                            new InteractiveImageConfig.Click(6.0, false)
                    ),
                    new InteractiveImageConfig.Effects(
                            new InteractiveImageConfig.Glow(true),
                            new InteractiveImageConfig.ActionBar(true, 10, ""),
                            new InteractiveImageConfig.Title(false, "", "", 5, 20, 5),
                            new InteractiveImageConfig.BossBar(false, "", "YELLOW", "SOLID", 1.0, 10)
                    ),
                    new InteractiveImageConfig.Providers(
                            new InteractiveImageConfig.ImageFrame(true, true)
                    )
            );
        }
    }

    private void migrateDataIfNeeded() {
        // Plugin was previously named "interactiveimage" (lowercase) and earlier versions used other names.
        // Renaming the plugin changes getDataFolder() and can make data appear "lost".
        try {
            var dataFolder = getDataFolder();
            var parent = dataFolder.getParentFile();
            if (parent == null) {
                return;
            }

            var candidates = List.of(
                    new java.io.File(parent, "interactiveimage"),
                    new java.io.File(parent, "interactiveframe"),
                    new java.io.File(parent, "iimages")
            );

            java.io.File oldFolder = null;
            for (var c : candidates) {
                if (c.isDirectory() && !c.getAbsolutePath().equalsIgnoreCase(dataFolder.getAbsolutePath())) {
                    oldFolder = c;
                    break;
                }
            }
            if (oldFolder == null) {
                return;
            }

            if (!dataFolder.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dataFolder.mkdirs();
            }

            Path oldJson = oldFolder.toPath().resolve("iiamge.json");
            Path newJson = dataFolder.toPath().resolve("iiamge.json");
            if (Files.exists(oldJson) && !Files.exists(newJson)) {
                Files.copy(oldJson, newJson);
                getLogger().info("[InteractiveImage] Migrated iiamge.json from " + oldFolder.getName() + " to " + dataFolder.getName());
            }
        } catch (Throwable t) {
            getLogger().warning("[InteractiveImage] Failed to migrate old data folder: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }
}

