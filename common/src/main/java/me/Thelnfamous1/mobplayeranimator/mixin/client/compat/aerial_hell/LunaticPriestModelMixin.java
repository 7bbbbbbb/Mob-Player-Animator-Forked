package me.Thelnfamous1.mobplayeranimator.mixin.client.compat.aerial_hell;

import dev.kosmx.playerAnim.core.impl.AnimationProcessor;
import dev.kosmx.playerAnim.core.util.SetableSupplier;
import dev.kosmx.playerAnim.impl.IMutableModel;
import dev.kosmx.playerAnim.impl.IPlayerModel;
import fr.factionbedrock.aerialhell.Client.EntityModels.LunaticPriestModel;
import fr.factionbedrock.aerialhell.Entity.Bosses.LunaticPriestEntity;
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

import java.util.List;
import java.util.Map;

@Mixin(value = LunaticPriestModel.class, priority = 2000)
public abstract class LunaticPriestModelMixin extends EntityModel<LunaticPriestEntity>
        implements IPlayerModel, IMutableModel, HumanoidModelAccess, FirstPersonTracker {

    @Shadow @Final private ModelPart body;
    @Shadow @Final private ModelPart head;
    @Shadow @Final private ModelPart arm0;
    @Shadow @Final private ModelPart arm1;
    @Shadow @Final private ModelPart leg0;
    @Shadow @Final private ModelPart leg1;

    @Unique private final ModelPart mobplayeranimator$hat = new ModelPart(List.of(), Map.of());
    @Unique private HumanoidBodyPose mobplayeranimator$initialBodyPose;
    @Unique private SetableSupplier<AnimationProcessor> mobplayeranimator$emoteSupplier = new SetableSupplier<>();
    @Unique protected boolean mobplayeranimator$firstPersonNext = false;

    @Inject(method = "<init>(Lnet/minecraft/client/model/geom/ModelPart;)V", at = @At("RETURN"))
    private void post_init(ModelPart root, CallbackInfo ci) {
        PlayerAnimatorHelper.initEmoteSupplier(this, this.mobplayeranimator$emoteSupplier);
        this.mobplayeranimator$initialBodyPose = new HumanoidBodyPose(
                this.head.storePose(),
                this.body.storePose(),
                this.arm0.storePose(), this.arm1.storePose(),
                this.leg0.storePose(), this.leg1.storePose()
        );
    }

    @Inject(method = "setupAnim(Lfr/factionbedrock/aerialhell/Entity/Bosses/LunaticPriestEntity;FFFFF)V", at = @At("HEAD"))
    private void pre_setupAnim(LunaticPriestEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        PlayerAnimatorHelper.setDefaultPivot(this);
    }

    @Inject(method = "setupAnim(Lfr/factionbedrock/aerialhell/Entity/Bosses/LunaticPriestEntity;FFFFF)V", at = @At("TAIL"))
    private void post_setupAnim(LunaticPriestEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        PlayerAnimatorHelper.setEmote(this, PlayerAnimatorHelper.getAnimation(entity));
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
        return this.mobplayeranimator$hat;
    }

    @Override
    public ModelPart mobplayeranimator$getBody() {
        return this.body;
    }

    @Override
    public ModelPart mobplayeranimator$getLeftArm() {
        return this.arm0;
    }

    @Override
    public ModelPart mobplayeranimator$getRightArm() {
        return this.arm1;
    }

    @Override
    public ModelPart mobplayeranimator$getLeftLeg() {
        return this.leg0;
    }

    @Override
    public ModelPart mobplayeranimator$getRightLeg() {
        return this.leg1;
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
