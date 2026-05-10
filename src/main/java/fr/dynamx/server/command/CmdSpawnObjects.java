package fr.dynamx.server.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.dynamx.common.command.ISubCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Mass-spawn DynamX vehicles/props for stress testing.
 *
 * <p>TODO port:1.20.1 - Brigadier port; references {@code DynamXObjectLoaders},
 * {@code DynamXItemSpawner}, {@code RagdollEntity}, {@code TaskScheduler.ScheduledTask},
 * {@code PhysicsEntityEvent.Spawn}. Most loader/spawner pieces are partially ported.
 */
public class CmdSpawnObjects implements ISubCommand {
    private final List<String> objectIds = new ArrayList<>();

    public CmdSpawnObjects() {
        // TODO port:1.20.1 - rebuild from DynamXObjectLoaders (WHEELED_VEHICLES, TRAILERS, BOATS, HELICOPTERS, PROPS) + "ragdoll".
    }

    @Override
    public String getName() {
        return "spawn_objects";
    }

    @Override
    public String getUsage() {
        return "spawn_objects <object_id> <x> <y> <z> [cooldown] [width] [height] [depth] [xSpace] [ySpace] [zSpace] [spawnsPerStep] [delayBetweenSteps]";
    }

    @Override
    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
        // TODO port:1.20.1 - port to Brigadier register(dispatcher); needs ported DynamXItemSpawner/RagdollEntity.
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // TODO port:1.20.1 - Brigadier port: complex literal tree.
    }

    public static Object getSpawnItem(String vehicleModel) {
        // TODO port:1.20.1 - return DynamXItemSpawner once item registry path is fully ported.
        return null;
    }

    @Override
    public void getTabCompletions(MinecraftServer server, CommandSourceStack sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        // TODO port:1.20.1 - replaced by Brigadier argument suggestions.
    }
}
