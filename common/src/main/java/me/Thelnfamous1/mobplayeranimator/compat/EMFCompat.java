package me.Thelnfamous1.mobplayeranimator.compat;

import me.Thelnfamous1.mobplayeranimator.Constants;
import me.Thelnfamous1.mobplayeranimator.MobPlayerAnimatorClient;
import me.Thelnfamous1.mobplayeranimator.api.HumanoidModelAccess;
import me.Thelnfamous1.mobplayeranimator.api.IllagerAnimationRules;
import me.Thelnfamous1.mobplayeranimator.api.IllagerModelAccess;
import me.Thelnfamous1.mobplayeranimator.api.ModelPartFollowAccess;
import me.Thelnfamous1.mobplayeranimator.api.PlayerAnimatorHelper;
import me.Thelnfamous1.mobplayeranimator.api.SwimmingRules;
import me.Thelnfamous1.mobplayeranimator.api.part.MPABodyPart;
import me.Thelnfamous1.mobplayeranimator.api.part.MPAModelModifier;
import me.Thelnfamous1.mobplayeranimator.api.part.MPAModelPose;
import me.Thelnfamous1.mobplayeranimator.mixin.client.ModelPartAccessor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import traben.entity_model_features.EMFAnimationApi;
import traben.entity_model_features.models.IEMFModel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

public final class EMFCompat {

    private static final List<MPABodyPart> GROUPS = List.of(
            MPABodyPart.HEAD, MPABodyPart.TORSO,
            MPABodyPart.LEFT_ARM, MPABodyPart.RIGHT_ARM,
            MPABodyPart.LEFT_LEG, MPABodyPart.RIGHT_LEG);

    private static final Set<UUID> EMF_ANIMATIONS_HALTED = new HashSet<>();
    private static final Set<UUID> EMF_VANILLA_MODELS_FORCED = new HashSet<>();
    // vindicators whose "aggressive" flag was switched on for the render that is in progress
    private static final Set<UUID> AGGRESSION_FORCED = new HashSet<>();
    private static final Map<UUID, MPAModelPose> EMF_MODEL_POSES = new HashMap<>();
    private static final Map<UUID, List<ModelPart>> FOLLOWED_PARTS = new ConcurrentHashMap<>();
    private static final Map<UUID, List<SavedPartPose>> BAKED_POSES = new ConcurrentHashMap<>();
    // parts whose rotation EMF may not change, with the rotation to put back once EMF has animated the model
    private static final Map<ModelPart, float[]> ROTATION_HELD = new IdentityHashMap<>();
    // the entity and model currently being rendered, so EMF's animation hook only acts on that one
    private static final Set<Class<?>> LOGGED_LAYERS = new HashSet<>();
    private static RenderContext activeRender;
    // the second model a render layer is drawing for that entity right now, if any
    private static LayerRender activeLayer;

    private EMFCompat() {
    }

    public static void clearCache() {
        TransitionBlend.clearCache();
        EMFVanillaParts.clearCache();
        FOLLOWED_PARTS.clear();
        AGGRESSION_FORCED.clear();
        BAKED_POSES.clear();
        ROTATION_HELD.clear();
        activeRender = null;
        activeLayer = null;
    }

    public static void registerVariables(){
        registerVariable("is_playeranimator_animation_active", "is_playeranimator_animation_active",
                EMFCompat::isPlayerAnimatorAnimationActive);
        registerPartVariable("head", MPABodyPart.HEAD);
        registerPartVariable("body", MPABodyPart.TORSO);
        registerPartVariable("torso", MPABodyPart.TORSO);
        registerPartVariable("left_arm", MPABodyPart.LEFT_ARM);
        registerPartVariable("right_arm", MPABodyPart.RIGHT_ARM);
        registerPartVariable("left_leg", MPABodyPart.LEFT_LEG);
        registerPartVariable("right_leg", MPABodyPart.RIGHT_LEG);
        try {
            EMFAnimationApi.registerAnimationHook(new EMFAnimationApi.EMFAnimationHook() {
                @Override
                public void onAnimationEnd(EMFAnimationApi.EMFAnimationHook.AnimationContext context, boolean wasCancelledByHook) {
                    try {
                        onEmfAnimationEnd(context);
                    } catch (RuntimeException e) {
                        Constants.LOG.warn("Failed to finish an EMF animation hand-back for a Player Animator animation", e);
                    }
                }
            });
        } catch (Exception e) {
            Constants.LOG.warn("Failed to register the animation hook with Entity Model Features", e);
        }
    }

    private static void onEmfAnimationEnd(EMFAnimationApi.EMFAnimationHook.AnimationContext context) {
        RenderContext render = activeRender;
        if(render == null || (ROTATION_HELD.isEmpty() && !TransitionBlend.hasBlends())){
            return;
        }
        var state = context.activeState();
        if(state == null || !(state.emfEntity() instanceof LivingEntity living) || living != render.entity()){
            return;
        }
        if(render.root() == null || context.animatingModelRoot() != render.root()){
            return;
        }
        TransitionBlend.applyBlends();
        for(Map.Entry<ModelPart, float[]> held : ROTATION_HELD.entrySet()){
            ModelPart part = held.getKey();
            float[] rotation = held.getValue();
            part.xRot = rotation[0];
            part.yRot = rotation[1];
            part.zRot = rotation[2];
        }
    }

    private static void registerPartVariable(String name, MPABodyPart part) {
        registerVariable("is_playeranimator_" + name + "_active", "is_playeranimator_" + name + "_active", () -> {
            if (EMFAnimationApi.getCurrentEntity() instanceof LivingEntity living) {
                return claimedParts(PlayerAnimatorHelper.getCurrentlyAnimatedParts(living)).contains(part);
            }
            return false;
        });
    }

    private static void registerVariable(String name, String explanationKey, BooleanSupplier supplier) {
        try {
            // some EMF versions declare this method as "throws Exception" (it throws if a parameter is null)
            EMFAnimationApi.registerSingletonAnimationVariable(Constants.MOD_ID, name, emfTranslationKey(explanationKey), supplier);
        } catch (Exception e) {
            Constants.LOG.warn("Failed to register the " + name + " variable with Entity Model Features", e);
        }
    }

    public static boolean isPlayerAnimatorAnimationActive() {
        if (EMFAnimationApi.getCurrentEntity() instanceof LivingEntity livingEntity) {
            return PlayerAnimatorHelper.isAnimating(livingEntity)
                    && MobPlayerAnimatorClient.getClientConfigHelper().isAnimatingAnyNonBlacklistedAnimation(livingEntity);
        } else {
            return false;
        }
    }

    private static String emfTranslationKey(String key) {
        return "entity_model_features.config.variable_explanation." + Constants.MOD_ID + "." + key;
    }

    private static boolean isHandled(LivingEntity entity) {
        return !(entity instanceof Player);
    }

    public static boolean hasEMFAnimations(EntityModel<?> model){
        return EMFAnimationApi.isModelAnimatedByEMF(model);
    }

    public static boolean isRotationPaused(ModelPart part) {
        return ROTATION_HELD.containsKey(part);
    }

    public static void lockToVanillaModelFor(LivingEntity mob, EntityModel<?> model){
        if(isHandled(mob) && hasEMFAnimations(model) && shouldHaltEMFAnimations(mob) && MobPlayerAnimatorClient.getClientConfigHelper().isVanillaModelForcedForEMF(mob)){
            EMFAnimationApi.lockEntityToVanillaModel(EMFAnimationApi.emfEntityOf(mob));
            EMF_VANILLA_MODELS_FORCED.add(mob.getUUID());
        }
    }

    private static boolean shouldHaltEMFAnimations(LivingEntity mob) {
        return PlayerAnimatorHelper.isAnimating(mob)
                && MobPlayerAnimatorClient.getClientConfig().is_emf_animation_halt_enabled
                && MobPlayerAnimatorClient.getClientConfigHelper().isAnimationHaltedForEMF(mob)
                && MobPlayerAnimatorClient.getClientConfigHelper().isAnimatingAnyNonBlacklistedAnimation(mob);
    }

    public static void forceAggressionWhileAnimated(LivingEntity entity, EntityModel<?> model) {
        if(!(entity instanceof Vindicator vindicator) || vindicator.isAggressive() || !hasEMFAnimations(model)
                || !PlayerAnimatorHelper.isAnimating(vindicator) || !IllagerAnimationRules.animatesAnyArm(vindicator)){
            return;
        }
        vindicator.setAggressive(true);
        AGGRESSION_FORCED.add(vindicator.getUUID());
    }

    private static void restoreAggression(LivingEntity entity) {
        if(!AGGRESSION_FORCED.isEmpty() && AGGRESSION_FORCED.remove(entity.getUUID()) && entity instanceof Mob mob){
            mob.setAggressive(false);
        }
    }

    public static void pauseEMFAnimationsFor(LivingEntity mob, EntityModel<?> model) {
        if(!isHandled(mob)){
            return;
        }
        TransitionBlend.beginRender(mob);

        if(!hasEMFAnimations(model)){
            return;
        }
        ModelPart root = rootOf(model);
        activeRender = new RenderContext(mob, root);
        if(!shouldHaltEMFAnimations(mob) || MobPlayerAnimatorClient.getClientConfigHelper().isVanillaModelForcedForEMF(mob)){
            return;
        }
        UUID uuid = mob.getUUID();
        EMF_ANIMATIONS_HALTED.add(uuid);

        PausePlan plan = planPause(mob, model, root);
        if(!plan.followed().isEmpty()){
            FOLLOWED_PARTS.put(uuid, plan.followed());
        }
        Set<ModelPart> partsToPause = plan.parts();

        // configured per-entity model changes (visibility, offsets) for the duration of the render
        MPAModelModifier modelModifier = MobPlayerAnimatorClient.getClientConfigHelper().getModelModifier(mob.getType());
        if(modelModifier != null && root != null){
            // store the original poses for affected parts
            EMF_MODEL_POSES.put(uuid, new MPAModelPose(root, modelModifier, plan.rawParts()));
            // Now apply modifications to specific parts
            partsToPause.addAll(modelModifier.modify(root, plan.rawParts()).values());
        }
        pauseParts(mob, partsToPause);
    }

    public static void pauseEMFAnimationsForLayer(LivingEntity mob, EntityModel<?> layerModel) {
        RenderContext base = activeRender;
        if(!isHandled(mob) || base == null || base.entity() != mob || !hasEMFAnimations(layerModel)){
            return;
        }
        ModelPart root = rootOf(layerModel);
        if(root == null || root == base.root()){
            // the entity's own model, not a layer's
            return;
        }
        // the entity's own model has been rendered, so what it held is finished
        TransitionBlend.beginRender(mob);
        ROTATION_HELD.clear();
        LayerRender layer = new LayerRender(layerModel, base, new ArrayList<>());
        activeLayer = layer;
        activeRender = new RenderContext(mob, root);
        if(!shouldHaltEMFAnimations(mob) || MobPlayerAnimatorClient.getClientConfigHelper().isVanillaModelForcedForEMF(mob)){
            return;
        }
        PausePlan plan = planPause(mob, layerModel, root);
        layer.followed().addAll(plan.followed());
        pauseParts(mob, plan.parts());
        if(!plan.parts().isEmpty() && LOGGED_LAYERS.add(layerModel.getClass())){
            Constants.LOG.debug("Player Animator is also driving the {} that a render layer draws for {}", layerModel.getClass().getSimpleName(), mob.getType());
        }
    }

    public static void resumeEMFAnimationsForLayer(LivingEntity mob, EntityModel<?> layerModel) {
        LayerRender layer = activeLayer;
        if(layer == null || layer.model() != layerModel){
            return;
        }
        activeLayer = null;
        activeRender = layer.previous();
        ROTATION_HELD.clear();
        for(ModelPart part : layer.followed()){
            ModelPartFollowAccess bridge = (ModelPartFollowAccess) (Object) part;
            ModelPart target = bridge.mobplayeranimator$getFollowTarget();
            if(target != null){
                // a layer model is not baked, but its parts are drawn the same way and are handed back the same way
                TransitionBlend.recordRenderedPose(mob, part, composeWithTarget(part, target));
            }
            bridge.mobplayeranimator$setFollowTarget(null);
        }
    }

    private static TransitionBlend.Pose composeWithTarget(ModelPart part, ModelPart target) {
        Quaternionf rotation = new Quaternionf().rotationZYX(target.zRot, target.yRot, target.xRot);
        Vector3f pivot = rotation.transform(new Vector3f(part.x * target.xScale, part.y * target.yScale, part.z * target.zScale));
        rotation.rotateZYX(part.zRot, part.yRot, part.xRot);
        Vector3f euler = eulerAnglesZYX(rotation, new Vector3f());
        return new TransitionBlend.Pose(target.x + pivot.x, target.y + pivot.y, target.z + pivot.z, euler.x, euler.y, euler.z);
    }

    private static ModelPart rootOf(EntityModel<?> model) {
        return model instanceof IEMFModel emfModel ? emfModel.emf$getEMFRootModel() : null;
    }

    private record PausePlan(Set<ModelPart> parts, List<ModelPart> followed, Set<MPABodyPart> rawParts) {
    }

    private static PausePlan planPause(LivingEntity mob, EntityModel<?> model, ModelPart root) {
        Set<MPABodyPart> rawParts = PlayerAnimatorHelper.getCurrentlyAnimatedParts(mob);
        EnumSet<MPABodyPart> animatedParts = claimedParts(rawParts);
        Map<String, ModelPart> partsByName = EMFVanillaParts.byName(root);

        // a head nobody animates keeps looking where the mob looks; but if the torso moves, the head goes with it
        boolean headUnclaimed = !animatedParts.isEmpty() && !animatedParts.contains(MPABodyPart.HEAD);
        boolean torsoClaimed = animatedParts.contains(MPABodyPart.TORSO);
        boolean headKeepsVanillaLook = headUnclaimed && !torsoClaimed;
        if(headUnclaimed && torsoClaimed){
            animatedParts.add(MPABodyPart.HEAD);
        }
        if(headKeepsVanillaLook){
            List<ModelPart> headParts = new ArrayList<>();
            collectParts(model, partsByName, MPABodyPart.HEAD, headParts);
            // the head's rotation right now is the vanilla look direction, that is what it keeps
            for(ModelPart headPart : headParts){
                ROTATION_HELD.put(headPart, new float[]{headPart.xRot, headPart.yRot, headPart.zRot});
            }
        }

        // the arms of a swimming Drowned are posed by Player Animator alone, EMF's swimming animation does not reach them
        List<ModelPart> followed = SwimmingRules.armsIgnoreBody(mob) ? List.of() : setFollowTargets(animatedParts, partsByName);

        // every part of a driven group and everything attached to it, except branches that belong to another group
        Set<ModelPart> otherGroupRoots = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ModelPart> groupRoots = new ArrayList<>();
        for(MPABodyPart group : GROUPS){
            List<ModelPart> found = new ArrayList<>();
            collectParts(model, partsByName, group, found);
            if(animatedParts.contains(group)){
                groupRoots.addAll(found);
            } else {
                otherGroupRoots.addAll(found);
            }
        }
        Set<ModelPart> partsToPause = Collections.newSetFromMap(new IdentityHashMap<>());
        for(ModelPart groupRoot : groupRoots){
            addOwnedTree(groupRoot, otherGroupRoots, partsToPause);
        }
        return new PausePlan(partsToPause, followed, rawParts);
    }

    private static void pauseParts(LivingEntity mob, Set<ModelPart> partsToPause) {
        partsToPause.removeIf(Objects::isNull);
        if(!partsToPause.isEmpty()){
            TransitionBlend.markPaused(mob, partsToPause);
            EMFAnimationApi.pauseCustomAnimationsForThesePartsOfEntity(EMFAnimationApi.emfEntityOf(mob), partsToPause.toArray(ModelPart[]::new));
        }
    }

    public static void bakeFollowTransforms(LivingEntity mob) {
        if(!isHandled(mob)){
            return;
        }
        List<ModelPart> followed = FOLLOWED_PARTS.get(mob.getUUID());
        if(followed == null || followed.isEmpty()){
            return;
        }
        List<SavedPartPose> saved = new ArrayList<>(followed.size());
        for(ModelPart part : followed){
            ModelPartFollowAccess bridge = (ModelPartFollowAccess) (Object) part;
            ModelPart target = bridge.mobplayeranimator$getFollowTarget();
            if(target == null){
                continue;
            }
            saved.add(SavedPartPose.of(part));
            Quaternionf rotation = new Quaternionf().rotationZYX(target.zRot, target.yRot, target.xRot);
            Vector3f pivot = rotation.transform(new Vector3f(part.x * target.xScale, part.y * target.yScale, part.z * target.zScale));
            part.x = target.x + pivot.x;
            part.y = target.y + pivot.y;
            part.z = target.z + pivot.z;
            rotation.rotateZYX(part.zRot, part.yRot, part.xRot);
            Vector3f euler = eulerAnglesZYX(rotation, new Vector3f());
            part.xRot = euler.x;
            part.yRot = euler.y;
            part.zRot = euler.z;
            part.xScale *= target.xScale;
            part.yScale *= target.yScale;
            part.zScale *= target.zScale;
            // when this part goes back to EMF it has to blend from this pose, the one it was drawn in
            TransitionBlend.recordRenderedPose(mob, part, TransitionBlend.Pose.of(part));
            // the pose includes the target, so rendering must not apply it a second time
            bridge.mobplayeranimator$setFollowTarget(null);
        }
        if(!saved.isEmpty()){
            BAKED_POSES.put(mob.getUUID(), saved);
        }
    }

    public static void resumeEMFAnimationsFor(LivingEntity mob, EntityModel<?> model) {
        restoreAggression(mob);
        if(!isHandled(mob)){
            return;
        }
        TransitionBlend.endRender();
        ROTATION_HELD.clear();
        activeRender = null;
        UUID uuid = mob.getUUID();

        List<SavedPartPose> baked = BAKED_POSES.remove(uuid);
        if(baked != null){
            for(SavedPartPose pose : baked){
                pose.restore();
            }
        }
        revertFollowTargets(mob);

        if(EMF_ANIMATIONS_HALTED.remove(uuid)){
            EMFAnimationApi.resumeAllCustomAnimationsForEntity(EMFAnimationApi.emfEntityOf(mob));
        }
        if(EMF_VANILLA_MODELS_FORCED.remove(uuid)){
            EMFAnimationApi.unlockEntityToVanillaModel(EMFAnimationApi.emfEntityOf(mob));
        }
        // if a model had its parts modified, return all of them to their pre-modification poses
        MPAModelPose modelPose = EMF_MODEL_POSES.remove(uuid);
        if(modelPose != null && model instanceof IEMFModel emfModel){
            modelPose.pose(emfModel.emf$getEMFRootModel());
        }
    }

    static EnumSet<MPABodyPart> claimedParts(Set<MPABodyPart> animatedParts) {
        EnumSet<MPABodyPart> claimed = EnumSet.noneOf(MPABodyPart.class);
        for(MPABodyPart part : animatedParts){
            switch(part){
                case HEAD, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG -> claimed.add(part);
                case TORSO, BODY -> claimed.add(MPABodyPart.TORSO);
                default -> {
                    // held items are not part of the model
                }
            }
        }
        return claimed;
    }

    private static void collectParts(EntityModel<?> model, Map<String, ModelPart> partsByName, MPABodyPart group, List<ModelPart> out) {
        switch(group){
            case HEAD -> {
                addByName(partsByName, "head", out);
                addByName(partsByName, "hat", out);
            }
            case TORSO -> {
                addByName(partsByName, "body", out);
                addByName(partsByName, "jacket", out);
            }
            case RIGHT_ARM -> {
                addByName(partsByName, "right_arm", out);
                addByName(partsByName, "right_sleeve", out);
            }
            case LEFT_ARM -> {
                addByName(partsByName, "left_arm", out);
                addByName(partsByName, "left_sleeve", out);
            }
            case RIGHT_LEG -> {
                addByName(partsByName, "right_leg", out);
                addByName(partsByName, "right_pants", out);
            }
            case LEFT_LEG -> {
                addByName(partsByName, "left_leg", out);
                addByName(partsByName, "left_pants", out);
            }
            default -> {
            }
        }
        collectModelFields(model, group, out);
    }

    private static void collectModelFields(EntityModel<?> model, MPABodyPart group, List<ModelPart> out) {
        if(model instanceof PlayerModel<?> playerModel){
            switch(group){
                case HEAD -> { add(out, playerModel.head); add(out, playerModel.hat); }
                case TORSO -> { add(out, playerModel.body); add(out, playerModel.jacket); }
                case LEFT_ARM -> { add(out, playerModel.leftArm); add(out, playerModel.leftSleeve); }
                case RIGHT_ARM -> { add(out, playerModel.rightArm); add(out, playerModel.rightSleeve); }
                case LEFT_LEG -> { add(out, playerModel.leftLeg); add(out, playerModel.leftPants); }
                case RIGHT_LEG -> { add(out, playerModel.rightLeg); add(out, playerModel.rightPants); }
                default -> {
                }
            }
        } else if(model instanceof HumanoidModelAccess access){
            switch(group){
                case HEAD -> { add(out, access.mobplayeranimator$getHead()); add(out, access.mobplayeranimator$getHat()); }
                case TORSO -> add(out, access.mobplayeranimator$getBody());
                case LEFT_ARM -> {
                    add(out, access.mobplayeranimator$getLeftArm());
                    if(access instanceof IllagerModelAccess illager){
                        add(out, illager.mobplayeranimator$getArms());
                    }
                }
                case RIGHT_ARM -> {
                    add(out, access.mobplayeranimator$getRightArm());
                    if(access instanceof IllagerModelAccess illager){
                        add(out, illager.mobplayeranimator$getArms());
                    }
                }
                case LEFT_LEG -> add(out, access.mobplayeranimator$getLeftLeg());
                case RIGHT_LEG -> add(out, access.mobplayeranimator$getRightLeg());
                default -> {
                }
            }
        } else if(model instanceof HumanoidModel<?> humanoidModel){
            switch(group){
                case HEAD -> { add(out, humanoidModel.head); add(out, humanoidModel.hat); }
                case TORSO -> add(out, humanoidModel.body);
                case LEFT_ARM -> add(out, humanoidModel.leftArm);
                case RIGHT_ARM -> add(out, humanoidModel.rightArm);
                case LEFT_LEG -> add(out, humanoidModel.leftLeg);
                case RIGHT_LEG -> add(out, humanoidModel.rightLeg);
                default -> {
                }
            }
        }
    }

    private static void addByName(Map<String, ModelPart> partsByName, String name, List<ModelPart> out) {
        add(out, partsByName.get(name));
    }

    private static void add(List<ModelPart> out, ModelPart part) {
        if(part == null){
            return;
        }
        for(ModelPart existing : out){
            if(existing == part){
                return;
            }
        }
        out.add(part);
    }

    private static Map<String, ModelPart> childrenOf(ModelPart part) {
        return ((ModelPartAccessor) (Object) part).mobplayeranimator$getChildren();
    }

    private static void addOwnedTree(ModelPart part, Set<ModelPart> otherGroupRoots, Set<ModelPart> out) {
        if(!out.add(part)){
            return;
        }
        for(ModelPart child : childrenOf(part).values()){
            if(!otherGroupRoots.contains(child)){
                addOwnedTree(child, otherGroupRoots, out);
            }
        }
    }

    private static boolean isDescendant(ModelPart parent, ModelPart child) {
        if(parent == child){
            return true;
        }
        for(ModelPart part : childrenOf(parent).values()){
            if(isDescendant(part, child)){
                return true;
            }
        }
        return false;
    }

    private static List<ModelPart> setFollowTargets(Set<MPABodyPart> animatedParts, Map<String, ModelPart> partsByName) {
        boolean armsOrHeadActive = animatedParts.contains(MPABodyPart.LEFT_ARM)
                || animatedParts.contains(MPABodyPart.RIGHT_ARM)
                || animatedParts.contains(MPABodyPart.HEAD);
        boolean torsoFreeForEMF = !animatedParts.contains(MPABodyPart.TORSO);
        if(!armsOrHeadActive || !torsoFreeForEMF){
            return List.of();
        }
        ModelPart body = partsByName.get("body");
        if(body == null){
            return List.of();
        }
        List<ModelPart> followed = new ArrayList<>();
        for(MPABodyPart group : animatedParts){
            if(group != MPABodyPart.LEFT_ARM && group != MPABodyPart.RIGHT_ARM && group != MPABodyPart.HEAD){
                continue;
            }
            List<ModelPart> emfParts = new ArrayList<>();
            switch(group){
                case HEAD -> { addByName(partsByName, "head", emfParts); addByName(partsByName, "hat", emfParts); }
                case RIGHT_ARM -> { addByName(partsByName, "right_arm", emfParts); addByName(partsByName, "right_sleeve", emfParts); }
                case LEFT_ARM -> { addByName(partsByName, "left_arm", emfParts); addByName(partsByName, "left_sleeve", emfParts); }
                default -> {
                }
            }
            for(ModelPart candidate : emfParts){
                // parts already attached to the body follow it anyway
                if(isDescendant(body, candidate)){
                    continue;
                }
                boolean alreadyCovered = false;
                for(ModelPart ancestor : followed){
                    if(isDescendant(ancestor, candidate)){
                        alreadyCovered = true;
                        break;
                    }
                }
                if(!alreadyCovered){
                    ((ModelPartFollowAccess) (Object) candidate).mobplayeranimator$setFollowTarget(body);
                    followed.add(candidate);
                }
            }
        }
        return followed;
    }

    private static void revertFollowTargets(LivingEntity mob) {
        List<ModelPart> followed = FOLLOWED_PARTS.remove(mob.getUUID());
        if(followed != null){
            for(ModelPart part : followed){
                ((ModelPartFollowAccess) (Object) part).mobplayeranimator$setFollowTarget(null);
            }
        }
    }

    private static Vector3f eulerAnglesZYX(Quaternionf q, Vector3f dest) {
        dest.x = (float) Math.atan2(2.0 * (q.w * q.x + q.y * q.z), 1.0 - 2.0 * (q.x * q.x + q.y * q.y));
        dest.y = (float) Math.asin(Math.max(-1.0, Math.min(1.0, 2.0 * (q.w * q.y - q.z * q.x))));
        dest.z = (float) Math.atan2(2.0 * (q.w * q.z + q.x * q.y), 1.0 - 2.0 * (q.y * q.y + q.z * q.z));
        return dest;
    }

    private record RenderContext(LivingEntity entity, ModelPart root) {
    }

    private record LayerRender(EntityModel<?> model, RenderContext previous, List<ModelPart> followed) {
    }

    private record SavedPartPose(ModelPart part, float x, float y, float z, float xRot, float yRot, float zRot,
                                 float xScale, float yScale, float zScale) {
        static SavedPartPose of(ModelPart part) {
            return new SavedPartPose(part, part.x, part.y, part.z, part.xRot, part.yRot, part.zRot,
                    part.xScale, part.yScale, part.zScale);
        }

        void restore() {
            this.part.x = this.x;
            this.part.y = this.y;
            this.part.z = this.z;
            this.part.xRot = this.xRot;
            this.part.yRot = this.yRot;
            this.part.zRot = this.zRot;
            this.part.xScale = this.xScale;
            this.part.yScale = this.yScale;
            this.part.zScale = this.zScale;
        }
    }
}
