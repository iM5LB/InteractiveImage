package im5lb.interactiveimage.hooks.imageframe;

import im5lb.interactiveimage.InteractiveImage;
import im5lb.interactiveimage.config.InteractiveImageConfig;
import im5lb.interactiveimage.hooks.TargetResolver;
import im5lb.interactiveimage.model.ResolvedTarget;
import im5lb.interactiveimage.store.RuleStore;
import org.bukkit.Bukkit;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public final class ImageFrameResolver implements TargetResolver {

    private final InteractiveImage plugin;
    private final RuleStore ruleStore;

    private volatile boolean initTried = false;
    private volatile boolean available = false;

    private Field imageMapManagerField;
    private Method getFromMapViewMethod;
    private Method imageMapGetNameMethod;

    public ImageFrameResolver(InteractiveImage plugin, RuleStore ruleStore) {
        this.plugin = plugin;
        this.ruleStore = ruleStore;
    }

    @Override
    public Optional<ResolvedTarget> resolve(ItemFrame frame, Player viewer, InteractiveImageConfig cfg) {
        if (!cfg.providers().imageFrame().enabled()) {
            return Optional.empty();
        }
        if (!isAvailable()) {
            return Optional.empty();
        }

        return resolveInternal(frame, viewer, cfg, false);
    }

    public Optional<ResolvedTarget> resolveForAdmin(ItemFrame frame, Player viewer, InteractiveImageConfig cfg) {
        if (!cfg.providers().imageFrame().enabled()) {
            return Optional.empty();
        }
        if (!isAvailable()) {
            return Optional.empty();
        }
        return resolveInternal(frame, viewer, cfg, true);
    }

    private Optional<ResolvedTarget> resolveInternal(ItemFrame frame, Player viewer, InteractiveImageConfig cfg, boolean ignoreOnlyConfiguredGate) {
        ItemStack item = frame.getItem();
        if (item == null) {
            return Optional.empty();
        }
        if (!(item.getItemMeta() instanceof MapMeta mapMeta)) {
            return Optional.empty();
        }
        MapView mapView = mapMeta.getMapView();
        if (mapView == null) {
            return Optional.empty();
        }

        Object imageMap = getImageMap(mapView);
        if (imageMap == null) {
            return Optional.empty();
        }

        String mapName = getMapName(imageMap);
        if (mapName == null || mapName.isBlank()) {
            mapName = "(unnamed)";
        }

        var providersCfg = cfg.providers().imageFrame();
        Optional<InteractiveImageConfig.MapRule> ruleOpt = ruleStore.findImageFrameRuleOrWildcard(mapName);

        if (!ignoreOnlyConfiguredGate && providersCfg.onlyConfiguredMaps() && ruleOpt.isEmpty()) {
            return Optional.empty();
        }

        String title = ruleOpt.map(InteractiveImageConfig.MapRule::title).orElse(mapName);

        return Optional.of(new ResolvedTarget(
                "imageframe",
                frame.getUniqueId(),
                mapName,
                title,
                ruleOpt.orElse(null)
        ));
    }

    private boolean isAvailable() {
        if (available) {
            return true;
        }
        if (initTried) {
            return false;
        }
        initTried = true;

        if (Bukkit.getPluginManager().getPlugin("ImageFrame") == null) {
            available = false;
            return false;
        }

        try {
            Class<?> imageFrameClass = Class.forName("com.loohp.imageframe.ImageFrame");
            imageMapManagerField = imageFrameClass.getDeclaredField("imageMapManager");
            imageMapManagerField.setAccessible(true);

            Class<?> imageMapManagerClass = Class.forName("com.loohp.imageframe.objectholders.ImageMapManager");
            getFromMapViewMethod = imageMapManagerClass.getMethod("getFromMapView", MapView.class);

            Class<?> imageMapClass = Class.forName("com.loohp.imageframe.objectholders.ImageMap");
            imageMapGetNameMethod = imageMapClass.getMethod("getName");

            available = true;
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("[interactiveimage] Failed to hook ImageFrame API: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            available = false;
            return false;
        }
    }

    private Object getImageMap(MapView mapView) {
        try {
            Object manager = imageMapManagerField.get(null);
            if (manager == null) {
                return null;
            }
            return getFromMapViewMethod.invoke(manager, mapView);
        } catch (Throwable t) {
            return null;
        }
    }

    private String getMapName(Object imageMap) {
        try {
            Object name = imageMapGetNameMethod.invoke(imageMap);
            if (name == null) {
                return null;
            }
            return name.toString().trim();
        } catch (Throwable t) {
            return null;
        }
    }
}

