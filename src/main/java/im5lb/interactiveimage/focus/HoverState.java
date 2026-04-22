package im5lb.interactiveimage.focus;

import im5lb.interactiveimage.model.ResolvedTarget;

import java.util.UUID;

public record HoverState(
        UUID frameUuid,
        ResolvedTarget target,
        int accumulatedTicks
) {
}
