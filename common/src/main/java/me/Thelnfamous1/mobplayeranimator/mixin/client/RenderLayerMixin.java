package me.Thelnfamous1.mobplayeranimator.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.Thelnfamous1.mobplayeranimator.MobPlayerAnimatorClient;
import me.Thelnfamous1.mobplayeranimator.compat.EMFCompat;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderLayer.class)
public abstract class RenderLayerMixin {

    @Inject(method = "renderColoredCutoutModel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            require = 0)
    private static <T extends LivingEntity> void mobplayeranimator$preLayerModelRender(EntityModel<T> model, ResourceLocation textureLocation, PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, int color, CallbackInfo ci) {
        if (MobPlayerAnimatorClient.isEMFLoaded()) {
            EMFCompat.pauseEMFAnimationsForLayer(entity, model);
        }
    }

    @Inject(method = "renderColoredCutoutModel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", shift = At.Shift.AFTER),
            require = 0)
    private static <T extends LivingEntity> void mobplayeranimator$postLayerModelRender(EntityModel<T> model, ResourceLocation textureLocation, PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, int color, CallbackInfo ci) {
        if (MobPlayerAnimatorClient.isEMFLoaded()) {
            EMFCompat.resumeEMFAnimationsForLayer(entity, model);
        }
    }
}