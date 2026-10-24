package me.Thelnfamous1.mobplayeranimator.mixin.client.compat.illager_invasion;

import dev.kosmx.playerAnim.core.impl.AnimationProcessor;
import dev.kosmx.playerAnim.core.util.SetableSupplier;
import dev.kosmx.playerAnim.impl.IMutableModel;
import dev.kosmx.playerAnim.impl.IPlayerModel;
import fuzs.illagerinvasion.client.model.ArmoredIllagerEntityModel;
import me.Thelnfamous1.mobplayeranimator.api.FirstPersonTracker;
import me.Thelnfamous1.mobplayeranimator.api.IllagerAnimationRules;
import me.Thelnfamous1.mobplayeranimator.api.IllagerModelAccess;
import me.Thelnfamous1.mobplayeranimator.api.PlayerAnimatorHelper;
import me.Thelnfamous1.mobplayeranimator.api.part.HumanoidBodyPose;
import me.Thelnfamous1.mobplayeranimator.mixin.client.HierarchicalModelMixin;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractIllager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(value = ArmoredIllagerEntityModel.class, priority = 500)
public abstract class ArmoredIllagerEntityModelMixin<T extends AbstractIllager> extends HierarchicalModelMixin<T> implements IPlayerModel, IMutableModel, IllagerModelAccess, FirstPersonTracker {
    @Shadow @Final private ModelPart head;
    @Shadow @Final private ModelPart hat;
    @Shadow @Final private ModelPart leftArm;
    @Shadow @Final private ModelPart rightArm;
    @Shadow @Final private ModelPart rightLeg;
    @Shadow @Final private ModelPart leftLeg;
    @Shadow @Final private ModelPart arms;
    @Shadow @Final private ModelPart body;
    @Shadow @Final private ModelPart leftShoulderpad;
    @Shadow @Final private ModelPart rightShoulderpad;

    @Shadow public abstract ModelPart getHead();
    @Shadow public abstract ModelPart getHat();

    @Unique private SetableSupplier<AnimationProcessor> mobplayeranimator$animation = new SetableSupplier<>();
    @Unique private final SetableSupplier<AnimationProcessor> mobplayeranimator$emoteSupplier = new SetableSupplier<>();
    @Unique private boolean mobplayeranimator$firstPersonNext = false;
    @Unique private HumanoidBodyPose mobplayeranimator$initialBodyPose;

    public ArmoredIllagerEntityModelMixin(Function<ResourceLocation, RenderType> function) {
        super(function);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void post_init(ModelPart root, CallbackInfo ci) {
        PlayerAnimatorHelper.initBend(root, this);
        PlayerAnimatorHelper.initEmoteSupplier(this, this.mobplayeranimator$emoteSupplier);
        this.mobplayeranimator$initialBodyPose = new HumanoidBodyPose(
                this.head.storePose(),
                this.body.storePose(),
                this.leftArm.storePose(), this.rightArm.storePose(),
                this.leftLeg.storePose(), this.rightLeg.storePose());
    }

    @Override
    protected void mobplayeranimator$copyMutatedAttributes(EntityModel<T> otherModel) {
        PlayerAnimatorHelper.setAnimation((IMutableModel) otherModel, this.mobplayeranimator$animation);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/monster/AbstractIllager;FFFFF)V", at = @At("HEAD"))
    private void pre_setupAnim(T illager, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        PlayerAnimatorHelper.setDefaultPivot(this);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/monster/AbstractIllager;FFFFF)V", at = @At("TAIL"))
    private void post_setupAnim(T illager, float $$1, float $$2, float $$3, float $$4, float $$5, CallbackInfo ci) {
        if (IllagerAnimationRules.animatesAnyArm(illager)) {
            this.arms.visible = false;
            this.leftArm.visible = true;
            this.rightArm.visible = true;
            this.leftShoulderpad.visible = true;
            this.rightShoulderpad.visible = true;
        }
        PlayerAnimatorHelper.setEmote(this, PlayerAnimatorHelper.getAnimation(illager));
        this.rightShoulderpad.copyFrom(this.rightArm);
        this.leftShoulderpad.copyFrom(this.leftArm);
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
        return this.body;
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