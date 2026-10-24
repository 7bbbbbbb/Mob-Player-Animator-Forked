package me.Thelnfamous1.mobplayeranimator.api;

import me.Thelnfamous1.mobplayeranimator.api.part.MPABodyPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractIllager;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public final class IllagerAnimationRules {
    public static final Set<MPABodyPart> ANIMATED_PARTS = Collections.unmodifiableSet(
            EnumSet.of(MPABodyPart.LEFT_ARM, MPABodyPart.RIGHT_ARM));

    private IllagerAnimationRules() {
    }

    public static boolean appliesTo(HumanoidModelAccess model) {
        return model instanceof IllagerModelAccess;
    }

    public static boolean appliesTo(LivingEntity entity) {
        return entity instanceof AbstractIllager;
    }

    public static boolean animates(HumanoidModelAccess model, MPABodyPart part) {
        return !appliesTo(model) || ANIMATED_PARTS.contains(part);
    }

    public static Set<MPABodyPart> filter(LivingEntity entity, Set<MPABodyPart> animatedParts) {
        if (!appliesTo(entity)) {
            return animatedParts;
        }
        Set<MPABodyPart> filtered = new HashSet<>(animatedParts);
        filtered.retainAll(ANIMATED_PARTS);
        return filtered;
    }

    public static boolean animatesAnyArm(LivingEntity entity) {
        return !PlayerAnimatorHelper.getCurrentlyAnimatedParts(entity).isEmpty();
    }
}
