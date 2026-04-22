package im5lb.interactiveimage.hooks;

import im5lb.interactiveimage.config.InteractiveImageConfig;
import im5lb.interactiveimage.model.ResolvedTarget;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface TargetResolver {
    Optional<ResolvedTarget> resolve(ItemFrame frame, Player viewer, InteractiveImageConfig cfg);
}

