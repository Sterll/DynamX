package fr.dynamx.server.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.dynamx.common.command.ISubCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * Set the explosion force used by shockwave physics events.
 *
 * <p>TODO port:1.20.1 - Brigadier port.
 */
public class CmdShockWave implements ISubCommand {
    public static float explosionForce = 10;

    @Override
    public String getName() {
        return "shockwave";
    }

    @Override
    public String getUsage() {
        return "shockwave <force> - Changes shockwave force";
    }

    @Override
    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
        if (args != null && args.length == 2) {
            try {
                explosionForce = Float.parseFloat(args[1]);
                final float f = explosionForce;
                sender.sendSuccess(() -> Component.literal("Set force to " + f), false);
            } catch (NumberFormatException e) {
                sender.sendFailure(Component.literal("Usage: " + getUsage()));
            }
        } else {
            sender.sendFailure(Component.literal("Usage: " + getUsage()));
        }
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // TODO port:1.20.1 - Brigadier port: literal("shockwave").then(argument("force", FloatArgumentType.floatArg())).executes(...).
    }
}
