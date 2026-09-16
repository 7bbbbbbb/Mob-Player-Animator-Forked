package me.Thelnfamous1.mobplayeranimator.mixin.client.compat.aerial_hell;

import dev.kosmx.playerAnim.core.impl.AnimationProcessor;
import dev.kosmx.playerAnim.core.util.SetableSupplier;
import dev.kosmx.playerAnim.impl.IMutableModel;
import dev.kosmx.playerAnim.impl.IPlayerModel;
import fr.factionbedrock.aerialhell.Client.EntityModels.HumanoidTwoLayerModel;
import fr.factionbedrock.aerialhell.Entity.Monster.AbstractHumanoidMonster;
import me.Thelnfamous1.mobplayeranimator.api.FirstPersonTracker;
import me.Thelnfamous1.mobplayeranimator.api.HumanoidModelAccess;
import me.Thelnfamous1.mobplayeranimator.api.PlayerAnimatorHelper;
import me.Thelnfamous1.mobplayeranimator.api.part.HumanoidBodyPose;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HumanoidTwoLayerModel.class, priority = 2000)
public abstract class HumanoidTwoLayerModelMixin<T extends AbstractHumanoidMonster>
        extends EntityModel<T> implements IPlayerModel, IMutableModel, HumanoidModelAccess, FirstPersonTracker {

    @Shadow @Final private ModelPart head;
    @Shadow @Final private ModelPart head_overlay;
    @Shadow @Final private ModelPart body;
    @Shadow @Final private ModelPart body_overlay;
    @Shadow @Final private ModelPart leftArm;
    @Shadow @Final private ModelPart leftArm_overlay;
    @Shadow @Final private ModelPart rightArm;
    @Shadow @Final private ModelPart rightArm_overlay;
    @Shadow @Final private ModelPart leftLeg;
    @Shadow @Final private ModelPart leftLeg_overlay;
    @Shadow @Final private ModelPart rightLeg;
    @Shadow @Final private ModelPart rightLeg_overlay;

    @Unique private HumanoidBodyPose mobplayeranimator$initialBodyPose;
    @Unique private SetableSupplier<AnimationProcessor> mobplayeranimator$emoteSupplier = new SetableSupplier<>();
    @Unique protected boolean mobplayeranimator$firstPersonNext = false;

    @Inject(method = "<init>(Lnet/minecraft/client/model/geom/ModelPart;)V", at = @At("RETURN"))
    private void post_init(ModelPart root, CallbackInfo ci) {
        PlayerAnimatorHelper.initEmoteSupplier(this, this.mobplayeranimator$emoteSupplier);
        // Map according to actual physical sides (leftArm in Aerial Hell is physically on the right side)
        this.mobplayeranimator$initialBodyPose = new HumanoidBodyPose(
                this.head.storePose(),
                this.body.storePose(),
                this.rightArm.storePose(), this.leftArm.storePose(),
                this.rightLeg.storePose(), this.leftLeg.storePose()
        );
    }

    @Inject(method = "setupAnim(Lfr/factionbedrock/aerialhell/Entity/Monster/AbstractHumanoidMonster;FFFFF)V", at = @At("HEAD"))
    private void pre_setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        PlayerAnimatorHelper.setDefaultPivot(this);
    }

    @Inject(method = "setupAnim(Lfr/factionbedrock/aerialhell/Entity/Monster/AbstractHumanoidMonster;FFFFF)V", at = @At("TAIL"))
    private void post_setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        PlayerAnimatorHelper.setEmote(this, PlayerAnimatorHelper.getAnimation(entity));

        this.head_overlay.copyFrom(this.head);
        this.body_overlay.copyFrom(this.body);
        this.leftArm_overlay.copyFrom(this.leftArm);
        this.rightArm_overlay.copyFrom(this.rightArm);
        this.leftLeg_overlay.copyFrom(this.leftLeg);
        this.rightLeg_overlay.copyFrom(this.rightLeg);
    }

    @Override
    public void setEmoteSupplier(SetableSupplier<AnimationProcessor> supplier) {
        this.mobplayeranimator$emoteSupplier = supplier;
    }

    @Override
    public SetableSupplier<AnimationProcessor> getEmoteSupplier() {
        return this.mobplayeranimator$emoteSupplier;
    }

    @Override
    public void playerAnimator_prepForFirstPersonRender() {
        this.mobplayeranimator$setFirstPersonNext(true);
    }

    @Override
    public ModelPart mobplayeranimator$getHead() {
        return this.head;
    }

    @Override
    public ModelPart mobplayeranimator$getHat() {
        return this.head_overlay;
    }

    @Override
    public ModelPart mobplayeranimator$getBody() {
        return this.body;
    }

    @Override
    public ModelPart mobplayeranimator$getLeftArm() {
        return this.rightArm;
    }

    @Override
    public ModelPart mobplayeranimator$getRightArm() {
        return this.leftArm;
    }

    @Override
    public ModelPart mobplayeranimator$getLeftLeg() {
        return this.rightLeg;
    }

    @Override
    public ModelPart mobplayeranimator$getRightLeg() {
        return this.leftLeg;
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