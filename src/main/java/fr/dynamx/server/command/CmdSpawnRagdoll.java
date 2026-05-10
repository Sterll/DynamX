package fr.dynamx.server.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.dynamx.common.command.ISubCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Spawn a ragdoll from a player.
 *
 * <p>TODO port:1.20.1 - Brigadier port; needs {@code RagdollEntity} and
 * {@code DynamXContext.getPlayerToCollision()} ports.
 */
public class CmdSpawnRagdoll implements ISubCommand {
    @Override
    public String getName() {
        return "spawn_ragdoll";
    }

    @Override
    public String getUsage() {
        return "spawn_ragdoll [help|player] [life_expectancy] [velocity_x] [velocity_y] [velocity_z]";
    }

    @Override
    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
        // TODO port:1.20.1 - port to Brigadier register(dispatcher); needs RagdollEntity port.
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // TODO port:1.20.1 - Brigadier port: literal("spawn_ragdoll").then(argument("player", EntityArgument.player())).executes(...).
    }

    @Override
    public void getTabCompletions(MinecraftServer server, CommandSourceStack sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        // TODO port:1.20.1 - replaced by Brigadier EntityArgument suggestions.
    }
}
