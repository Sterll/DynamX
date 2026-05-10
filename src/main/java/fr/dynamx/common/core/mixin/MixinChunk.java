package fr.dynamx.common.core.mixin;

import fr.dynamx.utils.DynamXConstants;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Patches the world raytrace to raytrace on dynamx blocks (1.12 ASM target: {@code Chunk} -> 1.20.1 {@link LevelChunk}).
 *
 * <p>TODO port:1.20.1 - The legacy mixin {@code @Overwrite}-d {@code setBlockState(BlockPos, IBlockState)}
 * to fire {@code CommonEventHandler.onBlockChange}. In 1.20.1 the method is
 * {@code LevelChunk.setBlockState(BlockPos, BlockState, boolean)} and its internal flow is very
 * different (no more {@code ExtendedBlockStorage}, light is driven by {@code LevelLightEngine},
 * the old {@code Chunk.NULL_BLOCK_STORAGE} field is gone, etc.). Doing a wholesale {@code @Overwrite}
 * here would be brittle; the hook needs to be re-expressed as a much smaller {@code @Inject} once
 * we know what we want to observe. For now the class is kept as a placeholder mixin so the build
 * tooling still picks up the mixin reference; the injection body is empty.
 */
@Mixin(value = LevelChunk.class, priority = 500, remap = DynamXConstants.REMAP)
public abstract class MixinChunk {
    // TODO port:1.20.1 - resolve mojmap target for the setBlockState event hook and re-inject.
}
