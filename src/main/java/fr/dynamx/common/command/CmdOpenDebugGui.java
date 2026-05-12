package fr.dynamx.common.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.network.packets.MessageOpenDebugGui;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Opens the DynamX debug GUI.
 *
 * <p>TODO port:1.20.1 - Brigadier port; client-side body relies on acsguis ACsGuiApi which is not yet ported.
 */
public class CmdOpenDebugGui implements ISubCommand {
    @Override
    public String getName() {
        return "debug_gui";
    }

    @Override
    public String getUsage() {
        return "debug_gui";
    }

    @Override
    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
        // TODO port:1.20.1 - port to Brigadier register(dispatcher).
        try {
            ServerPlayer player = sender.getPlayerOrException();
            DynamXContext.getNetwork().sendToClient(new MessageOpenDebugGui((byte) 125), EnumPacketTarget.PLAYER, player);
        } catch (Exception e) {
            // Console or non-player: try client path
            if (sender.getEntity() instanceof Player) {
                executeClient();
            }
        }
    }

    @Override
    public com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildBrigadier() {
        return net.minecraft.commands.Commands.literal(getName()).executes(ctx -> {
            execute(ctx.getSource().getServer(), ctx.getSource(), new String[]{getName()});
            return 1;
        });
    }

    @OnlyIn(Dist.CLIENT)
    private void executeClient() {
        // TODO port:1.20.1 - relies on ACsGuiApi (acsguis) and NewGuiDnxDebug; not ported yet.
    }
}
