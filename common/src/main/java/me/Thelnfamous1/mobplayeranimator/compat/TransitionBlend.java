package me.Thelnfamous1.mobplayeranimator.compat;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TransitionBlend {
    private static final long BLEND_MILLIS = 250L;
    private static final Map<UUID, Map<ModelPart, Long>> LAST_PAUSED = new ConcurrentHashMap<>();
    // the pose a part was last drawn in, for parts whose drawn pose is not the pose stored in the part
    private static final Map<UUID, Map<ModelPart, Pose>> LAST_RENDERED = new ConcurrentHashMap<>();
    private static final List<Blend> ACTIVE = new ArrayList<>();

    private TransitionBlend() {
    }

    public record Pose(float x, float y, float z, float xRot, float yRot, float zRot) {
        public static Pose of(ModelPart part) {
            return new Pose(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot);
        }
    }

    private record Blend(ModelPart part, float alpha, Pose from) {
    }

    public static void cancel(LivingEntity mob) {
        LAST_PAUSED.remove(mob.getUUID());
        ACTIVE.clear();
    }

    public static void clearCache() {
        LAST_PAUSED.clear();
        LAST_RENDERED.clear();
        ACTIVE.clear();
    }


    public static void recordRenderedPose(LivingEntity mob, ModelPart part, Pose drawnPose) {
        LAST_RENDERED.computeIfAbsent(mob.getUUID(), uuid -> new ConcurrentHashMap<>()).put(part, drawnPose);
    }

    public static void markPaused(LivingEntity mob, Collection<ModelPart> pausedParts) {
        if (pausedParts.isEmpty()) {
            return;
        }
        Map<ModelPart, Long> byPart = LAST_PAUSED.computeIfAbsent(mob.getUUID(), uuid -> new ConcurrentHashMap<>());
        long now = System.currentTimeMillis();
        for (ModelPart part : pausedParts) {
            byPart.put(part, now);
        }
        for (int i = 0; i < ACTIVE.size(); i++) {
            Blend blend = ACTIVE.get(i);
            if (pausedParts.contains(blend.part())) {
                ACTIVE.set(i, new Blend(blend.part(), blend.alpha(), Pose.of(blend.part())));
            }
        }
    }

    public static void beginRender(LivingEntity mob) {
        ACTIVE.clear();
        Map<ModelPart, Long> byPart = LAST_PAUSED.get(mob.getUUID());
        if (byPart == null || byPart.isEmpty()) {
            return;
        }
        Map<ModelPart, Pose> rendered = LAST_RENDERED.get(mob.getUUID());
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<ModelPart, Long>> it = byPart.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ModelPart, Long> entry = it.next();
            long elapsed = now - entry.getValue();
            ModelPart part = entry.getKey();
            if (elapsed >= BLEND_MILLIS) {
                it.remove();
                if (rendered != null) {
                    rendered.remove(part);
                }
            } else {
                Pose from = rendered != null && rendered.containsKey(part) ? rendered.get(part) : Pose.of(part);
                ACTIVE.add(new Blend(part, (float) elapsed / (float) BLEND_MILLIS, from));
            }
        }
        if (byPart.isEmpty()) {
            LAST_PAUSED.remove(mob.getUUID());
        }
        if (rendered != null && rendered.isEmpty()) {
            LAST_RENDERED.remove(mob.getUUID());
        }
    }

    public static void endRender() {
        ACTIVE.clear();
    }

    public static boolean hasBlends() {
        return !ACTIVE.isEmpty();
    }

    public static void applyBlends() {
        for (Blend blend : ACTIVE) {
            ModelPart part = blend.part();
            float alpha = blend.alpha();
            Pose from = blend.from();
            part.x = blend(alpha, from.x(), part.x);
            part.y = blend(alpha, from.y(), part.y);
            part.z = blend(alpha, from.z(), part.z);
            part.xRot = blendAngle(alpha, from.xRot(), part.xRot);
            part.yRot = blendAngle(alpha, from.yRot(), part.yRot);
            part.zRot = blendAngle(alpha, from.zRot(), part.zRot);
        }
    }

    public static float blendAngle(float alpha, float from, float to) {
        return from + alpha * Mth.wrapDegrees((float) Math.toDegrees(to - from)) * ((float) Math.PI / 180F);
    }

    public static float blend(float alpha, float from, float to) {
        return from + alpha * (to - from);
    }
}
