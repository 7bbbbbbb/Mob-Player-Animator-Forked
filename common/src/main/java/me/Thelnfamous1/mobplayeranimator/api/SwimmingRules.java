package me.Thelnfamous1.mobplayeranimator.api;

import me.Thelnfamous1.mobplayeranimator.api.part.MPABodyPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Drowned;

import java.util.Set;


public final class SwimmingRules {

    public static final boolean DROWNED_IGNORES_SWIM_POSE_WHILE_ATTACKING = true;

    public static final boolean DROWNED_ARMS_FOLLOW_BODY_WHILE_SWIMMING = false;

    private SwimmingRules() {
    }

    public static boolean isSwimming(LivingEntity entity) {
        return entity.isSwimming() || (entity.isInWater() && !entity.onGround());
    }

    public static boolean drownedIgnoresSwimPose(LivingEntity entity) {
        return DROWNED_IGNORES_SWIM_POSE_WHILE_ATTACKING && entity instanceof Drowned && isSwimming(entity)
                && PlayerAnimatorHelper.isAnimating(entity) && animatesAnArm(entity);
    }

    public static boolean armsIgnoreBody(LivingEntity entity) {
        return !DROWNED_ARMS_FOLLOW_BODY_WHILE_SWIMMING && entity instanceof Drowned && isSwimming(entity);
    }

    private static boolean animatesAnArm(LivingEntity entity) {
        Set<MPABodyPart> parts = PlayerAnimatorHelper.getCurrentlyAnimatedParts(entity);
        return parts.contains(MPABodyPart.LEFT_ARM) || parts.contains(MPABodyPart.RIGHT_ARM);
    }
}
