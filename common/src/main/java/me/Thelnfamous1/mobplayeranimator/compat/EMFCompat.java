package me.Thelnfamous1.mobplayeranimator.compat;

import com.entityplayercompat.api.EntityPlayerCompatApi;
import com.entityplayercompat.api.EntityPlayerCompatApi.DrivenPart;
import me.Thelnfamous1.mobplayeranimator.MobPlayerAnimatorClient;
import me.Thelnfamous1.mobplayeranimator.api.PlayerAnimatorHelper;
import me.Thelnfamous1.mobplayeranimator.api.part.MPABodyPart;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EMFCompat {

    /**
     * EMF compat via EntityPlayerCompat API.
     */
    public static void updateCompatFor(LivingEntity mob) {
        if (!shouldApplyCompat(mob)) {
            return;
        }

        Set<MPABodyPart> animatedParts = PlayerAnimatorHelper.getCurrentlyAnimatedParts(mob);
        if (animatedParts.isEmpty()) {
            return;
        }

        List<DrivenPart> drivenParts = new ArrayList<>();
        for (MPABodyPart part : animatedParts) {
            DrivenPart driven = mapToDrivenPart(part);
            if (driven != null) {
                drivenParts.add(driven);
            }
        }

        if (!drivenParts.isEmpty()) {
            EntityPlayerCompatApi.reportDrivenParts(mob, drivenParts.toArray(new DrivenPart[0]));
        }
    }

    private static boolean shouldApplyCompat(LivingEntity mob) {
        return MobPlayerAnimatorClient.isEPCLoaded()
                && PlayerAnimatorHelper.isAnimating(mob)
                && MobPlayerAnimatorClient.getClientConfigHelper().isAnimatingAnyNonBlacklistedAnimation(mob);
    }

    private static DrivenPart mapToDrivenPart(MPABodyPart part) {
        return switch (part) {
            case BODY, LEFT_ITEM, RIGHT_ITEM -> null;
            case HEAD -> DrivenPart.HEAD;
            case TORSO -> DrivenPart.TORSO;
            case LEFT_ARM -> DrivenPart.LEFT_ARM;
            case RIGHT_ARM -> DrivenPart.RIGHT_ARM;
            case LEFT_LEG -> DrivenPart.LEFT_LEG;
            case RIGHT_LEG -> DrivenPart.RIGHT_LEG;
        };
    }

    // Legacy methods to keep existing calls from breaking

    public static void pauseEMFAnimationsFor(LivingEntity mob, EntityModel<?> model) {
        updateCompatFor(mob);
    }

    public static void lockToVanillaModelFor(LivingEntity mob, EntityModel<?> model) {
        updateCompatFor(mob);
    }

    public static void resumeEMFAnimationsFor(LivingEntity mob, EntityModel<?> model) {
    }

    public static boolean hasEMFAnimations(EntityModel<?> model) {
        return MobPlayerAnimatorClient.isEPCLoaded();
    }
}