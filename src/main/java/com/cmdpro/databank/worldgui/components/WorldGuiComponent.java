package com.cmdpro.databank.worldgui.components;

import com.cmdpro.databank.DatabankRegistries;
import com.cmdpro.databank.worldgui.WorldGui;
import com.cmdpro.databank.worldgui.WorldGuiEntity;
import com.cmdpro.databank.worldgui.WorldGuiHitResult;
import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;

public abstract class WorldGuiComponent {
    public static final Codec<WorldGuiComponent> CODEC = DatabankRegistries.WORLD_GUI_COMPONENT_REGISTRY.byNameCodec().dispatch(
        WorldGuiComponent::getType,
        WorldGuiComponentType::codec
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, WorldGuiComponent> STREAM_CODEC = ByteBufCodecs.registry(
        DatabankRegistries.WORLD_GUI_COMPONENT_REGISTRY_KEY).dispatch(
        WorldGuiComponent::getType,
        WorldGuiComponentType::streamCodec
    );

    public abstract void render(WorldGui gui, GuiGraphicsExtractor guiGraphics);

    public void leftClick(boolean isClient, Player player, int x, int y) {
    }

    public void rightClick(boolean isClient, Player player, int x, int y) {
    }

    public abstract WorldGuiComponentType getType();

    public int getDrawPriority() {
        return 0;
    }

    public boolean isPosInBounds(int x, int y, int minX, int minY, int maxX, int maxY) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    public int normalXIntoGuiX(WorldGui gui, double normalX) {
        return (int) (normalX * gui.getType().getRenderSize().x);
    }

    public int normalYIntoGuiY(WorldGui gui, double normalY) {
        return (int) (normalY * gui.getType().getRenderSize().y);
    }

    public Vec2 getClientTargetNormal(WorldGui gui) {
        WorldGuiEntity entity = ClientHandler.getClientTargetGui();
        if (entity != null && entity.getGuiData() == gui) {
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
    }
}
