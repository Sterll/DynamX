package fr.dynamx.common.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.dynamx.utils.DynamXConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Toggles the terrain debug flag.
 *
 * <p>TODO port:1.20.1 - Brigadier port; logic ported cleanly.
 */
public class CmdTerrainDebug implements ISubCommand {
    private final boolean isClient;

    public CmdTerrainDebug(boolean isClient) {
        this.isClient = isClient;
    }

    private String prefix() {
        if (isClient) {
            return ChatFormatting.GOLD + "[DynamX-Client] " + ChatFormatting.RESET;
        }
        return ChatFormatting.GREEN + "[DynamX-Server] " + ChatFormatting.RESET;
    }

    @Override
    public String getName() {
        return "terrain_debug";
    }

    @Override
    public String getUsage() {
        return "terrain_debug [false|true] - enables terrain debug in console. Has a performance impact.";
    }

    @Override
    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
        if (args != null && args.length == 2) {
            boolean enableDebug = Boolean.parseBoolean(args[1]);
            DynamXConfig.enableDebugTerrainManager = enableDebug;
            final String p = prefix();
            sender.sendSuccess(() -> Component.literal(p + (enableDebug ? "Enabled" : "Disabled") + " terrain debug. Enabling it can have some performance impact. This configuration will not persist after game restart."), false);
        } else {
            final String p = prefix();
            sender.sendSuccess(() -> Component.literal(p + "Terrain debug is " + (DynamXConfig.enableDebugTerrainManager ? "enabled" : "disabled")), false);
        }
    }

    @Override
    public com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildBrigadier() {
        return net.minecraft.commands.Commands.literal(getName())
                .executes(ctx -> {
                    execute(ctx.getSource().getServer(), ctx.getSource(), new String[]{getName()});
                    return 1;
                })
                .then(net.minecraft.commands.Commands.argument("value",
                                com.mojang.brigadier.arguments.BoolArgumentType.bool())
                        .executes(ctx -> {
                            boolean value = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "value");
                            execute(ctx.getSource().getServer(), ctx.getSource(),
                                    new String[]{getName(), Boolean.toString(value)});
                            return 1;
                        }));
    }
}
