package fr.dynamx.common.network.packets;

import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.utils.DynamXUtils;
import io.netty.buffer.ByteBuf;

public class MessagePlaySound implements IDnxPacket {

    private Vector3f pos;
    private float volume, pitch;

    public MessagePlaySound() {
    }

    public MessagePlaySound(Vector3f pos, float volume, float pitch) {
        this.pos = pos;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        DynamXUtils.writeVector3f(buf, pos);
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = DynamXUtils.readVector3f(buf);
        volume = buf.readFloat();
        pitch = buf.readFloat();
    }

    public static void handle(MessagePlaySound message /*, IPayloadContext ctx */) {
        // TODO port:1.20.1 - Original played SoundEvents.ANVIL_HIT (1.20.1: SoundEvents.ANVIL_LAND?)
        // at message.pos with SoundSource.AMBIENT and spawned a SMOKE particle. Re-port in Phase 5b
        // using Minecraft.getInstance().level.playLocalSound + addParticle.
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
