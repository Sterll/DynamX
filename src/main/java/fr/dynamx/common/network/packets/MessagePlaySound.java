package fr.dynamx.common.network.packets;

import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.utils.DynamXUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;

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

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        if (side != LogicalSide.CLIENT) {
            return;
        }
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> playClient(pos, volume, pitch));
    }

    private static void playClient(Vector3f pos, float volume, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        mc.level.playLocalSound(pos.x, pos.y, pos.z, SoundEvents.ANVIL_LAND, SoundSource.AMBIENT, volume, pitch, true);
        mc.level.addParticle(ParticleTypes.SMOKE, pos.x, pos.y + 0.2D, pos.z, 0, 0, 0);
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
