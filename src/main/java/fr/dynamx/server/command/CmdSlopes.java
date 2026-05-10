package fr.dynamx.server.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.dynamx.common.command.ISubCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import java.util.List;

/**
 * Slope builder admin command (create/delete/automatic/enableAutoSlopes).
 *
 * <p>TODO port:1.20.1 - Brigadier port; the legacy body manipulates {@code ItemSlopes} NBT,
 * {@code SlopeGenerator}, {@code SlopeBuildingConfig}, {@code ChunkCollisions}, and triggers
 * {@code MessageUpdateChunk}/{@code MessageSwitchAutoSlopesMode}. Slope subsystem is partly ported.
 */
public class CmdSlopes implements ISubCommand {
    @Override
    public String getName() {
        return "slopes";
    }

    @Override
    public String getUsage() {
        return getName() + " <create|delete|automatic|enableAutoSlopes>";
    }

    @Override
    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
        // TODO port:1.20.1 - port to Brigadier register(dispatcher); needs ItemSlopes/SlopeGenerator ports.
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // TODO port:1.20.1 - Brigadier port: complex literal tree with sub-arguments per mode.
    }

    @Override
    public void getTabCompletions(MinecraftServer server, CommandSourceStack sender, String[] args, BlockPos targetPos, List<String> r) {
        // TODO port:1.20.1 - replaced by Brigadier argument suggestions.
    }
}
