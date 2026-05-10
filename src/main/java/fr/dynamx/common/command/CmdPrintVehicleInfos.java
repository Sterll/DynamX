package fr.dynamx.common.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

/**
 * Vehicle debug command: prints info about the entity ridden by the executing player.
 *
 * <p>TODO port:1.20.1 - Brigadier port; relies on {@code PhysicsEntity},
 * {@code ModularPhysicsEntity}, {@code PackPhysicsEntity}, {@code WheelsModule},
 * {@code EntityNode} (client scene-graph) which are not fully ported.
 */
public class CmdPrintVehicleInfos implements ISubCommand {
    @Override
    public String getName() {
        return "vehicle_debug";
    }

    @Override
    public String getUsage() {
        return "/[client_]dynamx vehicle_debug";
    }

    @Override
    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
        // TODO port:1.20.1 - port to Brigadier register(dispatcher); needs PhysicsEntity hooks.
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // TODO port:1.20.1 - Brigadier port: literal("vehicle_debug").executes(ctx -> ...).
    }
}
