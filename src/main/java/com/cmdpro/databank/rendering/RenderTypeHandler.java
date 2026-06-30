package com.cmdpro.databank.rendering;

import com.cmdpro.databank.Databank;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class RenderTypeHandler {
    public static final BlendFunction ADDITIVE_TRANSPARENCY = BlendFunction.LIGHTNING;
    public static final BlendFunction TRANSPARENCY = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
    public static List<RenderType> renderTypes = new ArrayList<>();
    public static List<RenderType> normalRenderTypes = new ArrayList<>();
    public static List<RenderType> particleRenderTypes = new ArrayList<>();

    public static final RenderPipeline TRANSLUCENT_PIPELINE = RenderPipeline
        .builder(RenderPipelines.PARTICLE_SNIPPET)
        .withLocation(Databank.locate("translucent"))
        .withVertexShader(Databank.locate("translucent"))
        .withFragmentShader(Databank.locate("translucent"))
        .withColorTargetState(new ColorTargetState(TRANSPARENCY))
        .withCull(false)
        .build();

    public static final RenderPipeline ADDITIVE_PIPELINE = RenderPipeline
        .builder(RenderPipelines.PARTICLE_SNIPPET)
        .withLocation(Databank.locate("additive"))
        .withVertexShader(Databank.locate("additive"))
        .withFragmentShader(Databank.locate("additive"))
        .withColorTargetState(new ColorTargetState(ADDITIVE_TRANSPARENCY))
        .withCull(false)
        .build();

    public static final RenderPipeline SCREEN_PROJECTION_PIPELINE = RenderPipeline
        .builder(RenderPipelines.MATRICES_FOG_SNIPPET)
        .withLocation(Databank.locate("screen_projection"))
        .withVertexShader(Databank.locate("screen_projection"))
        .withFragmentShader(Databank.locate("screen_projection"))
        .withSampler("ProjectedTarget")
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
        .build();

    public static final BiFunction<Identifier, Boolean, RenderType> TRANSPARENT = Util.memoize(
            (texture, outline) -> {
                return registerRenderType(RenderType.create(Databank.MOD_ID + ":transparent",
                    RenderSetup.builder(TRANSLUCENT_PIPELINE)
                        .bufferSize(256)
                        .withTexture("Sampler0", texture)
                        .affectsCrumbling()
                        .sortOnUpload()
                        .setOutline(outline ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE)
                        .createRenderSetup()
                ), true);
            });
    public static final BiFunction<Identifier, Boolean, RenderType> ADDITIVE = Util.memoize(
            (texture, outline) -> {
                return registerRenderType(RenderType.create(Databank.MOD_ID + ":additive",
                        RenderSetup.builder(ADDITIVE_PIPELINE)
                            .bufferSize(256)
                            .withTexture("Sampler0", texture)
                            .affectsCrumbling()
                            .sortOnUpload()
                            .setOutline(outline ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE)
                            .createRenderSetup()
                ), false);
            });
    public static final RenderType TRANSPARENT_PARTICLE = registerRenderType(RenderType.create(Databank.MOD_ID + ":transparent",
        RenderSetup.builder(TRANSLUCENT_PIPELINE)
            .bufferSize(256)
            .withTexture("Sampler0", TextureAtlas.LOCATION_PARTICLES)
            .useLightmap()
            .affectsCrumbling()
            .sortOnUpload()
            .createRenderSetup()
    ), false);
    public static final RenderType ADDITIVE_PARTICLE = registerRenderType(RenderType.create(Databank.MOD_ID + ":additive",
        RenderSetup.builder(ADDITIVE_PIPELINE)
            .bufferSize(256)
            .withTexture("Sampler0", TextureAtlas.LOCATION_PARTICLES)
            .useLightmap()
            .affectsCrumbling()
            .sortOnUpload()
            .createRenderSetup()
    ),false);
    public static final RenderType SCREEN_PROJECTION = registerRenderType(
        RenderType.create(
            Databank.MOD_ID + ":screen_projection",
            RenderSetup.builder(SCREEN_PROJECTION_PIPELINE)
                .bufferSize(256)
                .withTexture("ProjectedTarget", RenderProjectionUtil.PROJECTED_TARGET_TEXTURE)
                .createRenderSetup()
        ), false);
    public static RenderType registerRenderType(RenderType type, boolean isParticle) {
        renderTypes.add(type);
        if (isParticle) {
            particleRenderTypes.add(type);
        } else {
            normalRenderTypes.add(type);
        }
        return type;
    }
    public static RenderType transparent(Identifier location, boolean outline) {
        return TRANSPARENT.apply(location, outline);
    }

    public static RenderType transparent(Identifier location) {
        return transparent(location, true);
    }
    public static RenderType additive(Identifier location, boolean outline) {
        return ADDITIVE.apply(location, outline);
    }

    public static RenderType additive(Identifier location) {
        return additive(location, true);
    }
    public static void load() {
        Databank.LOGGER.info("Loaded Databank Render Types");
    }
}
