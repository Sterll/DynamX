package fr.dynamx.client.command;

import fr.dynamx.client.network.ClientPhysicsSyncManager;
import fr.dynamx.client.network.udp.UdpClientNetworkHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.command.ISubCommand;
import fr.dynamx.common.network.packets.MessagePing;
import fr.dynamx.common.network.udp.UdpTestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * <p>TODO port:1.20.1 - same migration set as {@link CmdClientNetworkDebug}:
 * {@code CommandBase.parseInt(String, int, int)} replaced with bounded {@link Integer#parseInt(String)},
 * {@code TextComponentString} -> {@code Component.literal}.</p>
 */
public class CmdUdpTest implements ISubCommand {
    private Thread tester;
    private int packetsPerTick;
    private int remainingPackets;
    private int remainingTicks;
    private int packetId;
    public static boolean[] received;
    public static long startTime = 0;

    private final Runnable TESTER = new Runnable() {
        @Override
        public void run() {
            startTime = System.currentTimeMillis();
            while (remainingPackets > 0) {
                remainingTicks--;
                remainingPackets -= packetsPerTick;
                for (int i = 0; i < packetsPerTick; i++) {
                    ((UdpClientNetworkHandler) DynamXContext.getNetwork().getQuickNetwork()).sendPacket(new UdpTestPacket(packetId, System.currentTimeMillis()));
                    packetId++;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int failCount = 0;
            for (boolean b : received) {
                if (!b)
                    failCount++;
            }
            if (Minecraft.getInstance().player != null)
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("UDP test finished over " + packetId + " sent packets with " + failCount + " loss"));
            else {
                System.out.println("UDP test finished over " + packetId + " sent packets with " + failCount + " loss");
            }
        }
    };

    @Override
    public String getName() {
        return "udp_test";
    }

    @Override
    public String getUsage() {
        return "/client_dynamx udp_test <ping/test> [totalpackets] [packetspertick]";
    }

    @Override
    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
        try {
            if (args.length == 4 && args[1].matches("test")) {
                if (tester != null && tester.isAlive()) {
                    sender.sendSystemMessage(Component.literal("Already testing udp !"));
                } else if (!(DynamXContext.getNetwork().getQuickNetwork() instanceof UdpClientNetworkHandler)) {
                    sender.sendSystemMessage(Component.literal("UDP isn't started !"));
                } else {
                    remainingPackets = parseBoundedInt(args[2], 1, 100000);
                    packetsPerTick = parseBoundedInt(args[3], 1, 100);
                    remainingTicks = remainingPackets / packetsPerTick;
                    received = new boolean[remainingPackets];
                    packetId = 0;
                    tester = new Thread(TESTER);
                    tester.setName("UDP tester");
                    sender.sendSystemMessage(Component.literal("UDP is in test, " + remainingTicks + " ticks remaining..."));
                    tester.start();
                }
            } else if (args.length == 2 && args[1].equalsIgnoreCase("ping")) {
                sender.sendSystemMessage(Component.literal("[DynamX] Pinging..."));
                ClientPhysicsSyncManager.pingMs = -3;
                DynamXContext.getNetwork().sendToServer(new MessagePing(System.currentTimeMillis(), true));
            } else {
                sender.sendSystemMessage(Component.literal(getUsage()));
            }
        } catch (NumberFormatException e) {
            sender.sendSystemMessage(Component.literal(getUsage()));
        }
    }

    private static int parseBoundedInt(String s, int min, int max) {
        int v = Integer.parseInt(s);
        if (v < min || v > max) throw new NumberFormatException("Value " + v + " not in [" + min + ", " + max + "]");
        return v;
    }
}
