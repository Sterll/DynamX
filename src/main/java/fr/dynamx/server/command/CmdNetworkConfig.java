package fr.dynamx.server.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.dynamx.common.command.ISubCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import java.util.List;

/**
 * Network sync config sub-command (resync_params/sync_params/sync_delay/epsilon/resyncId).
 *
 * <p>TODO port:1.20.1 - Brigadier port; touches {@code PlayerSyncBuffer}, {@code EntityPosVariable},
 * {@code SyncHelper}, network packets that are partially ported.
 */
public class CmdNetworkConfig implements ISubCommand {
    @Override
    public String getName() {
        return "network_config";
    }

    @Override
    public String getUsage() {
        return getName() + " <resync_params|sync_params|sync_delay|epsilon|resyncId>";
    }

    @Override
    public void getTabCompletions(MinecraftServer server, CommandSourceStack sender, String[] args, BlockPos targetPos, List<String> r) {
        // TODO port:1.20.1 - replaced by Brigadier argument suggestions.
    }

    @Override
    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
        // TODO port:1.20.1 - port to Brigadier register(dispatcher); needs ported EntityPosVariable/SyncHelper.
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // TODO port:1.20.1 - Brigadier port.
    }
}
