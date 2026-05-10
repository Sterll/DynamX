package fr.dynamx.server.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.dynamx.common.command.ISubCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Kill DynamX entities by category.
 *
 * <p>TODO port:1.20.1 - Brigadier port; needs ported entity classes
 * ({@code CarEntity}, {@code BoatEntity}, {@code HelicopterEntity},
 * {@code PropsEntity}, {@code RagdollEntity}, {@code TrailerEntity}, {@code DoorEntity}).
 */
public class CmdKillEntities implements ISubCommand {
    @Override
    public String getName() {
        return "kill";
    }

    @Override
    public String getUsage() {
        return getName() + " <all|cars|boats|helicopters|props|ragdolls|trailers|doors>";
    }

    @Override
    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
        // TODO port:1.20.1 - port to Brigadier register(dispatcher); needs ported PhysicsEntity subclasses.
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // TODO port:1.20.1 - Brigadier port: literal("kill").then(literal("all"|"cars"|...).executes(...)).
    }

    @Override
    public void getTabCompletions(MinecraftServer server, CommandSourceStack sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        // TODO port:1.20.1 - replaced by Brigadier argument suggestions.
    }
}
