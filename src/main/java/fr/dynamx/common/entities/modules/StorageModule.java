package fr.dynamx.common.entities.modules;

import fr.dynamx.api.blocks.IBlockEntityModule;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.parts.PartStorage;
import fr.dynamx.common.entities.IDynamXObject;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.physics.entities.PackEntityPhysicsHandler;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Inventory module for vehicles or DynamX blocks.
 */
// TODO port:1.20.1 - InventoryBasic -> SimpleContainer; TEDynamXBlock & PartStorage forward-referenced.
@Getter
public class StorageModule implements IPhysicsModule<PackEntityPhysicsHandler<?, ?>>, IBlockEntityModule, IPackInfoReloadListener {
    private final IDynamXObject owner;
    private final Map<Byte, SimpleContainer> inventories = new HashMap<>();

    public StorageModule(PackPhysicsEntity<?, ?> entity, PartStorage<?> partStorage) {
        this.owner = entity;
        addInventory(entity, partStorage);
    }

    public StorageModule(TEDynamXBlock block, BlockPos pos, PartStorage<?> partStorage) {
        this.owner = block;
        addInventory(block, pos, partStorage);
    }

    public void addInventory(PackPhysicsEntity<?, ?> entity, PartStorage<?> partStorage) {
        addInventory(player -> !entity.isRemoved() && entity.distanceToSqr(player) <= 256, partStorage);
    }

    public void addInventory(TEDynamXBlock block, BlockPos pos, PartStorage<?> partStorage) {
        addInventory(player -> player.level().getBlockEntity(pos) == block &&
                player.distanceToSqr((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D) <= 64.0D, partStorage);
    }

    public void addInventory(Predicate<Player> usagePredicate, PartStorage<?> partStorage) {
        inventories.put(partStorage.getId(), new SimpleContainer(partStorage.getStorageSize()) {
            @Override
            public boolean stillValid(Player player) {
                return usagePredicate.test(player);
            }
        });
    }

    public Container getInventory(byte id) {
        return inventories.get(id);
    }

    @Override
    public void getBlockDrops(NonNullList<ItemStack> drops, BlockGetter world, BlockPos pos, BlockState state, int fortune) {
        for (SimpleContainer inventory : inventories.values()) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    drops.add(stack);
                }
            }
        }
    }

    @Override
    public void onSetDead() {
        if (!(owner instanceof Entity) || ((Entity) owner).level().isClientSide)
            return;
        for (SimpleContainer inventory : inventories.values()) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    ((Entity) owner).spawnAtLocation(stack, 0.5F);
                }
            }
        }
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        int j = 0;
        for (Map.Entry<Byte, SimpleContainer> inventoryBasic : inventories.entrySet()) {
            ListTag list = new ListTag();
            for (int i = 0; i < inventoryBasic.getValue().getContainerSize(); i++) {
                // TODO port:1.20.1 - ItemStack#writeToNBT replaced by ItemStack#save (mutates a passed tag).
                list.add(inventoryBasic.getValue().getItem(i).save(new CompoundTag()));
            }
            tag.put("StorageInv" + j, list);
            j++;
        }
        tag.putInt("StorageCount", j);
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        if (tag.contains("StorageInv", Tag.TAG_LIST)) {
            ListTag list = tag.getList("StorageInv", Tag.TAG_COMPOUND);
            SimpleContainer inventory = inventories.get((byte) 0);
            for (int i = 0; i < Math.min(inventory.getContainerSize(), list.size()); i++) {
                inventory.setItem(i, ItemStack.of(list.getCompound(i)));
            }
        }
        for (int j = 0; j < Math.min(tag.getInt("StorageCount"), inventories.size()); j++) {
            ListTag list = tag.getList("StorageInv" + j, Tag.TAG_COMPOUND);
            SimpleContainer inventory = inventories.get((byte) j);
            for (int i = 0; i < Math.min(inventory.getContainerSize(), list.size()); i++) {
                inventory.setItem(i, ItemStack.of(list.getCompound(i)));
            }
        }
    }

    @Override
    public void onPackInfosReloaded() {
        List<PartStorage> storageParts = getOwner().getPackInfo().getPartsByType(PartStorage.class);
        if (storageParts.size() != inventories.size()) {
            Map<Byte, SimpleContainer> newInventories = new HashMap<>();
            for (PartStorage<?> partStorage : storageParts) {
                if (!inventories.containsKey(partStorage.getId())) {
                    addInventory(getOwner() instanceof PackPhysicsEntity<?, ?> ? (PackPhysicsEntity<?, ?>) getOwner() : null, partStorage);
                }
                newInventories.put(partStorage.getId(), inventories.get(partStorage.getId()));
            }
            inventories.clear();
            inventories.putAll(newInventories);
        }
    }
}
