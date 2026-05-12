package fr.dynamx.server.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.dynamx.common.command.ISubCommand;
import fr.dynamx.utils.DynamXLoadingTasks;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * Reload DynamX pack configuration at runtime.
 *
 * <p>TODO port:1.20.1 - Brigadier port.
 */
public class CmdReloadConfig implements ISubCommand {
    @Override
    public String getName() {
        return "reload_config";
    }

    @Override
    public String getUsage() {
        return getName();
    }

    @Override
    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
        Component msg = Component.literal("/!\\ Reloading DynamX packs, you may lag").withStyle(ChatFormatting.GOLD);
        if (server != null && server.getPlayerList() != null) {
            server.getPlayerList().broadcastSystemMessage(msg, false);
        }
        DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.SERVER_RUNNING, DynamXLoadingTasks.PACK).thenAccept(empty -> {
            sender.sendSuccess(() -> Component.literal("Packs reloaded"), true);
            // TODO port:1.20.1 - report pack errors when DynamXErrorManager.hasErrors() is ported.
        });
    }

    @Override
    public com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildBrigadier() {
        return net.minecraft.commands.Commands.literal(getName()).executes(ctx -> {
            execute(ctx.getSource().getServer(), ctx.getSource(), new String[]{getName()});
            return 1;
        });
    }
}
