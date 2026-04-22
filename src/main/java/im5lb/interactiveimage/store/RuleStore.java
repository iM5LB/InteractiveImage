package im5lb.interactiveimage.store;

import im5lb.interactiveimage.config.InteractiveImageConfig;

import java.util.Optional;

public interface RuleStore {
    Optional<InteractiveImageConfig.MapRule> findImageFrameRule(String mapName);

    Optional<InteractiveImageConfig.MapRule> findImageFrameRuleOrWildcard(String mapName);

    InteractiveImageConfig.MapRule upsertImageFrameRule(String mapName, InteractiveImageConfig.MapRule rule);

    boolean deleteImageFrameRule(String mapName);

    void save();
}

