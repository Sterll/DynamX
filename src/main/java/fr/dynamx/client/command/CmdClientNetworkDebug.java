package fr.dynamx.client.command;

import fr.dynamx.common.command.ISubCommand;
import fr.dynamx.common.network.sync.variables.NetworkActivityTracker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * <p>TODO port:1.20.1 - The legacy {@code CommandBase.parseInt(String)} helper is gone.
 * We inline it as a plain {@link Integer#parseInt(String)} here. {@code TextComponentString}
 * -> {@code Component.literal}. {@code sender.sendMessage} -> {@code sender.sendSystemMessage}.</p>
 */
public class CmdClientNetworkDebug implements ISubCommand {
    @Override
    public String getName() {
        return "network_debug";
    }

    @Override
    public String getUsage() {
        return "/client_dynamx network_debug <pause|resume|set|get|next|prev|set_entity>";
    }

    @Override
    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
        if (args.length < 2) {
            sender.sendSystemMessage(Component.literal(getUsage()));
            return;
        }
        switch (args[1]) {
            case "pause":
                NetworkActivityTracker.pause();
                sender.sendSystemMessage(Component.literal("Paused"));
                break;
            case "resume":
                NetworkActivityTracker.resume();
                sender.sendSystemMessage(Component.literal("Resumed"));
                break;
            case "set":
                if (args.length < 3) {
                    sender.sendSystemMessage(Component.literal("/client_dynamx network_debug set <view_index>"));
                    return;
                }
                NetworkActivityTracker.viewIndex = Integer.parseInt(args[2]);
                break;
            case "next":
                NetworkActivityTracker.viewIndex += 10;
                break;
            case "prev":
                NetworkActivityTracker.viewIndex -= 10;
                break;
            case "set_entity":
                if (args.length < 3) {
                    sender.sendSystemMessage(Component.literal("/client_dynamx network_debug set_entity <entity_id>"));
                    return;
                }
                NetworkActivityTracker.viewEntity = Integer.parseInt(args[2]);
                break;
            default:
                sender.sendSystemMessage(Component.literal(getUsage()));
                return;
        }
        sender.sendSystemMessage(Component.literal("Selected tick is " + NetworkActivityTracker.viewIndex + ". Last is " + NetworkActivityTracker.lastTime));
        sender.sendSystemMessage(Component.literal("Selected entity is " + NetworkActivityTracker.viewEntity));
    }

    @Override
    public void getTabCompletions(MinecraftServer server, CommandSourceStack sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        if (args.length == 2) {
            // TODO port:1.20.1 - replace static matching with Brigadier SuggestionProvider.
            for (String s : new String[]{"pause", "resume", "set", "next", "prev", "set_entity"}) {
                if (s.startsWith(args[1].toLowerCase())) r.add(s);
            }
        }
    }
}
