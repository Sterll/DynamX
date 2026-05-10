package fr.dynamx.common.items.tools;

import com.jme3.math.Vector3f;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * TODO port:1.20.1 - Slopes tool:
 *  - {@code IBlockState} -> {@link BlockState}.
 *  - {@code AxisAlignedBB} -> {@link AABB}; collision boxes come from {@code BlockState#getCollisionShape(...)}
 *    (a {@code VoxelShape}) — {@link VoxelShape#bounds()} returns an AABB.
 *  - {@code new BlockPos(double, double, double)} -> {@link BlockPos#containing(double, double, double)}.
 *  - {@code stack.hasTagCompound() / getTagCompound() / setTagCompound()} -> custom CompoundTag helpers via the new
 *    data-component / NBT bridge. We use the legacy {@code Tag} field "tag" through reflection-light helpers for now;
 *    keep the same call shape by using {@link ItemStack#getOrCreateTag()} and {@link ItemStack#getTag()}.
 *  - {@code NBTTagCompound} -> {@link CompoundTag}; {@code NBTTagList} -> {@link ListTag}.
 *  - {@code NBTTagList#getCompoundTagAt(int)} -> {@link ListTag#getCompound(int)}.
 *  - {@code NBTTagList#tagCount()} -> {@link ListTag#size()}.
 *  - {@code NBTTagList#appendTag} -> {@link ListTag#add(Tag)}.
 *  - {@code NBTTagList#removeTag} -> {@link ListTag#remove(int)}.
 *  - {@code TextComponentTranslation} -> {@code Component.translatable(...)} and
 *    {@code player.sendMessage(...)} -> {@link Player#displayClientMessage(Component, boolean)}.
 *  - {@code TAG_COMPOUND} numeric id 10 unchanged.
 */
public class ItemSlopes extends Item {
    public ItemSlopes() {
        super(new Properties());
        // TODO port:1.20.1 - setRegistryName / setTranslationKey / setCreativeTab removed.
        DynamXItemRegistry.add(this);
    }

    public void clearMemory(Level worldIn, Player playerIn, ItemStack stack) {
        CompoundTag nbt = stack.getOrCreateTag();
        if (nbt.getInt("mode") == 1) { // Create
            nbt.remove("plist");
            if (worldIn.isClientSide)
                playerIn.displayClientMessage(Component.translatable("slopes.clear.create"), false);
        } else if (nbt.getInt("mode") == 0) { // Delete
            nbt.remove("p1");
            nbt.remove("p2");
            if (worldIn.isClientSide)
                playerIn.displayClientMessage(Component.translatable("slopes.clear.delete"), false);
        } else if (nbt.getInt("mode") == 2) { // Auto
            nbt.remove("pt1");
            nbt.remove("pt2");
            nbt.remove("ptface");
            nbt.remove("ptround");
            if (worldIn.isClientSide)
                playerIn.displayClientMessage(Component.translatable("slopes.clear.auto"), false);
        }
    }

    public static Vector3f fixPos(Level worldIn, Vec3 post) {
        Vector3f pos = Vector3fPool.get(DynamXMath.preciseRound(post.x), DynamXMath.preciseRound(post.y), DynamXMath.preciseRound(post.z));
        BlockPos bpos = BlockPos.containing(post.x, post.y, post.z);
        BlockState state = worldIn.getBlockState(bpos);
        // TODO port:1.20.1 - IBlockState#getCollisionBoundingBox replaced by BlockState#getCollisionShape(...).bounds().
        //  When the shape is empty, fall back to 0 like the legacy code did when box was null.
        VoxelShape shape = state.getCollisionShape(worldIn, bpos);
        double maxY = shape.isEmpty() ? 0 : shape.bounds().maxY;
        pos.y = (float) (bpos.getY() + maxY);
        return pos;
    }

    public void clickedWith(Level worldIn, Player playerIn, InteractionHand handIn, Vector3f pos) {
        if (handIn != InteractionHand.MAIN_HAND)
            return;

        ItemStack s = playerIn.getItemInHand(handIn);
        CompoundTag nbt = s.getOrCreateTag();
        if (nbt.getInt("mode") == 1) { // Create
            if (!nbt.contains("plist"))
                nbt.put("plist", new ListTag());
            ListTag list = nbt.getList("plist", Tag.TAG_COMPOUND);
            boolean set = false;
            for (int i = 0; i < list.size(); i++) {
                Vector3f other = getPosFromTag(list.getCompound(i));
                if (other.equals(pos)) {
                    set = true;
                    list.remove(i);
                    break;
                }
            }
            if (!set) {
                list.add(createPosTag(pos));
            }
        } else if (nbt.getInt("mode") == 0) { // Delete
            if (!nbt.contains("p1")) {
                nbt.remove("p2");
                nbt.put("p1", createPosTag(pos));
            } else {
                if (pos.equals(getPosFromTag(nbt.getCompound("p1")))) {
                    if (!nbt.contains("p2"))
                        nbt.remove("p1");
                    else {
                        nbt.put("p1", nbt.getCompound("p2"));
                        nbt.remove("p2");
                    }
                } else
                    nbt.put("p2", createPosTag(pos));
            }
        } else if (nbt.getInt("mode") == 2) { // Auto
            if (!nbt.contains("pt1")) {
                nbt.remove("pt2");
                nbt.put("pt1", createPosTag(pos));
            } else {
                if (pos.equals(getPosFromTag(nbt.getCompound("pt1")))) {
                    if (!nbt.contains("pt2"))
                        nbt.remove("pt1");
                    else {
                        nbt.put("pt1", nbt.getCompound("pt2"));
                        nbt.remove("pt2");
                    }
                } else
                    nbt.put("pt2", createPosTag(pos));
            }
        }
    }

    public static CompoundTag createPosTag(Vector3f pos) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("X", pos.x);
        tag.putFloat("Y", pos.y);
        tag.putFloat("Z", pos.z);
        return tag;
    }

    public static Vector3f getPosFromTag(CompoundTag tag) {
        return Vector3fPool.get(tag.getFloat("X"), tag.getFloat("Y"), tag.getFloat("Z"));
    }
}
