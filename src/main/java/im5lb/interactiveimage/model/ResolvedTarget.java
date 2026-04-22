package im5lb.interactiveimage.model;

import im5lb.interactiveimage.config.InteractiveImageConfig;

import java.util.UUID;

public record ResolvedTarget(
        String providerId,
        UUID frameUuid,
        String mapName,
        String title,
        InteractiveImageConfig.MapRule rule
) {
}

