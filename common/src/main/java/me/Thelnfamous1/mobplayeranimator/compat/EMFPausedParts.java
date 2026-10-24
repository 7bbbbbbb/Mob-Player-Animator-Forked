package me.Thelnfamous1.mobplayeranimator.compat;

import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;
import traben.entity_model_features.utils.EMFAnimationPauseHandler;

public final class EMFPausedParts {

    private EMFPausedParts() {
    }

    public static @Nullable ModelPart[] get() {
        return EMFAnimationPauseHandler.getEntityPartsAnimPaused();
    }

    public static boolean isPaused(Object part) {
        ModelPart[] pausedParts = get();
        if (pausedParts != null) {
            for (ModelPart paused : pausedParts) {
                if (paused == part) {
                    return true;
                }
            }
        }
        return false;
    }
}
