package fr.dynamx.common.network.packets;

import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.utils.DynamXUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;

public class MessageSyncBlockCustomization implements IDnxPacket {

    private BlockPos blockPos;
    private Vector3f relativeTranslation, relativeScale, relativeRotation;

    public MessageSyncBlockCustomization() {
    }

    public MessageSyncBlockCustomization(BlockPos blockPos, Vector3f relativeTranslation, Vector3f relativeScale, Vector3f relativeRotation) {
        this.blockPos = blockPos;
        this.relativeTranslation = relativeTranslation;
        this.relativeScale = relativeScale;
        this.relativeRotation = relativeRotation;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        DynamXUtils.writeBlockPos(buf, blockPos);
        DynamXUtils.writeVector3f(buf, relativeTranslation);
        DynamXUtils.writeVector3f(buf, relativeScale);
        DynamXUtils.writeVector3f(buf, relativeRotation);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        blockPos = DynamXUtils.readBlockPos(buf);
        relativeTranslation = DynamXUtils.readVector3f(buf);
        relativeScale = DynamXUtils.readVector3f(buf);
        relativeRotation = DynamXUtils.readVector3f(buf);
    }

    public static void handle(MessageSyncBlockCustomization message /*, IPayloadContext ctx */) {
        // TODO port:1.20.1 - TEDynamXBlock not yet ported (Phase block-entities). Re-port using
        // level().getBlockEntity, BlockState markCollisionsDirty equivalent, setBlock invalidation,
        // ServerPlayer#hasPermissions(4) for permission check.
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
