package me.Thelnfamous1.mobplayeranimator.mixin.client.compat.aerial_hell;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.kosmx.playerAnim.core.impl.AnimationProcessor;
import dev.kosmx.playerAnim.core.util.SetableSupplier;
import dev.kosmx.playerAnim.impl.IMutableModel;
import dev.kosmx.playerAnim.impl.IPlayerModel;
import fr.factionbedrock.aerialhell.Client.EntityModels.LilithModel;
import fr.factionbedrock.aerialhell.Entity.Bosses.LilithEntity;
import me.Thelnfamous1.mobplayeranimator.api.FirstPersonTracker;
import me.Thelnfamous1.mobplayeranimator.api.HumanoidModelAccess;
import me.Thelnfamous1.mobplayeranimator.api.ModelPartFollowAccess;
import me.Thelnfamous1.mobplayeranimator.api.PlayerAnimatorHelper;
import me.Thelnfamous1.mobplayeranimator.api.part.HumanoidBodyPose;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(value = LilithModel.class, priority = 2000)
public abstract class LilithModelMixin extends EntityModel<LilithEntity>
        implements IPlayerModel, IMutableModel, HumanoidModelAccess, FirstPersonTracker {

    @Shadow @Final private ModelPart head;
    @Shadow @Final private ModelPart body;
    @Shadow @Final private ModelPart wingRight;
    @Shadow @Final private ModelPart wingLeft;
    @Shadow @Final private ModelPart legLeft;
    @Shadow @Final private ModelPart legRight;
    @Shadow @Final private ModelPart armLeft;
    @Shadow @Final private ModelPart armRight;

    @Unique private final ModelPart mobplayeranimator$hat = new ModelPart(List.of(), Map.of());
    @Unique private HumanoidBodyPose mobplayeranimator$initialBodyPose;
    @Unique private SetableSupplier<AnimationProcessor> mobplayeranimator$emoteSupplier = new SetableSupplier<>();
    @Unique protected boolean mobplayeranimator$firstPersonNext = false;

    @WrapOperation(method = "createBodyLayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/geom/builders/PartDefinition;addOrReplaceChild(Ljava/lang/String;Lnet/minecraft/client/model/geom/builders/CubeListBuilder;Lnet/minecraft/client/model/geom/PartPose;)Lnet/minecraft/client/model/geom/builders/PartDefinition;"))
    private static PartDefinition mobplayeranimator$moveBodyPivotToNeck(PartDefinition instance, String name, CubeListBuilder cubes, PartPose pose, Operation<PartDefinition> original) {
        if ("body".equals(name)) {
            cubes = CubeListBuilder.create().texOffs(17, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F));
            pose = PartPose.offset(0.0F, 0.0F, 0.0F);
        }
        return original.call(instance, name, cubes, pose);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/model/geom/ModelPart;)V", at = @At("RETURN"))
    private void post_init(ModelPart root, CallbackInfo ci) {
        PlayerAnimatorHelper.initEmoteSupplier(this, this.mobplayeranimator$emoteSupplier);
        this.mobplayeranimator$initialBodyPose = new HumanoidBodyPose(
                this.head.storePose(),
                this.body.storePose(),
                this.armLeft.storePose(), this.armRight.storePose(),
                this.legLeft.storePose(), this.legRight.storePose()
        );
        // The wings are root-level parts, so make them ride along with the (now neck-pivoted) torso.
        // The body sits at the model origin at rest, so this changes nothing unless the torso is animated.
        ((ModelPartFollowAccess) (Object) this.wingLeft).mobplayeranimator$setFollowTarget(this.body);
        ((ModelPartFollowAccess) (Object) this.wingRight).mobplayeranimator$setFollowTarget(this.body);
    }

    @Inject(method = "setupAnim(Lfr/factionbedrock/aerialhell/Entity/Bosses/LilithEntity;FFFFF)V", at = @At("HEAD"))
    private void pre_setupAnim(LilithEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        PlayerAnimatorHelper.setDefaultPivot(this);
    }

    @Inject(method = "setupAnim(Lfr/factionbedrock/aerialhell/Entity/Bosses/LilithEntity;FFFFF)V", at = @At("TAIL"))
    private void post_setupAnim(LilithEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
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
        return this.armLeft;
    }

    @Override
    public ModelPart mobplayeranimator$getRightArm() {
        return this.armRight;
    }

    @Override
    public ModelPart mobplayeranimator$getLeftLeg() {
        return this.legLeft;
    }

    @Override
    public ModelPart mobplayeranimator$getRightLeg() {
        return this.legRight;
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
