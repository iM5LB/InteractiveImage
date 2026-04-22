package im5lb.interactiveimage.config;

import java.util.List;

public record InteractiveImageConfig(
        Scan scan,
        Activation activation,
        Effects effects,
        Providers providers
) {

    public record Scan(
            int intervalTicks,
            double maxDistance
    ) {
    }

    public record Activation(
            Hover hover,
            Click click
    ) {
    }

    public record Hover(
            int requiredTicks,
            double maxDistance
    ) {
    }

    public record Click(
            double maxDistance,
            boolean requireHover
    ) {
    }

    public record Effects(
            Glow glow,
            ActionBar actionBar,
            Title title,
            BossBar bossBar
    ) {
    }

    public record Glow(
            boolean enabled
    ) {
    }

    public record ActionBar(
            boolean enabled,
            int refreshTicks,
            String format
    ) {
    }

    public record Title(
            boolean enabled,
            String title,
            String subtitle,
            int fadeInTicks,
            int stayTicks,
            int fadeOutTicks
    ) {
    }

    public record BossBar(
            boolean enabled,
            String text,
            String color,
            String style,
            double progress,
            int refreshTicks
    ) {
    }

    public record Providers(
            ImageFrame imageFrame
    ) {
    }

    public record ImageFrame(
            boolean enabled,
            boolean onlyConfiguredMaps
    ) {
    }

    public record MapRule(
            String title,
            int cooldownTicks,
            boolean cancelInteract,
            List<String> onRightClick,
            List<String> onLeftClick,
            MapActivation activation,
            MapEffects effects
    ) {
    }

    public record MapActivation(
            Integer hoverRequiredTicks,
            Double hoverMaxDistance,
            Double clickMaxDistance,
            OptionalBoolean requireHoverForClick
    ) {
        public static MapActivation inherit() {
            return new MapActivation(null, null, null, OptionalBoolean.inherit());
        }
    }

    public record MapEffects(
            OptionalBoolean glow,
            String glowColor,
            String glowMode,
            OptionalBoolean frameVisible,
            ActionBarOverride actionBar,
            TitleOverride title,
            BossBarOverride bossBar
    ) {
        public static MapEffects empty() {
            return new MapEffects(OptionalBoolean.inherit(), null, null, OptionalBoolean.inherit(), ActionBarOverride.inherit(), TitleOverride.inherit(), BossBarOverride.inherit());
        }
    }

    public record ActionBarOverride(
            OptionalBoolean enabled,
            String format,
            Integer refreshTicks
    ) {
        public static ActionBarOverride inherit() {
            return new ActionBarOverride(OptionalBoolean.inherit(), null, null);
        }
    }

    public record TitleOverride(
            OptionalBoolean enabled,
            String title,
            String subtitle,
            Integer fadeInTicks,
            Integer stayTicks,
            Integer fadeOutTicks
    ) {
        public static TitleOverride inherit() {
            return new TitleOverride(OptionalBoolean.inherit(), null, null, null, null, null);
        }
    }

    public record BossBarOverride(
            OptionalBoolean enabled,
            String text,
            String color,
            String style,
            Double progress,
            Integer refreshTicks
    ) {
        public static BossBarOverride inherit() {
            return new BossBarOverride(OptionalBoolean.inherit(), null, null, null, null, null);
        }
    }

    public record OptionalBoolean(Boolean value) {
        public static OptionalBoolean inherit() {
            return new OptionalBoolean(null);
        }

        public boolean isInherit() {
            return value == null;
        }

        public boolean orElse(boolean fallback) {
            return value == null ? fallback : value;
        }
    }
}
