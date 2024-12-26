package com.github.standobyte.jojo.network.packets.fromserver;

import java.util.function.Supplier;

import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.client.sound.ClientTickingSoundsHelper;
import com.github.standobyte.jojo.network.packets.IModPacketHandler;

import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class PlaySoundAtEntityPacket {
    private final SoundEvent sound;
    private final int entityId;
    private final float volume;
    private final float pitch;

    public PlaySoundAtEntityPacket(SoundEvent sound, int entityId, float volume, float pitch) {
        this.sound = sound;
        this.entityId = entityId;
        this.volume = volume;
        this.pitch = pitch;
    }
    
    
    
    public static class Handler implements IModPacketHandler<PlaySoundAtEntityPacket> {

        @Override
        public void encode(PlaySoundAtEntityPacket msg, PacketBuffer buf) {
            buf.writeRegistryIdUnsafe(ForgeRegistries.SOUND_EVENTS, msg.sound);
            buf.writeInt(msg.entityId);
            buf.writeFloat(msg.volume);
            buf.writeFloat(msg.pitch);
        }

        @Override
        public PlaySoundAtEntityPacket decode(PacketBuffer buf) {
            return new PlaySoundAtEntityPacket(buf.readRegistryIdUnsafe(ForgeRegistries.SOUND_EVENTS), buf.readInt(), buf.readFloat(), buf.readFloat());
        }

        @Override
        public void handle(PlaySoundAtEntityPacket msg, Supplier<NetworkEvent.Context> ctx) {
            Entity entity = ClientUtil.getEntityById(msg.entityId);
            ClientTickingSoundsHelper.playEntitySound(entity, msg.sound, msg.volume, msg.pitch);
        }

        @Override
        public Class<PlaySoundAtEntityPacket> getPacketClass() {
            return PlaySoundAtEntityPacket.class;
        }
    }

}
