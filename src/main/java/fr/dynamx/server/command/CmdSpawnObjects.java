package fr.dynamx.server.command;

import com.jme3.math.Vector3f;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.common.command.ISubCommand;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.items.DynamXItemSpawner;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * /dynamx spawn &lt;name&gt; - spawn a pack vehicle/prop at the executing player's position.
 *
 * <p>Replaces the legacy mass-spawn stress-test command. The richer iteration logic (cooldown,
 * matrix spread, scheduled steps) has been dropped pending a port of {@code TaskScheduler}.
 */
public class CmdSpawnObjects implements ISubCommand {
    @Override
    public String getName() {
        return "spawn";
    }

    @Override
    public String getUsage() {
        return "spawn <name>";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildBrigadier() {
        SuggestionProvider<CommandSourceStack> suggestions = (ctx, builder) ->
                SharedSuggestionProvider.suggest(collectIds().stream(), builder);
        return Commands.literal(getName())
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(suggestions)
                        .executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "name");
                            CommandSourceStack source = ctx.getSource();
                            ServerPlayer player = source.getPlayer();
                            ServerLevel level = player != null ? player.serverLevel() : source.getLevel();
                            Vec3 origin = source.getPosition();
                            float yaw = player != null ? player.getYRot() : source.getRotation().y;
                            return spawnAt(source, id, level,
                                    new Vector3f((float) origin.x, (float) origin.y, (float) origin.z),
                                    yaw);
                        }));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int spawnAt(CommandSourceStack source, String id, Level world, Vector3f pos, float yaw) {
        DynamXItemSpawner<?> item = getSpawnItem(id);
        if (item == null) {
            source.sendFailure(Component.literal("DynamX: no item registered for '" + id
                    + "'. Check the pack loaded without errors."));
            return 0;
        }
        Object entity = item.getSpawnEntity(world, null, pos, yaw, 0);
        if (!(entity instanceof Entity)) {
            source.sendFailure(Component.literal("DynamX: spawn factory for '" + id
                    + "' returned " + (entity == null ? "null" : entity.getClass().getName())));
            return 0;
        }
        world.addFreshEntity((Entity) entity);
        source.sendSuccess(() -> Component.literal("Spawned " + id + " at "
                + (int) pos.x + " " + (int) pos.y + " " + (int) pos.z), true);
        return 1;
    }

    private static List<String> collectIds() {
        List<String> ids = new ArrayList<>();
        collectFrom(DynamXObjectLoaders.WHEELED_VEHICLES.owners, ids);
        collectFrom(DynamXObjectLoaders.TRAILERS.owners, ids);
        collectFrom(DynamXObjectLoaders.BOATS.owners, ids);
        collectFrom(DynamXObjectLoaders.HELICOPTERS.owners, ids);
        collectFrom(DynamXObjectLoaders.PROPS.owners, ids);
        return ids;
    }

    private static void collectFrom(List<? extends IDynamXItem<?>> owners, List<String> out) {
        for (IDynamXItem<?> owner : owners) {
            String name = owner.getInfo() instanceof fr.dynamx.common.contentpack.type.ObjectInfo
                    ? ((fr.dynamx.common.contentpack.type.ObjectInfo<?>) owner.getInfo()).getFullName()
                    : owner.toString();
            out.add(name);
        }
    }

    public static DynamXItemSpawner<?> getSpawnItem(String fullName) {
        DynamXItemSpawner<?> hit = findIn(DynamXObjectLoaders.WHEELED_VEHICLES.owners, fullName);
        if (hit != null) return hit;
        hit = findIn(DynamXObjectLoaders.TRAILERS.owners, fullName);
        if (hit != null) return hit;
        hit = findIn(DynamXObjectLoaders.BOATS.owners, fullName);
        if (hit != null) return hit;
        hit = findIn(DynamXObjectLoaders.HELICOPTERS.owners, fullName);
        if (hit != null) return hit;
        return findIn(DynamXObjectLoaders.PROPS.owners, fullName);
    }

    private static DynamXItemSpawner<?> findIn(List<? extends IDynamXItem<?>> owners, String fullName) {
        for (IDynamXItem<?> owner : owners) {
            if (!(owner instanceof DynamXItemSpawner)) continue;
            if (!(owner.getInfo() instanceof fr.dynamx.common.contentpack.type.ObjectInfo)) continue;
            String name = ((fr.dynamx.common.contentpack.type.ObjectInfo<?>) owner.getInfo()).getFullName();
            if (name.equals(fullName)) return (DynamXItemSpawner<?>) owner;
        }
        return null;
    }

    @Override
    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    }

    @Override
    public void getTabCompletions(MinecraftServer server, CommandSourceStack sender, String[] args,
                                  @Nullable BlockPos targetPos, List<String> r) {
        if (args.length == 2) r.addAll(collectIds());
    }
}
