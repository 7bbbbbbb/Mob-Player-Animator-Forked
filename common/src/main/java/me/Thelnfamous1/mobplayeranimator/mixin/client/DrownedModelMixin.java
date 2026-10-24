package me.Thelnfamous1.mobplayeranimator.mixin.client;

import me.Thelnfamous1.mobplayeranimator.api.SwimmingRules;
import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrownedModel.class)
public abstract class DrownedModelMixin<T extends Zombie> extends ZombieModel<T> {

    protected DrownedModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/monster/Zombie;FFFFF)V", at = @At("HEAD"), require = 0)
    private void mobplayeranimator$ignoreSwimPoseWhileAttacking(T drowned, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (SwimmingRules.drownedIgnoresSwimPose(drowned)) {
            this.swimAmount = 0.0F;
        }
    }
}
