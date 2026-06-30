package com.cmdpro.databank.worldgui;

import com.cmdpro.databank.Databank;
import com.cmdpro.databank.networking.ModMessages;
import com.cmdpro.databank.networking.packet.WorldGuiInteractC2SPacket;
import com.cmdpro.databank.worldgui.components.WorldGuiComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = Databank.MOD_ID)
public class WorldGuiClientEvents {
    @SubscribeEvent
    public static void interactionKeyMappingTriggeredEvent(InputEvent.InteractionKeyMappingTriggered event) {
        HitResult hitResult = Minecraft.getInstance().hitResult;
        if (hitResult instanceof WorldGuiHitResult result) {
            if (result.getEntity() instanceof WorldGuiEntity entity) {
                var gui = entity.getGuiData();
                if (gui != null) {
                    var guiType = gui.getType();
                    Vec2 normal = result.result.normal;
                    int x = (int) (normal.x * guiType.getRenderSize().x);
                    int y = (int) (normal.y * guiType.getRenderSize().y);
                    if (event.getKeyMapping().equals(Minecraft.getInstance().options.keyAttack)) {
                        gui.leftClick(true, Minecraft.getInstance().player, x, y);
                        for (WorldGuiComponent i : gui.components.stream().toList()) {
                            if (gui.tryLeftClickComponent(true, Minecraft.getInstance().player, i, x, y)) {
                                i.leftClick(true, Minecraft.getInstance().player, x, y);
                            }
                        }
                        ModMessages.sendToServer(new WorldGuiInteractC2SPacket(entity.getId(), 0, x, y));
                        event.setCanceled(true);
                    } else if (event.getKeyMapping().equals(Minecraft.getInstance().options.keyUse)) {
                        gui.rightClick(true, Minecraft.getInstance().player, x, y);
                        for (WorldGuiComponent i : gui.components.stream().toList()) {
                            if (gui.tryLeftClickComponent(true, Minecraft.getInstance().player, i, x, y)) {
                                i.rightClick(true, Minecraft.getInstance().player, x, y);
                            }
                        }
                        ModMessages.sendToServer(new WorldGuiInteractC2SPacket(entity.getId(), 1, x, y));
                        event.setCanceled(true);
                    }
                }
            }
        }
    }
}