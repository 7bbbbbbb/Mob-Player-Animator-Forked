package me.Thelnfamous1.mobplayeranimator.api;

import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;

public interface ModelPartFollowAccess {

    void mobplayeranimator$setFollowTarget(@Nullable ModelPart target);

    @Nullable ModelPart mobplayeranimator$getFollowTarget();
}
