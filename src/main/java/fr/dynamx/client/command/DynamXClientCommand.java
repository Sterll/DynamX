package fr.dynamx.client.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.dynamx.common.command.CmdChunkControl;
import fr.dynamx.common.command.CmdOpenDebugGui;
import fr.dynamx.common.command.CmdPoolStates;
import fr.dynamx.common.command.CmdPrintVehicleInfos;
import fr.dynamx.common.command.CmdTerrainDebug;
import fr.dynamx.common.command.ISubCommand;
import fr.dynamx.utils.optimization.PooledHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side {@code /client_dynamx} command.
 *
 * <p>TODO port:1.20.1 - The legacy {@code CommandBase}/{@code ICommandSender}/
 * {@code ClientCommandHandler.instance.registerCommand} pipeline is gone. In 1.20.1 client-only
 * commands register through {@code RegisterClientCommandsEvent} on the mod bus with a Brigadier
 * {@code LiteralArgumentBuilder<CommandSourceStack>}. The body below keeps the legacy
 * sub-command map alive but the actual /client_dynamx registration must happen elsewhere
 * (Phase 9 / client setup).</p>
 */
public class DynamXClientCommand {
    private final Map<String, ISubCommand> commands = new HashMap<>();

    public DynamXClientCommand() {
        addCommand(new CmdOpenDebugGui());
        addCommand(new CmdChunkControl(true));
        addCommand(new CmdTerrainDebug(true));
        addCommand(new CmdClientNetworkDebug());
        addCommand(new CmdUdpTest());
        addCommand(new CmdPrintVehicleInfos());
        addCommand(new CmdPoolStates());

        // TODO INVESTIGATE ON DISABLE MAP POOL ON CLIENT THEN REMOVE
        addCommand(new ISubCommand() {
            @Override
            public String getName() {
                return "disablemappool";
            }

            @Override
            public String getUsage() {
                return "disablemappool <false/true> - tests for hash map pool optimizations";
            }

            @Override
            public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
                if (args.length == 2) {
                    boolean testFullGo = Boolean.parseBoolean(args[1]);
                    PooledHashMap.DISABLE_POOL = testFullGo;
                    sender.sendSystemMessage(Component.literal("Set disablemappool to " + testFullGo));
                } else {
                    sender.sendSystemMessage(Component.literal(getUsage()));
                }
            }
        });
    }

    public void addCommand(ISubCommand command) {
        commands.put(command.getName(), command);
    }

    public String getName() {
        return "client_dynamx";
    }

    public String getUsage() {
        StringBuilder usage = new StringBuilder();
        commands.keySet().forEach(s -> usage.append("|").append(s));
        return "/dynamx <" + usage.substring(1) + ">";
    }

    /**
     * <p>TODO port:1.20.1 - Brigadier registration entry point. Builds /client_dynamx with each
     * sub-command's {@link ISubCommand#register(CommandDispatcher)} hook. Stubbed for now.</p>
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // TODO port:1.20.1 - Brigadier port: build LiteralArgumentBuilder.literal("client_dynamx")
        // and chain each sub-command via .then(...).
        for (ISubCommand sub : commands.values()) {
            sub.register(dispatcher);
        }
    }
}
