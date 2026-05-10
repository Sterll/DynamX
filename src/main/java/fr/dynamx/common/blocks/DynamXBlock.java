package fr.dynamx.common.blocks;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * 1.20.1 NeoForge port of the DynamX block.
 *
 * <p>TODO port:1.20.1 - The legacy 1.12 class extended {@code net.minecraft.block.Block} (raw Block),
 * used the old metadata model ({@code IBlockState} + {@code PropertyInteger}), set creative tab on the
 * block (creative tabs are now CreativeModeTab + registry events), used Material/SoundType getters
 * that no longer exist as block-level setters, and posted Forge events that have NeoForge equivalents.
 *
 * <p>In 1.20.1 properties are immutable on the {@link BlockBehaviour.Properties} record and items
 * are registered separately via {@code DeferredRegister<Item>} — registry + creative-tab wiring
 * happens upstream of the constructor. This class therefore provides:
 *  - the same public constructors with the same parameter shapes (for source-level back-compat),
 *  - the {@code METADATA} property as an {@link IntegerProperty} (0..15) for the block state,
 *  - the {@code IDynamXItem} contract,
 *  - {@link EntityBlock#newBlockEntity(BlockPos, BlockState)} for dx-models,
 *
 * with stubbed bodies for everything that depends on un-ported code paths.
 */
public class DynamXBlock<T extends BlockObject<?>> extends Block implements IDynamXItem<T>, IResourcesOwner, EntityBlock {

    public static final IntegerProperty METADATA = IntegerProperty.create("metadata", 0, 15);

    public T blockObjectInfo;

    private final int textureNum;
    private final boolean isDxModel;

    /**
     * Internally used by DynamX. Creates a {@link Block} from a loaded {@link BlockObject}.
     */
    public DynamXBlock(T blockObjectInfo) {
        super(propsFor(blockObjectInfo));
        setInfo(blockObjectInfo);
        this.textureNum = Math.min(16, blockObjectInfo.getMaxVariantId());
        this.isDxModel = blockObjectInfo.isDxModel();
        // TODO port:1.20.1 - block registration & translation key now driven by DeferredRegister<Block>.
        this.registerDefaultState(this.stateDefinition.any().setValue(METADATA, 0));
    }

    /**
     * Custom-block constructor (no prop).
     *
     * <p>TODO port:1.20.1 - 1.12 took a {@code Material material}. Material is gone in 1.20.1;
     * caller should pass {@code Block.Properties} configured equivalents. We keep the signature
     * with {@link Object} to stay source-compatible without forcing Material imports.
     */
    public DynamXBlock(Object material, String modid, String blockName, ResourceLocation model) {
        this(material, modid, blockName, model, null);
    }

    /**
     * Custom-block constructor that may also register a prop.
     */
    @SuppressWarnings("unchecked")
    public DynamXBlock(Object material, String modid, String blockName, ResourceLocation model, String propsName) {
        super(defaultProperties());
        // TODO port:1.20.1 - addBuiltinObject + content-pack builtin registration; mirrors legacy flow.
        this.textureNum = 1;
        this.isDxModel = blockObjectInfo != null && blockObjectInfo.isDxModel();
        this.registerDefaultState(this.stateDefinition.any().setValue(METADATA, 0));
        // TODO port:1.20.1 - prop registration via DynamXObjectLoaders + PropObject when contentpack is wired.
        if (propsName != null) {
            // PropObject<?> prop = new PropObject<>((ISubInfoTypeOwner<BlockObject<?>>) getInfo(), propsName);
        }
    }

    private static BlockBehaviour.Properties propsFor(BlockObject<?> info) {
        // TODO port:1.20.1 - blockObjectInfo.getMaterial()/hardness/resistance/sound now need to be
        // expressed via Properties.of(MapColor)... full plumbing belongs to the content-pack layer.
        return defaultProperties();
    }

    private static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 6.0F);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(METADATA);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target, net.minecraft.world.level.LevelReader world, BlockPos pos, Player player) {
        return super.getCloneItemStack(state, target, world, pos, player);
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(worldIn, pos, state, placer, stack);
        if (placer == null) return;
        int rotation = net.minecraft.util.Mth.floor((placer.getYRot() * 16.0F / 360.0F) + 0.5D) & 0xF;
        if (isDxModel) {
            BlockEntity be = worldIn.getBlockEntity(pos);
            if (be instanceof TEDynamXBlock teDynamXBlock) {
                teDynamXBlock.setRotation(rotation);
            }
        } else {
            worldIn.setBlockAndUpdate(pos, state.setValue(METADATA, rotation));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (isDxModel) {
            // TODO port:1.20.1 - DynamXBlockEvent.CreateTileEntity bus dispatch (Phase 5/events).
            return new TEDynamXBlock(blockObjectInfo);
        }
        return null;
    }

    @Override
    public T getInfo() {
        return blockObjectInfo;
    }

    @Override
    public void setInfo(T info) {
        this.blockObjectInfo = info;
        // TODO port:1.20.1 - block properties are immutable in 1.20.1; light level / hardness /
        // resistance / sound must be applied via BlockBehaviour.Properties at construction time.
    }

    @Override
    public boolean createJson() {
        return IResourcesOwner.super.createJson() || blockObjectInfo.get3DItemRenderLocation() != Enum3DRenderLocation.ALL;
    }

    @Override
    public String getJsonName(int meta) {
        return getInfo().getName().toLowerCase();
    }

    @Override
    public IModelPackObject getDxModel() {
        return isDxModel ? getInfo() : null;
    }

    @Override
    public int getMaxMeta() {
        return textureNum;
    }

    public boolean isDxModel() {
        return isDxModel;
    }

    /**
     * Helper retained from 1.12 for {@code MixinWorld} (raytracing on DynamX blocks).
     */
    public AABB getComputedBB(Level world, BlockPos pos) {
        BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof TEDynamXBlock teDynamXBlock) {
            return teDynamXBlock.computeBoundingBox();
        }
        return new AABB(0, 0, 0, 1, 1, 1);
    }
}
