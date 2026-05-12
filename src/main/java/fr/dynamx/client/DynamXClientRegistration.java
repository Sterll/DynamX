package fr.dynamx.client;

import fr.dynamx.client.renders.RenderProp;
import fr.dynamx.client.renders.RenderRagdoll;
import fr.dynamx.client.renders.RenderSeatEntity;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.client.renders.vehicle.RenderDoor;
import fr.dynamx.common.core.DynamXEntities;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Client-side wiring of mod-bus events that don't fit in {@link ClientProxy}'s
 * lifecycle hooks. Notably: {@link EntityRenderersEvent.RegisterRenderers} requires
 * the mod bus, which is only accessible from the mod constructor.
 */
public final class DynamXClientRegistration {
    private DynamXClientRegistration() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(DynamXClientRegistration::onRegisterRenderers);
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
}
