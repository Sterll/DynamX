package fr.dynamx.server.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.dynamx.common.command.ISubCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import java.util.List;

/**
 * Refresh DynamX collision chunks in a given AABB.
 *
 * <p>TODO port:1.20.1 - Brigadier port; needs ported {@code DynamXContext.getPhysicsWorld()}
 * terrain manager bridge.
 */
public class CmdRefreshChunks implements ISubCommand {
    @Override
    public String getName() {
        return "refresh_chunks";
    }

    @Override
    public String getUsage() {
        return getName() + " <x1> <y1> <z1> <x2> <y2> <z2> - Updates collisions in the given zone";
    }

    @Override
    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
        // TODO port:1.20.1 - port to Brigadier register(dispatcher); needs terrain-manager port.
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // TODO port:1.20.1 - Brigadier port: argument("from", BlockPosArgument.blockPos()).then(argument("to", BlockPosArgument.blockPos())).executes(...).
    }

    @Override
    public void getTabCompletions(MinecraftServer server, CommandSourceStack sender, String[] args, BlockPos targetPos, List<String> r) {
        // TODO port:1.20.1 - replaced by Brigadier BlockPosArgument suggestions.
    }
}
