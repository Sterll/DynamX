package fr.dynamx.common;

import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.network.IDnxNetworkSystem;
import fr.dynamx.api.physics.IPhysicsSimulationMode;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.api.physics.IRotatedCollisionHandler;
import fr.dynamx.client.DynamXModelRegistry;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.handlers.RotatedCollisionHandlerImpl;
import fr.dynamx.common.network.DynamXNetwork;
import fr.dynamx.common.objloader.data.DxModelData;
import fr.dynamx.common.objloader.data.GltfModelData;
import fr.dynamx.common.objloader.data.ObjModelData;
import fr.dynamx.common.physics.player.PlayerPhysicsHandler;
import fr.dynamx.common.physics.world.PhysicsSimulationModes;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Common DynamX variables.
 *
 * <p>TODO port:1.20.1 -
 * <ul>
 *   <li>{@code EntityPlayer} -> {@link Player} (mojmap).</li>
 *   <li>{@code @SideOnly(Side.CLIENT)} -> {@link OnlyIn @OnlyIn}{@code (Dist.CLIENT)}.</li>
 *   <li>{@code FMLCommonHandler.instance().getSide()} -> {@link FMLEnvironment#dist}.</li>
 *   <li>{@code World#provider#getDimension()} (int) -> {@link Level#dimension()}
 *       ({@code ResourceKey<Level>}). The {@code PHYSICS_WORLD_PER_DIMENSION} map is still keyed
 *       by int via {@code CommonProxy.dimensionKey(Level)} as a transitional shim.</li>
 *   <li>The {@code IPhysicsSimulationMode[]} indexed by {@code Side.ordinal()} now uses
 *       {@link LogicalSide#ordinal()} which has the same {@code CLIENT=0, SERVER=1} layout.</li>
 * </ul>
 */
public class DynamXContext {
    /**
     * -- GETTER --
     *
     * @return The collision handler for collisions between players and physics entities
     */
    @Getter
    private static final IRotatedCollisionHandler collisionHandler = new RotatedCollisionHandlerImpl();
    /**
     * -- GETTER --
     *
     * @return The current {@link IDnxNetworkSystem} for DynamX packets
     */
    @Getter
    private static IDnxNetworkSystem network;
    @OnlyIn(Dist.CLIENT)
    private static DynamXModelRegistry dxModelRegistry;

    /**
     * -- GETTER --
     *
     * @return The rigid bodies of all players
     */
    @Getter
    private static final Map<Player, PlayerPhysicsHandler> playerToCollision = new HashMap<>();
    /**
     * -- GETTER --
     *
     * @return The players walking on the top of entities
     */
    @Getter
    private static final ConcurrentHashMap<Player, PhysicsEntity<?>> walkingPlayers = new ConcurrentHashMap<>(0, 0.75f, 2);
    /**
     * -- GETTER --
     *
     * @return A map linking player ids with the entities they are holding
     */
    @Getter
    private static Map<Integer, Integer> playerPickingObjects = new HashMap<>();

    private static final IPhysicsSimulationMode[] physicsSimulationModes = new IPhysicsSimulationMode[]{
            new PhysicsSimulationModes.FullPhysics(), new PhysicsSimulationModes.FullPhysics()
    };

    private static final Map<ResourceLocation, DxModelData> DX_MODEL_DATA_CACHE = new HashMap<>();

    // TODO port:1.20.1 - keyed by ResourceKey<Level> instead of integer dim id.
    private static final Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, IPhysicsWorld> PHYSICS_WORLD_PER_DIMENSION = new HashMap<>();

    protected static void initNetwork() {
        // TODO port:1.20.1 - DynamXNetwork.init now takes a LogicalSide derived from FMLEnvironment.dist.
        LogicalSide side = FMLEnvironment.dist == Dist.CLIENT ? LogicalSide.CLIENT : LogicalSide.SERVER;
        network = DynamXNetwork.init(side);
    }

    @OnlyIn(Dist.CLIENT)
    public static void initObjModelRegistry() {
        dxModelRegistry = new DynamXModelRegistry();
    }

    /**
     * Use this to avoid manipulating physics on invalid sides
     *
     * @param world The Level to test
     * @return True is a {@link IPhysicsWorld} exists for this {@link Level} (depends on the side of the world) <br>
     * Always true except for client single player worlds
     */
    public static boolean usesPhysicsWorld(Level world) {
        return DynamXMain.proxy.shouldUseBulletSimulation(world);
    }

    /**
     * @return The local physics world
     */
    public static IPhysicsWorld getPhysicsWorld(Level world) {
        // TODO port:1.20.1 - now keyed by ResourceKey<Level>.
        return getPhysicsWorldPerDimensionMap().get(world.dimension());
    }

    /**
     * @return The obj model loader
     */
    @OnlyIn(Dist.CLIENT)
    public static DynamXModelRegistry getDxModelRegistry() {
        return dxModelRegistry;
    }

    // TODO port:1.20.1 - keyed by ResourceKey<Level> instead of integer dim id in 1.20.1.
    public static Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, IPhysicsWorld> getPhysicsWorldPerDimensionMap() {
        return PHYSICS_WORLD_PER_DIMENSION;
    }

    public static void setPlayerPickingObjects(Map<Integer, Integer> playerPickingObjects) {
        DynamXContext.playerPickingObjects = playerPickingObjects;
    }

    /**
     * @param side The side, server for the local physics simulation, client for the remote simulation world <br>
     *             Dist.CLIENT is <strong>only</strong> used on dedicated server for the client physics worlds
     * @return The {@link IPhysicsSimulationMode} on the given side
     */
    public static IPhysicsSimulationMode getPhysicsSimulationMode(LogicalSide side) {
        return physicsSimulationModes[side.ordinal()];
    }

    /**
     * @param side                  The side, server for the local physics simulation, client for the remote simulation world <br>
     *                              Dist.CLIENT is <strong>only</strong> used on dedicated server for the client physics worlds
     * @param physicsSimulationMode The {@link IPhysicsSimulationMode} to set on the given side
     */
    public static void setPhysicsSimulationMode(LogicalSide side, IPhysicsSimulationMode physicsSimulationMode) {
        DynamXContext.physicsSimulationModes[side.ordinal()] = physicsSimulationMode;
    }

    public static DxModelData getDxModelDataFromCache(DxModelPath modelPath) {
        if (DX_MODEL_DATA_CACHE.containsKey(modelPath.getModelPath())) {
            return DX_MODEL_DATA_CACHE.get(modelPath.getModelPath());
        } else {
            DxModelData objModelData = null;
            switch (modelPath.getFormat()) {
                case OBJ:
                    objModelData = new ObjModelData(modelPath);
                    break;
                case GLTF:
                    objModelData = new GltfModelData(modelPath);
                    break;
            }
            DX_MODEL_DATA_CACHE.put(modelPath.getModelPath(), objModelData);
            return objModelData;
        }
    }

    public static Map<ResourceLocation, DxModelData> getDxModelDataCache() {
        return DX_MODEL_DATA_CACHE;
    }

    /**
     * Tiny accessor so we can call the {@code protected} {@link CommonProxy#dimensionKey(Level)}
     * shim from this class. Once {@code PHYSICS_WORLD_PER_DIMENSION} is re-keyed to
     * {@code ResourceKey<Level>}, this can go.
     */
    /**
     * Tiny accessor exposing the package-protected {@link CommonProxy#dimensionKey(Level)} static
     * shim. Cannot use override because the original is static; a subclass is used purely for
     * package-visibility access until {@code PHYSICS_WORLD_PER_DIMENSION} is re-keyed.
     */
    private static final class CommonProxyDimensionAccessor extends CommonProxy {
        // TODO port:1.20.1 - kept only so this class lives in the right package; once
        // PHYSICS_WORLD_PER_DIMENSION is keyed by ResourceKey<Level>, drop this entirely.
        @Override
        public boolean ownsSimulation(PhysicsEntity<?> entity) {
            return false;
        }

        @Override
        public void scheduleTask(Level mcWorld, Runnable task) {
        }

        @Override
        public void schedulePacksInit() {
        }
    }
}
