package com.cmdpro.databank.impact;

import com.cmdpro.databank.Databank;
import com.cmdpro.databank.config.DatabankClientConfig;
import com.cmdpro.databank.shaders.PostShaderInstance;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.Identifier;

import java.util.Map;

public class ImpactShader extends PostShaderInstance {
    public static final Identifier IMPACT_TARGET = Databank.locate("impact");
    public static final Identifier FROZEN_IMPACT_TARGET = Databank.locate("frozen_impact");

    private static final UniformBufferInfo ALPHA_UNIFORM_INFO = new UniformBufferInfo(
        (instance, builder) -> {
            float progress = 0;

            if (ImpactFrameHandler.impactFrame != null) {
                float maxProgress = (float) ImpactFrameHandler.impactFrame.startTicks / 20f;
                progress = instance.getTime() / maxProgress;
            }

            builder.putFloat(ImpactFrameHandler.impactFrame != null ? ImpactFrameHandler.impactFrame.alpha.getValue(progress) : 0f);
        },
        new Std140SizeCalculator().putFloat()
    );

    public ImpactShader() {
        super(Map.of("alpha", ALPHA_UNIFORM_INFO), IMPACT_TARGET, FROZEN_IMPACT_TARGET);
    }

    @Override
    public Identifier getShaderLocation() {
        return Databank.locate("impact");
    }

    @Override
    public void setupFrameGraph(FrameGraphBuilder frameGraphBuilder) {
        super.setupFrameGraph(frameGraphBuilder);

        targets.put(IMPACT_TARGET, frameGraphBuilder.importExternal("databank_impact", ImpactFrameHandler.getImpactTarget()));
        targets.put(FROZEN_IMPACT_TARGET, frameGraphBuilder.importExternal("databank_frozen_impact", ImpactFrameHandler.getImpactTarget()));
    }

    @Override
    public void beforeProcess() {
        super.beforeProcess();
        if (ImpactFrameHandler.impactFrame == null) {
            setActive(false);
        }
        if (!DatabankClientConfig.allowImpactVisuals) {
            setActive(false);
        }
    }
}
