package com.cmdpro.databank.model;

import com.cmdpro.databank.model.animation.DatabankAnimationState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public abstract class BaseDatabankModel<T> {
    private static final Vector3f VECTOR_CACHE = new Vector3f();

    public void submitPartAndChildren(T obj, PoseStack pPoseStack, SubmitNodeCollector submitNodeCollector, int pPackedLight, int pPackedOverlay, int pColor, ModelPose.ModelPosePart part, Vec3 normalMult) {
        submitPartAndChildren(
            obj,
            pPoseStack,
            submitNodeCollector,
            pPackedLight,
            pPackedOverlay,
            pColor,
            part,
            normalMult,
            new Quaternionf()
        );
    }

    private void submitPartAndChildren(T obj, PoseStack pPoseStack, SubmitNodeCollector submitNodeCollector, int pPackedLight, int pPackedOverlay, int pColor, ModelPose.ModelPosePart part, Vec3 normalMult, Quaternionf quaternionf) {
        pPoseStack.pushPose();
        if (part.part.data instanceof DatabankPartData.DatabankGroupPart) {
            pPoseStack.translate((part.pos.x() / 16f), (part.pos.y() / 16f), (part.pos.z() / 16f));
            pPoseStack.mulPose(new Quaternionf().rotationZYX(
                part.rotation.z(),
                -part.rotation.y(),
                -part.rotation.x()
            ));
            pPoseStack.scale(part.scale.x(), part.scale.y(), part.scale.z());
            quaternionf.rotateZYX(part.rotation.z(), -part.rotation.y(), -part.rotation.x());
        }
        if (part.part.data instanceof DatabankPartData.DatabankCubePart ||
            part.part.data instanceof DatabankPartData.DatabankMeshPart) {
            submitNodeCollector.submitCustomGeometry(
                pPoseStack,
                getRenderType(obj, part),
                (pose, buffer) -> part.render(
                    getModel(),
                    pose,
                    buffer,
                    getPackedLight(obj, part, pPackedLight),
                    getPackedOverlay(obj, part, pPackedOverlay),
                    getColor(obj, part, pColor),
                    normalMult,
                    isShadedByNormal(obj, part),
                    new Quaternionf(quaternionf)
                )
            );

        }
        for (ModelPose.ModelPosePart i : part.children) {
            submitPartAndChildren(
                obj,
                pPoseStack,
                submitNodeCollector,
                pPackedLight,
                pPackedOverlay,
                pColor,
                i,
                normalMult,
                new Quaternionf(quaternionf)
            );
        }
        pPoseStack.popPose();
    }

    public boolean isShadedByNormal(T obj, ModelPose.ModelPosePart part) {
        Vector3fc dimensions = part.part.data.getDimensions();
        float inflate = part.part.data.getInflate();
        if (dimensions.x() + inflate <= 0.001 && dimensions.x() + inflate >= -0.001) {
            return false;
        }
        if (dimensions.y() + inflate <= 0.001 && dimensions.y() + inflate >= -0.001) {
            return false;
        }
        if (dimensions.z() + inflate <= 0.001 && dimensions.z() + inflate >= -0.001) {
            return false;
        }
        return true;
    }

    public int getColor(T obj, ModelPose.ModelPosePart part, int original) {
        return original;
    }

    public int getPackedLight(T obj, ModelPose.ModelPosePart part, int original) {
        return original;
    }

    public int getPackedOverlay(T obj, ModelPose.ModelPosePart part, int original) {
        return original;
    }

    public RenderType getRenderType(T obj, ModelPose.ModelPosePart part) {
        return getRenderType(obj);
    }

    public RenderType getRenderType(T obj) {
        return RenderTypes.entityCutout(getTextureLocation());
    }

    public abstract Identifier getTextureLocation();

    public abstract void setupModelPose(T obj);

    public abstract DatabankModel getModel();

    protected ModelPose modelPose;

    protected void animate(DatabankAnimationState state) {
        ModelPose pose = getModel().createModelPose();
        state.update();
        state.getAnim().animation.animationParts.forEach(i -> {
            HashMap<DatabankAnimation.AnimationKeyframe, Keyframe> keyframes = new HashMap<>();
            for (DatabankAnimation.AnimationKeyframe j : i.keyframes) {
                keyframes.put(j, j.createKeyframe());
            }
            Keyframe[] keyframeArray = keyframes.values().stream().sorted(Comparator.comparingDouble(Keyframe::timestamp)).toList().toArray(
                new Keyframe[0]);
            List<DatabankAnimation.AnimationKeyframe> databankKeyframes = i.keyframes.stream().sorted(Comparator.comparingDouble(
                (animFrame) -> animFrame.timestamp)).toList();
            DatabankAnimation.AnimationKeyframe current = null;
            for (DatabankAnimation.AnimationKeyframe j : databankKeyframes) {
                if (j.timestamp <= state.getProgress()) {
                    current = j;
                }
            }
            boolean forceNextToCurrent = false;
            if (current == null) {
                if (!databankKeyframes.isEmpty()) {
                    current = databankKeyframes.getFirst();
                    forceNextToCurrent = true;
                }
            }
            if (current != null) {
                int currentIndex = databankKeyframes.indexOf(current);
                int nextIndex = forceNextToCurrent ? currentIndex : keyframes.size() > currentIndex + 1 ? currentIndex +
                    1 : currentIndex;
                DatabankAnimation.AnimationKeyframe next = forceNextToCurrent ? current : databankKeyframes.get(
                    nextIndex);
                Keyframe keyframe = keyframes.get(current);
                Keyframe nextKeyframe = keyframes.get(next);
                AnimationChannel.Interpolation interpolation = keyframe.interpolation();
                float delta = 0f;
                if (currentIndex != nextIndex) {
                    delta = (float) ((state.getProgress() - current.timestamp) / (next.timestamp - current.timestamp));
                }
                interpolation.apply(VECTOR_CACHE, delta, keyframeArray, currentIndex, nextIndex, 1.0f);
                current.targetChannel.apply(pose.stringToPart.get(i.bone), VECTOR_CACHE);
            }
        });
        modelPose = pose;
    }
}
