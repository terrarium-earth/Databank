package com.cmdpro.databank.networking.packet;

import com.cmdpro.databank.ClientDatabankUtils;
import com.cmdpro.databank.Databank;
import com.cmdpro.databank.DatabankRegistries;
import com.cmdpro.databank.hidden.Hidden;
import com.cmdpro.databank.hidden.HiddenManager;
import com.cmdpro.databank.hidden.HiddenSerializer;
import com.cmdpro.databank.hidden.HiddenTypeInstance;
import com.cmdpro.databank.networking.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;

public record HiddenSyncS2CPacket(Map<Identifier, Hidden> hidden) implements Message {

    public static HiddenSyncS2CPacket read(FriendlyByteBuf buf) {
        Map<Identifier, Hidden> blocks = buf.readMap(FriendlyByteBuf::readIdentifier, (buf2) -> HiddenSerializer.STREAM_CODEC.decode((RegistryFriendlyByteBuf)buf2));
        for (Map.Entry<Identifier, Hidden> i : blocks.entrySet()) {
            i.getValue().id = i.getKey();
        }
        return new HiddenSyncS2CPacket(blocks);
    }
    public static void write(FriendlyByteBuf buf, HiddenSyncS2CPacket obj) {
        buf.writeMap(obj.hidden, FriendlyByteBuf::writeIdentifier, (buf2, val) -> HiddenSerializer.STREAM_CODEC.encode((RegistryFriendlyByteBuf)buf2, val));
    }
    public static final Type<HiddenSyncS2CPacket> TYPE = new Type<>(Databank.locate("hidden_block_sync"));
    @Override
    public Type<HiddenSyncS2CPacket> type() {
        return TYPE;
    }

    @Override
    public void handleClient(Minecraft minecraft, Player player, IPayloadContext context) {
        context.enqueueWork(() -> {
            HiddenManager.hidden.clear();
            HiddenManager.hidden.putAll(hidden);
            for (HiddenTypeInstance.HiddenType<?> i : DatabankRegistries.HIDDEN_TYPE_REGISTRY.stream().toList()) {
                i.updateClient();
                i.onRecieveClient();
            }
        });
    }
}