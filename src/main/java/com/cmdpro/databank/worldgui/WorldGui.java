package com.cmdpro.databank.worldgui;

import com.cmdpro.databank.DatabankRegistries;
import com.cmdpro.databank.worldgui.components.WorldGuiComponent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public abstract class WorldGui {
    public static final Codec<WorldGui> CODEC = DatabankRegistries.WORLD_GUI_TYPE_REGISTRY.byNameCodec().dispatch(
        "id",
        WorldGui::getType,
        WorldGuiType::codec
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, WorldGui> STREAM_CODEC = ByteBufCodecs.registry(
        DatabankRegistries.WORLD_GUI_TYPE_REGISTRY.key()).dispatch(WorldGui::getType, WorldGuiType::streamCodec);

    public static final RecordCodecBuilder<WorldGui, List<WorldGuiComponent>> COMPONENTS_CODEC_BUILDER = WorldGuiComponent.CODEC
        .listOf()
        .fieldOf("components")
        .forGetter((gui) -> gui.components);

    public List<WorldGuiComponent> components;

    public WorldGui() {
        this(new ArrayList<>());
    }

    public WorldGui(List<WorldGuiComponent> components) {
        this.components = components;
    }

    public WorldGui addComponent(WorldGuiComponent component) {
        if (!components.contains(component)) {
            components.add(component);
        }
        return this;
    }

    public WorldGui removeComponent(WorldGuiComponent component) {
        components.remove(component);
        return this;
    }

    public WorldGui removeComponents(Predicate<WorldGuiComponent> predicate) {
        components.removeAll(components.stream().filter(predicate).toList());
        return this;
    }

    public void renderComponents(GuiGraphicsExtractor guiGraphics) {
        for (WorldGuiComponent i : components) {
            i.render(this, guiGraphics);
        }
    }

    public boolean tryLeftClickComponent(boolean isClient, Player player, WorldGuiComponent component, int x, int y) {
        return true;
    }

    public boolean tryRightClickComponent(boolean isClient, Player player, WorldGuiComponent component, int x, int y) {
        return true;
    }

    public abstract WorldGuiType getType();

    public void renderGui(GuiGraphicsExtractor guiGraphics) {
        renderComponents(guiGraphics);
    }

    public void leftClick(boolean isClient, Player player, int x, int y) {
    }

    public void rightClick(boolean isClient, Player player, int x, int y) {
    }

    public void tick() {
    }

    public List<Matrix3f> getMatrixs() {
        return new ArrayList<>();
    }

    public void addMatrixsForFacingPlayer(WorldGuiEntity entity, List<Matrix3f> matrixs, boolean horizontal, boolean vertical) {
        if (!entity.level().isClientSide()) {
            return;
        }

        Vec2 angle = ClientHandler.angleToClient(entity, this);
        matrixs.add(new Matrix3f()
            .rotateX((float) Math.toRadians(-90))
        );
        if (horizontal) {
            matrixs.add(new Matrix3f()
                .rotateZ((float) Math.toRadians(-angle.y + 180))
            );
        }
        if (vertical) {
            matrixs.add(new Matrix3f()
                .rotateX((float) Math.toRadians(-angle.x))
            );
        }
    }

    public boolean isPosInBounds(int x, int y, int minX, int minY, int maxX, int maxY) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    public int normalXIntoGuiX(double normalX) {
        return (int) (normalX * getType().getRenderSize().x);
    }

    public int normalYIntoGuiY(double normalY) {
        return (int) (normalY * getType().getRenderSize().y);
    }

    public Vec2 getClientTargetNormal() {
        WorldGuiEntity entity = ClientHandler.getClientTargetGui();
        if (entity != null && entity.getGuiData() == this) {
            return ClientHandler.getClientTargetNormal();
        }
        return null;
    }

    public static Vec2 getClientTargetNormalGlobal() {
        return ClientHandler.getClientTargetNormal();
    }

    private static class ClientHandler {
        public static Vec2 getClientTargetNormal() {
            HitResult hitResult = Minecraft.getInstance().hitResult;
            if (hitResult instanceof WorldGuiHitResult result) {
                return result.result.normal;
            }
            return null;
        }

        public static WorldGuiEntity getClientTargetGui() {
            HitResult hitResult = Minecraft.getInstance().hitResult;
            if (hitResult instanceof WorldGuiHitResult result) {
                if (result.getEntity() instanceof WorldGuiEntity entity) {
                    return entity;
                }
            }
            return null;
        }

        public static Vec2 angleToClient(WorldGuiEntity entity, WorldGui gui) {
            Vec3 pointA = entity.position();
            Vec3 pointB = Minecraft.getInstance().player.getEyePosition();
            return calculateRotationVector(pointA, pointB);
        }

        private static Vec2 calculateRotationVector(Vec3 pVec, Vec3 pTarget) {
            double d0 = pTarget.x() - pVec.x();
            double d1 = pTarget.y() - pVec.y();
            double d2 = pTarget.z() - pVec.z();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            return new Vec2(
                Mth.wrapDegrees((float) (-(Mth.atan2(d1, d3) * (double) (180F / (float) Math.PI)))),
                Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * (double) (180F / (float) Math.PI)) - 90.0F)
            );
        }
    }
}
