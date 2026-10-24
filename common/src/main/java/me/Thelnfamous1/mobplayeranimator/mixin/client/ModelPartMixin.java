package me.Thelnfamous1.mobplayeranimator.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.Thelnfamous1.mobplayeranimator.api.ModelPartFollowAccess;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public abstract class ModelPartMixin implements ModelPartFollowAccess {

    @Unique
    private @Nullable ModelPart mobplayeranimator$followTarget;

    @Override
    public void mobplayeranimator$setFollowTarget(@Nullable ModelPart target) {
        this.mobplayeranimator$followTarget = target;
    }

    @Override
    public @Nullable ModelPart mobplayeranimator$getFollowTarget() {
        return this.mobplayeranimator$followTarget;
    }

    @Inject(method = "translateAndRotate", at = @At("HEAD"))
    private void mobplayeranimator$applyFollowTarget(PoseStack poseStack, CallbackInfo ci) {
        if (this.mobplayeranimator$followTarget != null) {
            this.mobplayeranimator$followTarget.translateAndRotate(poseStack);
        }
    }
}
