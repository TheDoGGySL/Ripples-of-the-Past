package com.github.standobyte.jojo.network.packets.fromserver;

import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.network.NetworkUtil;
import com.github.standobyte.jojo.network.packets.IModPacketHandler;
import com.github.standobyte.jojo.util.mod.IPlayerPossess;

import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.GameType;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.fml.network.NetworkEvent;

public class TrPossessEntityPacket {
    private final int entityId;
    private final int hostEntityId;
    private final boolean asAlive;
    private final Optional<GameType> prevGameMode;

    public TrPossessEntityPacket(int entityId, int hostEntityId, boolean asAlive, @Nonnull Optional<GameType> prevGameMode) {
        this.entityId = entityId;
        this.hostEntityId = hostEntityId;
        this.asAlive = asAlive;
        this.prevGameMode = prevGameMode;
    }
    
    
    
    public static class Handler implements IModPacketHandler<TrPossessEntityPacket> {

        @Override
        public void encode(TrPossessEntityPacket msg, PacketBuffer buf) {
            buf.writeInt(msg.entityId);
            buf.writeInt(msg.hostEntityId);
            buf.writeBoolean(msg.asAlive);
            NetworkUtil.writeOptional(buf, msg.prevGameMode, buf::writeEnum);
        }

        @Override
        public TrPossessEntityPacket decode(PacketBuffer buf) {
            return new TrPossessEntityPacket(buf.readInt(), buf.readInt(), buf.readBoolean(), 
                    NetworkUtil.readOptional(buf, buffer -> buffer.readEnum(GameType.class)));
        }

        @Override
        public void handle(TrPossessEntityPacket msg, Supplier<NetworkEvent.Context> ctx) {
            Entity entity = ClientUtil.getEntityById(msg.entityId);
            if (entity instanceof IPlayerPossess) {
                Entity hostEntity = ClientUtil.getEntityById(msg.hostEntityId);
                IPlayerPossess player = (IPlayerPossess) entity;
                player.jojoPossessEntity(hostEntity, msg.asAlive);
                player.jojoSetPrePossessGameMode(msg.prevGameMode);
                ForgeIngameGui.renderSpectatorTooltip = hostEntity == null;
            }
        }

        @Override
        public Class<TrPossessEntityPacket> getPacketClass() {
            return TrPossessEntityPacket.class;
        }
    }

}
