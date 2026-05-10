package fr.dynamx.common.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * /dynamx sub commands helper.
 *
 * <p>TODO port:1.20.1 - In 1.13+ commands use Brigadier. The legacy {@code execute(server, sender, args[])}
 * model is preserved here only as a thin compatibility surface; the real registration must happen
 * through {@link #register(CommandDispatcher)} which builds a {@code LiteralArgumentBuilder<CommandSourceStack>}.
 */
public interface ISubCommand {
    String getName();

    String getUsage();

    /**
     * Legacy entry point. New port targets {@link #register(CommandDispatcher)}.
     *
     * <p>TODO port:1.20.1 - port to Brigadier register(dispatcher).
     */
    default void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
        // TODO port:1.20.1 - port to Brigadier register(dispatcher)
    }

    /**
     * Brigadier registration hook. Implementations should attach this sub-command to the given
     * dispatcher (or to a parent literal). Most implementations are stubbed in Phase 9.
     *
     * <p>TODO port:1.20.1 - Brigadier port.
     */
    default void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // TODO port:1.20.1 - Brigadier port: attach sub-command on the dispatcher.
    }

    default void getTabCompletions(MinecraftServer server, CommandSourceStack sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
    }

    default String getPermission() {
        return DynamXConstants.ID + ".command." + getName();
    }

    default String getRootCommandUsage() {
        return "/dynamx ";
    }
}
