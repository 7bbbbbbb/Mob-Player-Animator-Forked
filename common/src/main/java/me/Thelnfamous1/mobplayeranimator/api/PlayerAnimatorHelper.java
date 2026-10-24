package me.Thelnfamous1.mobplayeranimator.api;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.*;
import dev.kosmx.playerAnim.core.impl.AnimationProcessor;
import dev.kosmx.playerAnim.core.util.Pair;
import dev.kosmx.playerAnim.core.util.SetableSupplier;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.impl.Helper;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import dev.kosmx.playerAnim.impl.IMutableModel;
import dev.kosmx.playerAnim.impl.IUpperPartHelper;
import dev.kosmx.playerAnim.impl.animation.AnimationApplier;
import dev.kosmx.playerAnim.impl.animation.IBendHelper;
import me.Thelnfamous1.mobplayeranimator.api.part.MPABodyPart;
import me.Thelnfamous1.mobplayeranimator.mixin.AbstractFadeModifierAccessor;
import me.Thelnfamous1.mobplayeranimator.mixin.AnimationStackAccessor;
import me.Thelnfamous1.mobplayeranimator.platform.Services;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractIllager;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class PlayerAnimatorHelper {

    @Nullable
    public static AnimationApplier getAnimation(LivingEntity entity){
        if(entity instanceof IAnimatedPlayer animatedPlayer){
            return animatedPlayer.playerAnimator_getAnimation();
        }
        return null;
    }

    public static boolean isAnimating(LivingEntity entity){
        AnimationApplier animation = getAnimation(entity);
        return animation != null && animation.isActive();
    }

    public static void applyBodyRotations(@Nullable AnimationApplier animation, PoseStack matrixStack, float tickDelta) {
        if(animation == null) return;

        animation.setTickDelta(tickDelta);
        if(animation.isActive()){
            //These are additive properties
            Vec3f position = animation.get3DTransform("body", TransformType.POSITION, Vec3f.ZERO);
            matrixStack.translate(position.getX(), position.getY() + 0.7, position.getZ());
            Vec3f rotation = animation.get3DTransform("body", TransformType.ROTATION, Vec3f.ZERO);
            matrixStack.mulPose(Axis.ZP.rotation(rotation.getZ()));    //roll
            matrixStack.mulPose(Axis.YP.rotation(rotation.getY()));    //pitch
            matrixStack.mulPose(Axis.XP.rotation(rotation.getX()));    //yaw
            matrixStack.translate(0, - 0.7d, 0);
        }
    }

    public static void initBend(ModelPart root, HumanoidModelAccess model) {
        if(IllagerAnimationRules.animates(model, MPABodyPart.TORSO)){
            IBendHelper.INSTANCE.initBend(root.getChild("body"), Direction.DOWN);
        }
        IBendHelper.INSTANCE.initBend(root.getChild("right_arm"), Direction.UP);
        IBendHelper.INSTANCE.initBend(root.getChild("left_arm"), Direction.UP);
        if(IllagerAnimationRules.animates(model, MPABodyPart.RIGHT_LEG)){
            IBendHelper.INSTANCE.initBend(root.getChild("right_leg"), Direction.UP);
        }
        if(IllagerAnimationRules.animates(model, MPABodyPart.LEFT_LEG)){
            IBendHelper.INSTANCE.initBend(root.getChild("left_leg"), Direction.UP);
        }
        ((IUpperPartHelper)(Object) model.mobplayeranimator$getRightArm()).setUpperPart(true);
        ((IUpperPartHelper)(Object) model.mobplayeranimator$getLeftArm()).setUpperPart(true);
        ((IUpperPartHelper)(Object) model.mobplayeranimator$getHead()).setUpperPart(true);
        ((IUpperPartHelper)(Object) model.mobplayeranimator$getHat()).setUpperPart(true);
    }

    public static void setAnimation(IMutableModel model, SetableSupplier<AnimationProcessor> animation) {
        if(animation != null) {
            model.setEmoteSupplier(animation);
        }
    }

    public static boolean bendRenderToBuffer(PoseStack matrices, VertexConsumer vertices, int light, int overlay, int color , SetableSupplier<AnimationProcessor> animation, Iterable<ModelPart> headParts, Iterable<ModelPart> bodyParts) {
        if(Helper.isBendEnabled() && animation.get() != null && animation.get().isActive()){
            headParts.forEach((part)->{
                if(! ((IUpperPartHelper)(Object)part).isUpperPart()){
                    part.render(matrices, vertices, light, overlay, color);
                }
            });
            bodyParts.forEach((part)->{
                if(! ((IUpperPartHelper)(Object)part).isUpperPart()){
                    part.render(matrices, vertices, light, overlay, color);
                }
            });

            SetableSupplier<AnimationProcessor> emoteSupplier = animation;
            matrices.pushPose();
            IBendHelper.rotateMatrixStack(matrices, emoteSupplier.get().getBend("body"));
            headParts.forEach((part)->{
                if(((IUpperPartHelper)(Object)part).isUpperPart()){
                    part.render(matrices, vertices, light, overlay, color);
                }
            });
            bodyParts.forEach((part)->{
                if(((IUpperPartHelper)(Object)part).isUpperPart()){
                    part.render(matrices, vertices, light, overlay, color);
                }
            });
            matrices.popPose();
            return true;
        }
        return false;
    }

    public static void initEmoteSupplier(IMutableModel model, SetableSupplier<AnimationProcessor> emoteSupplier) {
        emoteSupplier.set(null);
        model.setEmoteSupplier(emoteSupplier);
    }

    public static void setDefaultPivot(HumanoidModelAccess model){
        /*
        model.mobplayeranimator$getHead().setPos(0.0F, 0.0F, 0.0F);
        model.mobplayeranimator$getHead().setRotation(model.mobplayeranimator$getHead().xRot, model.mobplayeranimator$getHead().yRot, 0.0F);
        model.mobplayeranimator$getBody().setPos(0.0F, 0.0F, 0.0F);
        model.mobplayeranimator$getBody().setRotation(0.0F, 0.0F, 0.0F);
        model.mobplayeranimator$getLeftArm().setPos(5.0F, model.mobplayeranimator$getLeftArm().y, 0.0F);
        model.mobplayeranimator$getRightArm().setPos(-5.0F, model.mobplayeranimator$getRightArm().y, 0.0F);
        model.mobplayeranimator$getLeftLeg().setPos(1.9F, 12.0F, 0.1F);
        model.mobplayeranimator$getRightLeg().setPos(-1.9F, 12.0F, 0.1F);
         */
        if(IllagerAnimationRules.appliesTo(model)){
            // illagers: only the arms are Player Animator's to reset, see IllagerAnimationRules
            model.mobplayeranimator$getInitialBodyPose().poseArms(model);
        } else {
            model.mobplayeranimator$getInitialBodyPose().pose(model);
        }
    }

    public static <T extends HumanoidModelAccess & IMutableModel & FirstPersonTracker> void setEmote(T model, @Nullable AnimationApplier emote) {
        if(emote == null){
            return;
        }
        if(!model.mobplayeranimator$isFirstPersonNext() && emote.isActive()) {
            model.getEmoteSupplier().set(emote);

            if(IllagerAnimationRules.animates(model, MPABodyPart.HEAD)){
                emote.updatePart("head", model.mobplayeranimator$getHead());
                model.mobplayeranimator$getHat().copyFrom(model.mobplayeranimator$getHead());
            }
            if(IllagerAnimationRules.animates(model, MPABodyPart.TORSO)){
                emote.updatePart("torso", model.mobplayeranimator$getBody());
            }
            if(IllagerAnimationRules.animates(model, MPABodyPart.LEFT_ARM)){
                emote.updatePart("leftArm", model.mobplayeranimator$getLeftArm());
            }
            if(IllagerAnimationRules.animates(model, MPABodyPart.RIGHT_ARM)){
                emote.updatePart("rightArm", model.mobplayeranimator$getRightArm());
            }
            if(IllagerAnimationRules.animates(model, MPABodyPart.LEFT_LEG)){
                emote.updatePart("leftLeg", model.mobplayeranimator$getLeftLeg());
            }
            if(IllagerAnimationRules.animates(model, MPABodyPart.RIGHT_LEG)){
                emote.updatePart("rightLeg", model.mobplayeranimator$getRightLeg());
            }
        }
        else {
            model.mobplayeranimator$setFirstPersonNext(false);
            model.getEmoteSupplier().set(null);
            if(IllagerAnimationRules.animates(model, MPABodyPart.TORSO)){
                resetBend(model.mobplayeranimator$getBody());
            }
            if(IllagerAnimationRules.animates(model, MPABodyPart.LEFT_ARM)){
                resetBend(model.mobplayeranimator$getLeftArm());
            }
            if(IllagerAnimationRules.animates(model, MPABodyPart.RIGHT_ARM)){
                resetBend(model.mobplayeranimator$getRightArm());
            }
            if(IllagerAnimationRules.animates(model, MPABodyPart.LEFT_LEG)){
                resetBend(model.mobplayeranimator$getLeftLeg());
            }
            if(IllagerAnimationRules.animates(model, MPABodyPart.RIGHT_LEG)){
                resetBend(model.mobplayeranimator$getRightLeg());
            }
        }
    }

    private static void resetBend(ModelPart part) {
        IBendHelper.INSTANCE.bend(part, null);
    }

    public static boolean recursiveAnimationTest(IAnimation animation, Predicate<KeyframeAnimationPlayer> animationTest) {
        if (animation instanceof KeyframeAnimationPlayer keyframeAnimationPlayer) {
            return animationTest.test(keyframeAnimationPlayer);
        } else if (animation instanceof ModifierLayer<?> modifierLayer) {
            IAnimation layerAnimation = modifierLayer.getAnimation();
            return layerAnimation != null && recursiveAnimationTest(layerAnimation, animationTest);
        } else if (animation instanceof AnimationContainer<?> animationContainer) {
            IAnimation containerAnim = animationContainer.getAnim();
            return containerAnim != null && recursiveAnimationTest(containerAnim, animationTest);
        } else if (animation instanceof AnimationStack animationStack) {
            ArrayList<Pair<Integer, IAnimation>> prioritizedLayers = ((AnimationStackAccessor) animationStack).mobplayeranimator$getLayers();
            for (Pair<Integer, IAnimation> prioritizedLayer : prioritizedLayers) {
                IAnimation layer = prioritizedLayer.getRight();
                if (recursiveAnimationTest(layer, animationTest)) return true;
            }
            return false;
        }
        return false;
    }

    private static void recursiveAnimationConsume(IAnimation animation, Consumer<KeyframeAnimationPlayer> animationConsumer) {
        if (animation instanceof KeyframeAnimationPlayer keyframeAnimationPlayer) {
            animationConsumer.accept(keyframeAnimationPlayer);
        } else if (animation instanceof ModifierLayer<?> modifierLayer) {
            IAnimation layerAnimation = modifierLayer.getAnimation();
            if(layerAnimation != null) recursiveAnimationConsume(layerAnimation, animationConsumer);
        } else if (animation instanceof AnimationContainer<?> animationContainer) {
            IAnimation containerAnim = animationContainer.getAnim();
            if(containerAnim != null) recursiveAnimationConsume(containerAnim, animationConsumer);
            // While a fade runs, the animation being faded away from still shapes the pose
            if(animation instanceof AbstractFadeModifierAccessor fadeModifier){
                IAnimation beginAnimation = fadeModifier.mobplayeranimator$getBeginAnimation();
                if(beginAnimation != null) recursiveAnimationConsume(beginAnimation, animationConsumer);
            }
        } else if (animation instanceof AnimationStack animationStack) {
            ArrayList<Pair<Integer, IAnimation>> prioritizedLayers = ((AnimationStackAccessor) animationStack).mobplayeranimator$getLayers();
            for (Pair<Integer, IAnimation> prioritizedLayer : prioritizedLayers) {
                IAnimation layer = prioritizedLayer.getRight();
                recursiveAnimationConsume(layer, animationConsumer);
            }
        }
    }

    public static Set<MPABodyPart> getCurrentlyAnimatedParts(LivingEntity entity){
        if(entity instanceof IAnimatedPlayer animatedPlayer){
            AnimationStack animationStack = animatedPlayer.getAnimationStack();
            // Illagers only count the parts IllagerAnimationRules lets Player Animator animate
            return IllagerAnimationRules.filter(entity, getCurrentlyAnimatedParts(animationStack));
        }
        return Set.of();
    }

    public static Set<MPABodyPart> getCurrentlyAnimatedParts(IAnimation animation){
        Set<MPABodyPart> activeParts = new HashSet<>();
        recursiveAnimationConsume(animation, keyframeAnimationPlayer -> addCurrentlyTransformedParts(keyframeAnimationPlayer, activeParts));
        return activeParts;
    }

    private static void addCurrentlyTransformedParts(KeyframeAnimationPlayer keyframeAnimationPlayer, Set<MPABodyPart> activeParts) {
        if(!keyframeAnimationPlayer.isActive()) return;

        for(Map.Entry<String, KeyframeAnimationPlayer.BodyPart> part : keyframeAnimationPlayer.bodyParts.entrySet()){
            if(isActive(part.getValue())){
                String partName = part.getKey();
                MPABodyPart mpaBodyPart = MPABodyPart.byName(partName);
                if(mpaBodyPart != null){
                    activeParts.add(mpaBodyPart);
                } else if(Services.PLATFORM.isDevelopmentEnvironment()){
                    throw new IllegalArgumentException("Invalid part name for MPABodyPart: " + partName);
                }
            }
        }
    }

    public static boolean isActive(KeyframeAnimationPlayer.BodyPart bodyPart) {
        return bodyPart.part != null && bodyPart.part.isEnabled();
    }

    @Unique
    public static <T extends HumanoidModelAccess & IMutableModel & FirstPersonTracker> T asMPAModel(HumanoidModelAccess model){
        return (T) model;
    }
}
