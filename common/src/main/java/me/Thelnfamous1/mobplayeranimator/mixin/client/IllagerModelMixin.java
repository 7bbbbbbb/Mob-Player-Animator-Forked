package me.Thelnfamous1.mobplayeranimator.mixin.client;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.kosmx.playerAnim.core.impl.AnimationProcessor;
import dev.kosmx.playerAnim.core.util.SetableSupplier;
import dev.kosmx.playerAnim.impl.IMutableModel;
import dev.kosmx.playerAnim.impl.IPlayerModel;
import me.Thelnfamous1.mobplayeranimator.api.FirstPersonTracker;
import me.Thelnfamous1.mobplayeranimator.api.IllagerModelAccess;
import me.Thelnfamous1.mobplayeranimator.api.PlayerAnimatorHelper;
import me.Thelnfamous1.mobplayeranimator.api.part.HumanoidBodyPose;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractIllager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(value = IllagerModel.class, priority = 500) // Lower priority number
public abstract class IllagerModelMixin<T extends AbstractIllager> extends HierarchicalModelMixin<T> implements IPlayerModel, IMutableModel, IllagerModelAccess, FirstPersonTracker {
    @Shadow @Final private ModelPart head;
    @Shadow @Final private ModelPart hat;
    @Shadow @Final private ModelPart leftArm;
    @Shadow @Final private ModelPart rightArm;
    @Shadow @Final private ModelPart rightLeg;
    @Shadow @Final private ModelPart leftLeg;
    @Shadow @Final private ModelPart arms;
    @Unique private HumanoidBodyPose mobplayeranimator$initialBodyPose;

    @Shadow public abstract ModelPart getHead();
    @Shadow public abstract ModelPart getHat();

    @Unique private SetableSupplier<AnimationProcessor> mobplayeranimator$animation = new SetableSupplier<>();
    @Unique private final SetableSupplier<AnimationProcessor> mobplayeranimator$emoteSupplier = new SetableSupplier<>();
    @Unique private boolean mobplayeranimator$firstPersonNext = false;
    @Unique private ModelPart mobplayeranimator$body;

    public IllagerModelMixin(Function<ResourceLocation, RenderType> $$0) {
        super($$0);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void post_init(ModelPart root, CallbackInfo ci) {
        this.mobplayeranimator$body = root.getChild("body");
        PlayerAnimatorHelper.initBend(root, this);
        PlayerAnimatorHelper.initEmoteSupplier(this, this.mobplayeranimator$emoteSupplier);
        this.mobplayeranimator$initialBodyPose = new HumanoidBodyPose(
                this.head.storePose(),
                this.mobplayeranimator$body.storePose(),
                this.leftArm.storePose(), this.rightArm.storePose(),
                this.leftLeg.storePose(), this.rightLeg.storePose());
    }

    @Override
    protected void mobplayeranimator$copyMutatedAttributes(EntityModel<T> otherModel) {
        PlayerAnimatorHelper.setAnimation((IMutableModel) otherModel, this.mobplayeranimator$animation);
    }

    @Override
    protected boolean mobplayeranimator$bendRenderToBuffer(PoseStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        return PlayerAnimatorHelper.bendRenderToBuffer(matrices, vertices, light, overlay, color, this.mobplayeranimator$animation, this.mobplayeranimator$headParts(), this.mobplayeranimator$bodyParts());
    }

    @Unique
    private Iterable<ModelPart> mobplayeranimator$headParts() {
        return ImmutableList.of(this.head);
    }

    @Unique
    private Iterable<ModelPart> mobplayeranimator$bodyParts() {
        return ImmutableList.of(this.mobplayeranimator$body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg, this.hat);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/monster/AbstractIllager;FFFFF)V", at = @At("HEAD"), cancellable = true)
    private void pre_setupAnim(T illager, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        PlayerAnimatorHelper.setDefaultPivot(this);

        if (PlayerAnimatorHelper.isAnimating(illager)) {
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
            this.head.xRot = headPitch * ((float) Math.PI / 180F);

            if (this.riding) {
                this.rightLeg.xRot = -1.4137167F;
                this.rightLeg.yRot = ((float) Math.PI / 10F);
                this.rightLeg.zRot = 0.07853982F;
                this.leftLeg.xRot = -1.4137167F;
                this.leftLeg.yRot = (-(float) Math.PI / 10F);
                this.leftLeg.zRot = -0.07853982F;
            } else {
                this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount * 0.5F;
                this.rightLeg.yRot = 0.0F;
                this.rightLeg.zRot = 0.0F;
                this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount * 0.5F;
                this.leftLeg.yRot = 0.0F;
                this.leftLeg.zRot = 0.0F;
            }

            this.arms.visible = false;
            this.leftArm.visible = true;
            this.rightArm.visible = true;

            PlayerAnimatorHelper.setEmote(this, PlayerAnimatorHelper.getAnimation(illager));

            ci.cancel();
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/monster/AbstractIllager;FFFFF)V", at = @At("TAIL"))
    private void post_setupAnim(T illager, float $$1, float $$2, float $$3, float $$4, float $$5, CallbackInfo ci) {
        PlayerAnimatorHelper.setEmote(this, PlayerAnimatorHelper.getAnimation(illager));
    }

    @WrapWithCondition(method = "setupAnim",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/AnimationUtils;animateZombieArms(Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;ZFF)V")
    )
    private boolean onlyAnimateZombieArmsIfAllowed(ModelPart leftArm, ModelPart rightArm, boolean aggressive, float attackTime, float bob, T illager, float $$1, float $$2, float $$3, float $$4, float $$5) {
        return !PlayerAnimatorHelper.isAnimating(illager);
    }

    @WrapWithCondition(method = "setupAnim",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/AnimationUtils;swingWeaponDown(Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/world/entity/Mob;FF)V")
    )
    private boolean onlyAnimateWeaponSwingIfAllowed(ModelPart rightArm, ModelPart leftArm, Mob mob, float attackTime, float bob) {
        return !PlayerAnimatorHelper.isAnimating(mob);
    }

    @Override
    public void setEmoteSupplier(SetableSupplier<AnimationProcessor> emoteSupplier) {
        this.mobplayeranimator$animation = emoteSupplier;
    }

    @Override
    public SetableSupplier<AnimationProcessor> getEmoteSupplier() {
        return this.mobplayeranimator$animation;
    }

    @Override
    public void playerAnimator_prepForFirstPersonRender() {
        this.mobplayeranimator$setFirstPersonNext(true);
    }

    @Override
    public ModelPart mobplayeranimator$getHead() {
        return this.getHead();
    }

    @Override
    public ModelPart mobplayeranimator$getHat() {
        return this.getHat();
    }

    @Override
    public ModelPart mobplayeranimator$getBody() {
        return this.mobplayeranimator$body;
    }

    @Override
    public ModelPart mobplayeranimator$getLeftArm() {
        return this.leftArm;
    }

    @Override
    public ModelPart mobplayeranimator$getRightArm() {
        return this.rightArm;
    }

    @Override
    public ModelPart mobplayeranimator$getLeftLeg() {
        return this.leftLeg;
    }

    @Override
    public ModelPart mobplayeranimator$getRightLeg() {
        return this.rightLeg;
    }

    @Override
    public ModelPart mobplayeranimator$getArms() {
        return this.arms;
    }

    @Override
    public boolean mobplayeranimator$isFirstPersonNext() {
        return this.mobplayeranimator$firstPersonNext;
    }

    @Override
    public void mobplayeranimator$setFirstPersonNext(boolean firstPersonNext) {
        this.mobplayeranimator$firstPersonNext = firstPersonNext;
    }

    @Override
    public HumanoidBodyPose mobplayeranimator$getInitialBodyPose() {
        return this.mobplayeranimator$initialBodyPose;
    }
}