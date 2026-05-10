package fr.dynamx.common.core.mixin;

import fr.dynamx.utils.DynamXConstants;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Patches the world raytrace to also raytrace against DynamX block AABBs stored in the
 * per-{@link net.minecraft.world.level.chunk.LevelChunk LevelChunk} attachment
 * (1.12 ASM target: {@code World} -> 1.20.1 {@link Level}).
 *
 * <p>TODO port:1.20.1 - The 1.12 mixin {@code @Overwrite}-d
 * {@code World#rayTraceBlocks(Vec3d, Vec3d, boolean, boolean, boolean)} to walk the voxel grid
 * itself and, at each step, query the DynamX chunk-capability for extra {@code AxisAlignedBB}
 * boxes to intercept. In 1.20.1 the entire raytrace surface has been re-shaped:
 * <ul>
 *   <li>{@code World.rayTraceBlocks(Vec3d, Vec3d, ...)} is now
 *       {@code Level.clip(ClipContext)} which returns {@code BlockHitResult}.</li>
 *   <li>{@code Vec3d} -> {@code Vec3}, {@code AxisAlignedBB} -> {@code AABB},
 *       {@code RayTraceResult} -> {@code HitResult} / {@code BlockHitResult}.</li>
 *   <li>{@code BlockState#collisionRayTrace} is gone — collision is driven by
 *       {@code VoxelShape}s ({@code BlockBehaviour.BlockStateBase#getCollisionShape}) and
 *       the actual walk is delegated to
 *       {@code BlockGetter#traverseBlocks(Vec3, Vec3, T, BiFunction, Function)}.</li>
 *   <li>The capability {@code DynamXChunkDataProvider.DYNAMX_CHUNK_DATA_CAPABILITY} migrated to
 *       a NeoForge {@code AttachmentType}, with a different lookup API.</li>
 * </ul>
 *
 * <p>Wholesale {@code @Overwrite} of {@code Level#clip} is brittle (vanilla, Forge events, and
 * various mods all hook here); the right move is a {@code @ModifyReturnValue} or
 * {@code @Inject(at = RETURN)} that compares the vanilla {@code BlockHitResult} against
 * DynamX's own raytrace and returns the closer of the two. Body stubbed; {@code @Mixin}
 * target preserved so the mixin config still binds.
 */
@Mixin(value = Level.class, remap = DynamXConstants.REMAP)
public abstract class MixinWorld {
    // TODO port:1.20.1 - re-express the DynamX-aware raytrace as a non-destructive @Inject
    //   on Level#clip(ClipContext) using the new AttachmentType lookup.
}
