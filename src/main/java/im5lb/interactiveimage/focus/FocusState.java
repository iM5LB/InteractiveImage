package im5lb.interactiveimage.focus;

import im5lb.interactiveimage.model.ResolvedTarget;

import java.util.List;
import java.util.UUID;

public record FocusState(
        UUID worldUuid,
        UUID frameUuid,
        List<UUID> affectedFrameUuids,
        ResolvedTarget target,
        long lastHudNanos
) {
    public FocusState(
            UUID worldUuid,
            UUID frameUuid,
            List<UUID> affectedFrameUuids,
            ResolvedTarget target
    ) {
        this(worldUuid, frameUuid, affectedFrameUuids, target, 0L);
    }
}
