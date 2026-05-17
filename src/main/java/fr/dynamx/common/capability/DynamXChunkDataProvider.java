package fr.dynamx.common.capability;

import fr.dynamx.utils.DynamXConstants;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Forge 1.20.1 capability provider for {@link DynamXChunkData}.
 *
 * In 1.12 this was a plain {@code ICapabilitySerializable<NBTBase>} attached to chunks via
 * {@code AttachCapabilitiesEvent<Chunk>} and registered through {@code CapabilityManager.INSTANCE
 * .register}. The Forge 1.20.1 equivalent is {@link RegisterCapabilitiesEvent#register(Class)}
 * plus an {@link AttachCapabilitiesEvent}{@code <LevelChunk>} listener; the {@link CapabilityToken}
 * pattern replaces the deprecated {@code @CapabilityInject} annotation.
 */
public class DynamXChunkDataProvider implements ICapabilitySerializable<CompoundTag> {

    /**
     * The capability handle. Resolved through {@link CapabilityToken} (the ASM transformer fills in
     * the generic type at runtime), and the actual registration happens in
     * {@link #onRegisterCapabilities}.
     */
    public static final Capability<DynamXChunkData> DYNAMX_CHUNK_DATA_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<DynamXChunkData>() {});

    /**
     * Resource id used as the AttachCapabilitiesEvent key for chunks.
     */
    public static final ResourceLocation CAPABILITY_LOCATION = new ResourceLocation(DynamXConstants.ID, "chunkaabb");

    private final DynamXChunkData data = new DynamXChunkData();
    private final LazyOptional<DynamXChunkData> handle = LazyOptional.of(() -> data);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == DYNAMX_CHUNK_DATA_CAPABILITY ? handle.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return DynamXChunkDataStorage.writeNBT(data);
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        DynamXChunkDataStorage.readNBT(data, nbt);
    }

    /**
     * Helper accessor: returns the {@link DynamXChunkData} for the given chunk, or {@code null}
     * if the capability is missing (e.g. before chunk attach finished).
     */
    @Nullable
    public static DynamXChunkData get(LevelChunk chunk) {
        if (chunk == null) return null;
        return chunk.getCapability(DYNAMX_CHUNK_DATA_CAPABILITY).orElse(null);
    }

    /**
     * Bridge helper preserving the 1.12 {@code chunk.getCapability(CAP, side)} signature.
     */
    @Nullable
    public static DynamXChunkData get(LevelChunk chunk, Direction side) {
        if (chunk == null) return null;
        return chunk.getCapability(DYNAMX_CHUNK_DATA_CAPABILITY, side).orElse(null);
    }

    /**
     * Wires the capability into Forge: registration on the mod event bus and chunk-attachment on
     * the main Forge bus.
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(DynamXChunkDataProvider::onRegisterCapabilities);
        MinecraftForge.EVENT_BUS.addGenericListener(LevelChunk.class, DynamXChunkDataProvider::onAttach);
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(DynamXChunkData.class);
    }

    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<LevelChunk> event) {
        event.addCapability(CAPABILITY_LOCATION, new DynamXChunkDataProvider());
    }
}
