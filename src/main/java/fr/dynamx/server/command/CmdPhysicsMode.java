package fr.dynamx.server.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.dynamx.common.command.ISubCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Toggle physics simulation mode (full/light) for client/server sides.
 *
 * <p>TODO port:1.20.1 - Brigadier port; depends on {@code IPhysicsSimulationMode} and
 * {@code PhysicsSimulationModes} which aren't yet fully ported.
 */
public class CmdPhysicsMode implements ISubCommand {
    @Override
    public String getName() {
        return "set_physics_mode";
    }

    @Override
    public String getUsage() {
        return getRootCommandUsage() + getName() + " <get|full|light> [server_only|client_server]";
    }

    @Override
    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
        // TODO port:1.20.1 - port to Brigadier register(dispatcher); needs PhysicsSimulationModes port.
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // TODO port:1.20.1 - Brigadier port.
    }

    @Override
    public void getTabCompletions(MinecraftServer server, CommandSourceStack sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        // TODO port:1.20.1 - replaced by Brigadier argument suggestions.
    }
}
