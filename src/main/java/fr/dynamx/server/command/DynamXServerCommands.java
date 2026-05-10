package fr.dynamx.server.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.dynamx.common.command.CmdChunkControl;
import fr.dynamx.common.command.CmdOpenDebugGui;
import fr.dynamx.common.command.CmdPoolStates;
import fr.dynamx.common.command.CmdPrintVehicleInfos;
import fr.dynamx.common.command.CmdTerrainDebug;
import fr.dynamx.common.command.ISubCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Root /dynamx command dispatcher.
 *
 * <p>In 1.12 this extended {@code CommandBase} and was registered via
 * {@code FMLServerStartingEvent#registerServerCommand}. In 1.20.1 NeoForge command
 * registration moves to {@link RegisterCommandsEvent}, which exposes a
 * {@link CommandDispatcher} for {@link CommandSourceStack}.
 *
 * <p>TODO port:1.20.1 - Forge {@code PermissionAPI.registerNode(node, DefaultPermissionLevel.OP, desc)}
 * has no clean equivalent in NeoForge 1.20.1: it has been replaced by the new
 * {@code net.minecraftforge.server.permission.PermissionAPI} (declarative nodes,
 * registered via {@code PermissionGatherEvent.Nodes}). Wired with TODOs for now.
 */
public class DynamXServerCommands {
    private final Map<String, ISubCommand> commands = new HashMap<>();

    public DynamXServerCommands() {
        addCommand(new CmdSlopes());
        addCommand(new CmdReloadConfig());
        addCommand(new CmdRefreshChunks());
        addCommand(new CmdNetworkConfig());
        addCommand(new CmdChunkControl(false));
        addCommand(new CmdSpawnObjects());
        addCommand(new CmdKillEntities());
        addCommand(new CmdOpenDebugGui());
        addCommand(new CmdSpawnRagdoll());
        addCommand(new CmdShockWave());
        addCommand(new CmdTerrainDebug(false));
        addCommand(new CmdPhysicsMode());
        addCommand(new CmdPrintVehicleInfos());
        addCommand(new CmdPoolStates());
    }

    public void addCommand(ISubCommand command) {
        commands.put(command.getName(), command);
        // TODO port:1.20.1 - permission registration moved to PermissionGatherEvent.Nodes (NeoForge).
    }

    public String getName() {
        return "dynamx";
    }

    public Map<String, ISubCommand> getCommands() {
        return commands;
    }

    /**
     * NeoForge entry point: forwards to every sub-command's {@link ISubCommand#register(CommandDispatcher)}.
     */
    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        // TODO port:1.20.1 - Brigadier port: build a single literal("dynamx") node, then attach all
        //   sub-commands as children using their register(dispatcher) hooks.
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        for (ISubCommand cmd : commands.values()) {
            cmd.register(dispatcher);
        }
    }

    public ServerPlayer getPlayer(String name) {
        MinecraftServer mc = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        return mc == null ? null : mc.getPlayerList().getPlayerByName(name);
    }
}
