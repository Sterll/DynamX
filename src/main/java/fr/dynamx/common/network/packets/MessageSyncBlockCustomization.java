package fr.dynamx.common.network.packets;

import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.utils.DynamXUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.LogicalSide;

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

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        if (side != LogicalSide.SERVER || !(context instanceof ServerPlayer player) || context.level() == null) {
            return;
        }
        Level level = player.level();
        BlockEntity be = level.getBlockEntity(blockPos);
        if (!(be instanceof TEDynamXBlock te)) {
            return;
        }
        if (!player.hasPermissions(4)) {
            player.sendSystemMessage(Component.literal("You're not allowed to do this"));
            return;
        }
        te.setRelativeTranslation(relativeTranslation);
        te.setRelativeScale(relativeScale);
        te.setRelativeRotation(relativeRotation);
        te.setChanged();
        te.markCollisionsDirty(true);
        BlockState state = level.getBlockState(blockPos);
        level.sendBlockUpdated(blockPos, state, state, 4);
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
