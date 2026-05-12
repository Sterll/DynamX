package fr.dynamx.client;

import fr.aym.acsguis.api.ACsGuiApiService;
import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.ThreadedLoadingService;
import fr.aym.mps.utils.UserErrorMessageException;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.client.command.DynamXClientCommand;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.client.handlers.KeyHandler;
import fr.dynamx.client.network.ClientPhysicsEntitySynchronizer;
import fr.dynamx.client.sound.DynamXSoundHandler;
import fr.dynamx.common.CommonProxy;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.PhysicsEntitySynchronizer;
import fr.dynamx.common.network.sync.SPPhysicsEntitySynchronizer;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.common.physics.world.BuiltinThreadedPhysicsWorld;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.optimization.SubClassPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.LogicalSide;

/**
 * Client-side proxy.
 *
 * <p>TODO port:1.20.1 - massive surface change vs the 1.12 proxy:</p>
 * <ul>
 *   <li>{@code ISelectiveResourceReloadListener} / {@code IResourceType} / {@code VanillaResourceType} pipeline is
 *       gone; reload happens through {@code RegisterClientReloadListenersEvent}. Stubbed here.</li>
 *   <li>{@code RenderingRegistry.registerEntityRenderingHandler} is replaced by
 *       {@code EntityRenderersEvent.RegisterRenderers} on the mod bus. Stubbed.</li>
 *   <li>{@code ClientRegistry.bindTileEntitySpecialRenderer} -> {@code EntityRenderersEvent.RegisterRenderers}
 *       with {@code BlockEntityRenderers.register(...)}.</li>
 *   <li>{@code FMLClientHandler.instance().getClient()} -> {@code Minecraft.getInstance()}.</li>
 *   <li>{@code Side.CLIENT} -> {@code LogicalSide.CLIENT} / {@code Dist.CLIENT}.</li>
 *   <li>{@code FMLCommonHandler.instance().getMinecraftServerInstance()} ->
 *       {@code ServerLifecycleHooks.getCurrentServer()}.</li>
 *   <li>{@code Minecraft.getMinecraft().getFramebuffer().enableStencil()} -> {@code mainRenderTarget.enableStencil()}.</li>
 *   <li>{@code ClientCommandHandler.instance.registerCommand} replaced by {@code RegisterClientCommandsEvent}.</li>
 *   <li>{@code World.provider.getDimension()} -> {@code Level.dimension()} returning {@code ResourceKey<Level>}.</li>
 *   <li>{@code SplashProgress} class gone, removed pause/resume calls.</li>
 *   <li>{@code ModelLoaderRegistry.registerLoader} is replaced by {@code ModelEvent.RegisterGeometryLoaders} on the mod bus.</li>
 *   <li>{@code player.sendMessage(new TextComponentTranslation(...))} -> {@code player.sendSystemMessage(Component.translatable(...))}.</li>
 * </ul>
 */
public class ClientProxy extends CommonProxy {
    public static DynamXSoundHandler SOUND_HANDLER = new DynamXSoundHandler();

    public ClientProxy() {
        DynamXContext.initObjModelRegistry();
        // TODO port:1.20.1 - register the DxItemModel geometry loader via ModelEvent.RegisterGeometryLoaders.
    }

    @Override
    public void scheduleTask(Level mcWorld, Runnable task) {
        if (mcWorld.isClientSide) {
            Minecraft.getInstance().execute(task);
        } else if (mcWorld instanceof ServerLevel) {
            ((ServerLevel) mcWorld).getServer().execute(task);
        }
    }

    @Override
    public void preInit() {
        super.preInit();

        DynamXContext.getDxModelRegistry().onPackInfosReloaded();

        // TODO port:1.20.1 - move to EntityRenderersEvent.RegisterRenderers on the mod bus:
        //   event.registerEntityRenderer(DynamXEntities.CAR.get(), RenderBaseVehicle.RenderCar::new);
        //   event.registerEntityRenderer(DynamXEntities.BOAT.get(), RenderBaseVehicle.RenderBoat::new);
        //   event.registerEntityRenderer(DynamXEntities.TRAILER.get(), RenderBaseVehicle.RenderTrailer::new);
        //   event.registerEntityRenderer(DynamXEntities.HELICOPTER.get(), RenderBaseVehicle.RenderHelicopter::new);
        //   event.registerEntityRenderer(DynamXEntities.PROP.get(), RenderProp::new);
        //   event.registerEntityRenderer(DynamXEntities.DOOR.get(), RenderDoor::new);
        //   event.registerEntityRenderer(DynamXEntities.RAGDOLL.get(), RenderRagdoll::new);
        //   event.registerEntityRenderer(DynamXEntities.SEAT.get(), RenderSeatEntity::new);
        //
        // TODO port:1.20.1 - register reload listener through RegisterClientReloadListenersEvent. Stubbed.
    }

    @Override
    public void init() {
        super.init();

        // KeyMappings are registered via RegisterKeyMappingsEvent in DynamXClientRegistration.
        // The KeyHandler instance is subscribed here for per-tick key polling.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new KeyHandler(Minecraft.getInstance()));

        // TODO port:1.20.1 - DynamXClientCommand registration moves to RegisterClientCommandsEvent.
        //
        // TODO port:1.20.1 - TESRDynamXBlock registration via EntityRenderersEvent.RegisterRenderers.
        //
        // TODO port:1.20.1 - framebuffer stencil:
        //   if (!Minecraft.getInstance().getMainRenderTarget().isStencilEnabled())
        //       Minecraft.getInstance().getMainRenderTarget().enableStencil();
    }

    @Override
    public void completeInit() {
        super.completeInit();
        try {
            DynamXContext.getDxModelRegistry().uploadVAOs();
        } catch (Exception ignored) {
            // TODO port:1.20.1 - SplashProgress.pause/resume is gone; nothing to do.
        }
    }

    @Override
    public Level getClientWorld() {
        return Minecraft.getInstance().level;
    }

    @Override
    public Level getServerWorld() {
        // TODO port:1.20.1 - ServerLifecycleHooks.getCurrentServer() instead of FMLCommonHandler.
        if (net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() != null) {
            return net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().overworld();
        }
        return null;
    }

    @Override
    public boolean shouldUseBulletSimulation(Level world) {
        return super.shouldUseBulletSimulation(world) && world.isClientSide;
    }

    @Override
    public <T extends AbstractEntityPhysicsHandler<?, ?>> PhysicsEntitySynchronizer<? extends PhysicsEntity<T>> getNetHandlerForEntity(PhysicsEntity<T> tPhysicsEntity) {
        if (tPhysicsEntity.level().isClientSide) {
            if (net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() != null) {
                return new SPPhysicsEntitySynchronizer<>(tPhysicsEntity, LogicalSide.CLIENT);
            } else {
                return new ClientPhysicsEntitySynchronizer<>(tPhysicsEntity);
            }
        }
        return super.getNetHandlerForEntity(tPhysicsEntity);
    }

    @Override
    public int getTickTime() {
        if (Minecraft.getInstance().player != null) {
            return Minecraft.getInstance().player.tickCount;
        }
        return 0;
    }

    @Override
    public boolean ownsSimulation(PhysicsEntity<?> entity) {
        if (entity.getSynchronizer().getSimulationHolder().ownsPhysics(entity.level().isClientSide ? LogicalSide.CLIENT : LogicalSide.SERVER)) {
            return true;
        }
        if (entity.level().isClientSide && ClientEventHandler.MC.player != null && ClientEventHandler.MC.player.getVehicle() instanceof PhysicsEntity
                && ((PhysicsEntity<?>) ClientEventHandler.MC.player.getVehicle()).getSynchronizer().getSimulationHolder().ownsPhysics(LogicalSide.CLIENT)) {
            return true;
        }
        return ClientEventHandler.MC.player != null && DynamXContext.getPlayerPickingObjects().containsKey(ClientEventHandler.MC.player.getId()) &&
                DynamXContext.getPlayerPickingObjects().get(ClientEventHandler.MC.player.getId()) == entity.getId();
    }

    /**
     * <p>TODO port:1.20.1 - hook to RegisterClientReloadListenersEvent. Logic kept for the future caller.</p>
     */
    public void onResourceManagerReload() {
        DynamXRenderUtils.initGlMeshes();
        DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.CLIENT, DynamXLoadingTasks.MODEL).thenAccept(empty -> {
            if (Minecraft.getInstance().player != null && DynamXErrorManager.getErrorManager().hasErrors(DynamXErrorManager.MODEL_ERRORS))
                Minecraft.getInstance().player.sendSystemMessage(Component.translatable("dynamx.reload.models.errors"));
        });
    }

    @Override
    public void initPhysicsWorld(Level world) {
        if (DynamXContext.getPhysicsWorldPerDimensionMap().containsKey(world.dimension())) {
            DynamXMain.log.info("Duplicate world load detected. Are you using BungeeCoord ? Unloading old world.");
            IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(world);
            if (physicsWorld != null) {
                DynamXMain.log.debug("Clearing current physics world...");
                physicsWorld.clearAll();
                DynamXContext.getPlayerToCollision().clear();
            } else {
                throw new IllegalStateException("Physics world loaded but not found. Dim: " + world.dimension() + " World: " + world);
            }
        }
        DynamXContext.getPhysicsWorldPerDimensionMap().put(world.dimension(), new BuiltinThreadedPhysicsWorld(world, !Minecraft.getInstance().hasSingleplayerServer()));
    }

    private byte loadingState;

    @Override
    public void schedulePacksInit() {
        try {
            // TODO port:1.20.1 - DefaultArtifactVersion/VersionRange types changed (Maven Aether vs FML), keeping a simple
            // string compare for now. Replace with proper version-range parsing once we wire up the new framework.
            ACsGuiApiService service = ACsLib.getPlatform().provideService(ACsGuiApiService.class);
            if (service == null) {
                DynamXMain.log.fatal("ACsGuis not found. Halting game loading at pre init.");
                DynamXMain.memoizedConstructionError = new UserErrorMessageException("Missing ACsGuis", null,
                        "Missing ACsGuis",
                        "This version of DynamX requires ACsGuis.",
                        "We advise you to install version " + DynamXConstants.DEFAULT_ACSGUIS_VERSION + " of ACsGuis.");
                return;
            }
        } catch (Exception e) {
            throw new RuntimeException("Bad ACSGUIS_REQUIRED_VERSION", e);
        }

        // TODO port:1.20.1 - register ClientEventHandler on MinecraftForge.EVENT_BUS in the mod constructor.
        // TODO port:1.20.1 - reload listener registration moves to RegisterClientReloadListenersEvent.
        // The state-machine loading logic is kept inline as a lambda for the future hook.
        Runnable scheduledLoad = () -> {
            if (loadingState == 0) {
                loadingState++;
            } else if (loadingState == 1) {
                loadingState++;
                ThreadedLoadingService loadingService = ACsLib.getPlatform().provideService(ThreadedLoadingService.class);
                loadingService.addTask(ThreadedLoadingService.ModLoadingSteps.BLOCK_REGISTRY, "packsload", () -> {
                    Vector3fPool.openPool(SubClassPool.PACK_MODEL_LOAD);
                    DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.MC_INIT, DynamXLoadingTasks.PACK);
                    Vector3fPool.closePool();

                    loadingService.addTask(ThreadedLoadingService.ModLoadingSteps.INIT, "proxy preinit", this::preInit);
                });
            }
        };
        scheduledLoad.run();
    }
}
