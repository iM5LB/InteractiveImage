package im5lb.interactiveimage.effects;

import im5lb.interactiveimage.config.InteractiveImageConfig;
import im5lb.interactiveimage.model.ResolvedTarget;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

public final class EffectManager {

    private static final int HOVER_TITLE_STAY_TICKS = 20 * 60 * 60;

    private final GlowEffect glowEffect;
    private final GlintEffect glintEffect;
    private final HiddenFrameBlockHighlightEffect hiddenFrameBlockHighlightEffect;
    private final ActionBarEffect actionBarEffect;
    private final TitleEffect titleEffect;
    private final BossBarEffect bossBarEffect;

    public EffectManager(
            GlowEffect glowEffect,
            GlintEffect glintEffect,
            HiddenFrameBlockHighlightEffect hiddenFrameBlockHighlightEffect,
            ActionBarEffect actionBarEffect,
            TitleEffect titleEffect,
            BossBarEffect bossBarEffect
    ) {
        this.glowEffect = glowEffect;
        this.glintEffect = glintEffect;
        this.hiddenFrameBlockHighlightEffect = hiddenFrameBlockHighlightEffect;
        this.actionBarEffect = actionBarEffect;
        this.titleEffect = titleEffect;
        this.bossBarEffect = bossBarEffect;
    }

    public void onFocus(Player player, ItemFrame frame, ResolvedTarget target, InteractiveImageConfig cfg) {
        onFocusVisuals(frame, target, cfg);

        var rule = target.rule();
        var overrides = rule == null ? InteractiveImageConfig.MapEffects.empty() : rule.effects();

        boolean actionBarEnabled = overrides.actionBar().enabled().orElse(cfg.effects().actionBar().enabled());
        if (actionBarEnabled) {
            actionBarEffect.onFocus(player, target, cfg, overrides.actionBar().format());
        }

        boolean titleEnabled = overrides.title().enabled().orElse(cfg.effects().title().enabled());
        if (titleEnabled) {
            String title = coalesce(overrides.title().title(), cfg.effects().title().title());
            String subtitle = coalesce(overrides.title().subtitle(), cfg.effects().title().subtitle());
            titleEffect.show(
                    player,
                    title,
                    subtitle,
                    overrides.title().fadeInTicks() != null ? overrides.title().fadeInTicks() : cfg.effects().title().fadeInTicks(),
                    HOVER_TITLE_STAY_TICKS,
                    overrides.title().fadeOutTicks() != null ? overrides.title().fadeOutTicks() : cfg.effects().title().fadeOutTicks()
            );
        }

        boolean bossEnabled = overrides.bossBar().enabled().orElse(cfg.effects().bossBar().enabled());
        if (bossEnabled) {
            String text = coalesce(overrides.bossBar().text(), cfg.effects().bossBar().text());
            String color = coalesce(overrides.bossBar().color(), cfg.effects().bossBar().color());
            String style = coalesce(overrides.bossBar().style(), cfg.effects().bossBar().style());
            double progress = overrides.bossBar().progress() != null ? overrides.bossBar().progress() : cfg.effects().bossBar().progress();
            bossBarEffect.onFocus(player, target, cfg, text, color, style, progress);
        } else {
            bossBarEffect.onUnfocus(player);
        }
    }

    public void refreshTitle(Player player, ResolvedTarget target, InteractiveImageConfig cfg) {
        if (player == null || target == null) {
            return;
        }
        var rule = target.rule();
        var overrides = rule == null ? InteractiveImageConfig.MapEffects.empty() : rule.effects();

        boolean titleEnabled = overrides.title().enabled().orElse(cfg.effects().title().enabled());
        if (!titleEnabled) {
            return;
        }

        String title = coalesce(overrides.title().title(), cfg.effects().title().title());
        String subtitle = coalesce(overrides.title().subtitle(), cfg.effects().title().subtitle());
        titleEffect.show(
                player,
                title,
                subtitle,
                overrides.title().fadeInTicks() != null ? overrides.title().fadeInTicks() : cfg.effects().title().fadeInTicks(),
                HOVER_TITLE_STAY_TICKS,
                overrides.title().fadeOutTicks() != null ? overrides.title().fadeOutTicks() : cfg.effects().title().fadeOutTicks()
        );
    }

    public void refreshActionBar(Player player, ResolvedTarget target, InteractiveImageConfig cfg) {
        refresh(player, null, target, cfg);
    }

    public void refresh(Player player, ItemFrame frame, ResolvedTarget target, InteractiveImageConfig cfg) {
        var rule = target.rule();
        var overrides = rule == null ? InteractiveImageConfig.MapEffects.empty() : rule.effects();

        boolean glowEnabled = overrides.glow().orElse(cfg.effects().glow().enabled());
        if (frame != null && glowEnabled) {
            String glowColor = overrides.glowColor() == null ? "WHITE" : overrides.glowColor();
            String glowMode = overrides.glowMode() == null ? "BLOCK" : overrides.glowMode();
            glintEffect.reapplyIfFocused(frame);
            if ("FRAME".equalsIgnoreCase(glowMode)) {
                glowEffect.reapplyIfFocused(frame, glowColor);
            } else {
                hiddenFrameBlockHighlightEffect.reapplyIfFocused(frame, glowColor);
            }
        }

        boolean actionBarEnabled = overrides.actionBar().enabled().orElse(cfg.effects().actionBar().enabled());
        if (actionBarEnabled) {
            actionBarEffect.onFocus(player, target, cfg, overrides.actionBar().format());
        } else {
            actionBarEffect.onUnfocus(player, cfg, true);
        }

        boolean bossEnabled = overrides.bossBar().enabled().orElse(cfg.effects().bossBar().enabled());
        if (bossEnabled) {
            String text = coalesce(overrides.bossBar().text(), cfg.effects().bossBar().text());
            String color = coalesce(overrides.bossBar().color(), cfg.effects().bossBar().color());
            String style = coalesce(overrides.bossBar().style(), cfg.effects().bossBar().style());
            double progress = overrides.bossBar().progress() != null ? overrides.bossBar().progress() : cfg.effects().bossBar().progress();
            bossBarEffect.onFocus(player, target, cfg, text, color, style, progress);
        } else {
            bossBarEffect.onUnfocus(player);
        }
    }

    public void onUnfocus(Player player, ItemFrame frame, ResolvedTarget target, InteractiveImageConfig cfg) {
        onUnfocus(player, frame, target, cfg, true);
    }

    public void onUnfocus(Player player, ItemFrame frame, ResolvedTarget target, InteractiveImageConfig cfg, boolean clearTitle) {
        if (frame != null && target != null) {
            onUnfocusVisuals(frame, target, cfg);
        }
        if (player != null) {
            var rule = target == null ? null : target.rule();
            var overrides = rule == null ? InteractiveImageConfig.MapEffects.empty() : rule.effects();
            actionBarEffect.onUnfocus(player, cfg, overrides.actionBar().enabled().orElse(cfg.effects().actionBar().enabled()));
            bossBarEffect.onUnfocus(player);
            if (clearTitle) {
                titleEffect.clear(player);
            }
        }
    }

    public void releaseGlow(ItemFrame frame) {
        if (frame == null) {
            return;
        }
        glowEffect.onUnfocus(frame);
        glintEffect.onUnfocus(frame);
        hiddenFrameBlockHighlightEffect.onUnfocus(frame);
    }

    public void reapplyGlow(ItemFrame frame, ResolvedTarget target, InteractiveImageConfig cfg) {
        if (frame == null || target == null) {
            return;
        }
        var rule = target.rule();
        var overrides = rule == null ? InteractiveImageConfig.MapEffects.empty() : rule.effects();
        boolean glowEnabled = overrides.glow().orElse(cfg.effects().glow().enabled());
        if (!glowEnabled) {
            return;
        }
        String glowColor = overrides.glowColor() == null ? "WHITE" : overrides.glowColor();
        String glowMode = overrides.glowMode() == null ? "BLOCK" : overrides.glowMode();
        glintEffect.reapplyIfFocused(frame);
        if ("FRAME".equalsIgnoreCase(glowMode)) {
            glowEffect.reapplyIfFocused(frame, glowColor);
        } else {
            hiddenFrameBlockHighlightEffect.reapplyIfFocused(frame, glowColor);
        }
    }

    public void shutdown() {
        glowEffect.shutdown();
        glintEffect.shutdown();
        // Displays are stored per-world; FocusScanner handles per-world cleanup on stop.
        bossBarEffect.shutdown();
    }

    public void shutdownWorld(org.bukkit.World world) {
        if (world == null) {
            return;
        }
        hiddenFrameBlockHighlightEffect.shutdown(world);
    }

    public void onFocusVisuals(ItemFrame frame, ResolvedTarget target, InteractiveImageConfig cfg) {
        var rule = target.rule();
        var overrides = rule == null ? InteractiveImageConfig.MapEffects.empty() : rule.effects();

        applyFrameVisibility(frame, overrides);

        boolean glowEnabled = overrides.glow().orElse(cfg.effects().glow().enabled());
        if (!glowEnabled) {
            return;
        }
        String glowColor = overrides.glowColor() == null ? "WHITE" : overrides.glowColor();
        glintEffect.onFocus(frame);
        String glowMode = overrides.glowMode() == null ? "BLOCK" : overrides.glowMode();

        if ("FRAME".equalsIgnoreCase(glowMode)) {
            glowEffect.onFocus(frame, glowColor);
        } else {
            hiddenFrameBlockHighlightEffect.onFocus(frame, glowColor);
        }
    }

    public void onUnfocusVisuals(ItemFrame frame, ResolvedTarget target, InteractiveImageConfig cfg) {
        // Always release what we applied, even if config changed while the player was focused.
        glowEffect.onUnfocus(frame);
        glintEffect.onUnfocus(frame);
        hiddenFrameBlockHighlightEffect.onUnfocus(frame);
    }

    private static String coalesce(String a, String b) {
        return a != null ? a : b;
    }

    private static void applyFrameVisibility(ItemFrame frame, InteractiveImageConfig.MapEffects effects) {
        if (frame == null || effects == null || effects.frameVisible() == null || effects.frameVisible().isInherit()) {
            return;
        }
        try {
            frame.setVisible(effects.frameVisible().orElse(true));
        } catch (Throwable ignored) {
        }
    }
}

