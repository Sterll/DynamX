package fr.dynamx.client;

import fr.dynamx.client.command.DynamXClientCommand;
import fr.dynamx.client.handlers.KeyHandler;
import fr.dynamx.client.renders.RenderProp;
import fr.dynamx.client.renders.RenderRagdoll;
import fr.dynamx.client.renders.RenderSeatEntity;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.client.renders.vehicle.RenderDoor;
import fr.dynamx.common.core.DynamXEntities;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Client-side wiring of mod-bus events that don't fit in {@link ClientProxy}'s
 * lifecycle hooks. Notably: {@link EntityRenderersEvent.RegisterRenderers} and
 * {@link RegisterKeyMappingsEvent} both require the mod bus, only accessible
 * from the mod constructor.
 */
public final class DynamXClientRegistration {
    private DynamXClientRegistration() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(DynamXClientRegistration::onRegisterRenderers);
        modBus.addListener(DynamXClientRegistration::onRegisterKeyMappings);
        MinecraftForge.EVENT_BUS.addListener(DynamXClientRegistration::onRegisterClientCommands);
    }

    private static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        new DynamXClientCommand().register(event.getDispatcher());
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DynamXEntities.CAR.get(), RenderBaseVehicle.RenderCar::new);
        event.registerEntityRenderer(DynamXEntities.BOAT.get(), RenderBaseVehicle.RenderBoat::new);
        event.registerEntityRenderer(DynamXEntities.TRAILER.get(), RenderBaseVehicle.RenderTrailer::new);
        event.registerEntityRenderer(DynamXEntities.HELICOPTER.get(), RenderBaseVehicle.RenderHelicopter::new);
        event.registerEntityRenderer(DynamXEntities.PROP.get(), RenderProp::new);
        event.registerEntityRenderer(DynamXEntities.DOOR.get(), RenderDoor::new);
        event.registerEntityRenderer(DynamXEntities.RAGDOLL.get(), RenderRagdoll::new);
        event.registerEntityRenderer(DynamXEntities.SEAT.get(), RenderSeatEntity::new);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (net.minecraft.client.KeyMapping mapping : KeyHandler.ALL_KEYS) {
            event.register(mapping);
        }
    }
}
