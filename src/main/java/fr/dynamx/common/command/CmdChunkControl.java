package fr.dynamx.common.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Chunk control debugging sub-command.
 *
 * <p>TODO port:1.20.1 - Brigadier port; legacy body referenced
 * {@code IPhysicsWorld}/{@code ChunkCollisions}/{@code ChunkLoadingTicket}/{@code ChunkGraph}
 * which are not yet ported.
 */
public class CmdChunkControl implements ISubCommand {
    private final boolean isClient;

    public CmdChunkControl(boolean isClient) {
        this.isClient = isClient;
    }

    @SuppressWarnings("unused")
    private String prefix() {
        if (isClient) {
            return ChatFormatting.GOLD + "[DynamX-Client] " + ChatFormatting.RESET;
        }
        return ChatFormatting.GREEN + "[DynamX-Server] " + ChatFormatting.RESET;
    }

    @Override
    public String getName() {
        return "chunkcontrol";
    }

    @Override
    public String getUsage() {
        return getName() + " <graphmode|getelements|getslopes|clearslopes|getgraph|resetstate|fullinfo> <pos|mode>";
    }

    @Override
    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
        // TODO port:1.20.1 - port to Brigadier register(dispatcher); needs IPhysicsWorld+ChunkCollisions ports.
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // TODO port:1.20.1 - Brigadier port: argument tree (graphmode|getelements|getslopes|clear|getgraph|resetstate|fullinfo).
    }

    @Override
    public void getTabCompletions(MinecraftServer server, CommandSourceStack sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        // TODO port:1.20.1 - replaced by Brigadier argument suggestions.
    }
}
