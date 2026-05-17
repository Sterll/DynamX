package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.entities.modules.movables.PickingObjectHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;

public class MessagePickObject implements IDnxPacket {
    private MovableModule.Action moduleAction;

    public MessagePickObject() {
    }

    public MessagePickObject(MovableModule.Action action) {
        this.moduleAction = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        MovableModule.Action action = new MovableModule.Action();
        action.setEnumAction(MovableModule.EnumAction.values()[buf.readInt()]);
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            MovableModule.EnumAction kind = action.getMovableAction();
            if (kind == MovableModule.EnumAction.TAKE || kind == MovableModule.EnumAction.PICK || kind == MovableModule.EnumAction.THROW) {
                action.setInfo(new Object[]{buf.readInt()});
            } else if (kind == MovableModule.EnumAction.LENGTH_CHANGE) {
                action.setInfo(new Object[]{buf.readBoolean(), buf.readInt()});
            } else if (kind == MovableModule.EnumAction.ATTACH_OBJECTS) {
                action.setInfo(new Object[]{buf.readBoolean()});
            }
        }
        this.moduleAction = action;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(moduleAction.getMovableAction().ordinal());
        buf.writeInt(moduleAction.getInfo().length);
        for (Object o : moduleAction.getInfo()) {
            if (o instanceof Boolean b) {
                buf.writeBoolean(b);
            } else if (o instanceof Integer i) {
                buf.writeInt(i);
            } else if (o instanceof Float f) {
                buf.writeFloat(f);
            }
        }
    }

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        if (side != LogicalSide.SERVER || context == null || context.level() == null) {
            return;
        }
        IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(context.level());
        if (physicsWorld == null) {
            return;
        }
        physicsWorld.schedule(() -> PickingObjectHelper.handlePickingControl(moduleAction, context));
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
