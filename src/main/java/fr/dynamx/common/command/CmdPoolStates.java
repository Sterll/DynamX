package fr.dynamx.common.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.dynamx.utils.optimization.BoundingBoxPool;
import fr.dynamx.utils.optimization.HashMapPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.UDPByteArrayPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Prints memory pool states; used for memory diagnostics.
 *
 * <p>TODO port:1.20.1 - Brigadier port. Logic translates fine to 1.20.1; physics-world scheduling
 * paths are stubbed since the IPhysicsWorld bridge is still in progress.
 */
public class CmdPoolStates implements ISubCommand {
    @Override
    public String getName() {
        return "pool_states";
    }

    @Override
    public String getUsage() {
        return "/[client_]dynamx pool_states [inspect]";
    }

    @Override
    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
        boolean client = sender.getLevel() != null && sender.getLevel().isClientSide;
        if (client) {
            sender.sendSuccess(() -> Component.literal(ChatFormatting.DARK_PURPLE + "Side: client"), false);
        } else {
            sender.sendSuccess(() -> Component.literal(ChatFormatting.BLUE + "Side: server"), false);
        }

        if (args != null && args.length == 3 && args[1].matches("inspect")) {
            String target = args[2];
            String result;
            switch (target) {
                case "Vector3f":
                    sender.sendSuccess(() -> Component.literal(ChatFormatting.GOLD + "Vector3f pool inspection:"), false);
                    result = Vector3fPool.getPool().getExpandedDebugInfo();
                    final String r1 = result;
                    sender.sendSuccess(() -> Component.literal(r1), false);
                    // TODO port:1.20.1 - schedule on physics thread when IPhysicsWorld bridge is ready.
                    break;
                case "Quaternion":
                    sender.sendSuccess(() -> Component.literal(ChatFormatting.GOLD + "Quaternion pool inspection:"), false);
                    result = QuaternionPool.getPool().getExpandedDebugInfo();
                    final String r2 = result;
                    sender.sendSuccess(() -> Component.literal(r2), false);
                    // TODO port:1.20.1 - schedule on physics thread when IPhysicsWorld bridge is ready.
                    break;
                default:
                    break;
            }
            return;
        }

        String result = "BoundingBox: " + BoundingBoxPool.getPool().getDebugInfo() + "\n" +
                "HashMap: " + HashMapPool.getINSTANCE().getDebugInfo() + "\n" +
                "Quaternion: " + QuaternionPool.getPool().getDebugInfo() + "\n" +
                "UDPByteArray: " + UDPByteArrayPool.getINSTANCE().getDebugInfo() + "\n" +
                "Vector3f: " + Vector3fPool.getPool().getDebugInfo() + "\n";

        if (client) {
            result += getClientResult();
        }

        final String finalResult = result;
        sender.sendSuccess(() -> Component.literal(ChatFormatting.GOLD + "Current thread pools:"), false);
        sender.sendSuccess(() -> Component.literal(finalResult), false);
        // TODO port:1.20.1 - physics-thread snapshot path requires IPhysicsWorld scheduling.
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // TODO port:1.20.1 - Brigadier port: literal("pool_states").then(literal("inspect").then(argument("type", string()))).
    }

    @Override
    public void getTabCompletions(MinecraftServer server, CommandSourceStack sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        // TODO port:1.20.1 - replaced by Brigadier argument suggestions.
    }

    private String getClientResult() {
        return "GlQuaternion: " + fr.dynamx.utils.optimization.GlQuaternionPool.getINSTANCE().getDebugInfo() + "\n";
    }
}
