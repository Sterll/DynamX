package fr.dynamx.common.slopes;

import fr.aym.acslib.utils.DeserializedData;
import fr.aym.acslib.utils.nbtserializer.ISerializable;
import fr.aym.acslib.utils.nbtserializer.NBTSerializer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class SlopeBuildingConfig implements ISerializable {
    private int version;
    private Direction facing = Direction.DOWN;
    private int diagDir;
    private boolean enableSlabs;
    private final List<Block> blackList = new ArrayList<>();

    public SlopeBuildingConfig() {
    }

    public SlopeBuildingConfig(CompoundTag from) {
        if (!from.isEmpty()) {
            try {
                NBTSerializer.deserialize(from, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setEnableSlabs(boolean enableSlabs) {
        this.enableSlabs = enableSlabs;
        version++;
    }

    public boolean isEnableSlabs() {
        return enableSlabs;
    }

    public void refresh() {
        version++;
    }

    public int getConfigVersion() {
        return version;
    }

    public int getDiagDir() {
        if (diagDir != -1 && diagDir != 1)
            diagDir = 1;
        return diagDir;
    }

    public void setDiagDir(int diagDir) {
        this.diagDir = diagDir;
        version++;
    }

    public void setFacing(Direction facing) {
        this.facing = facing;
        version++;
    }

    public Direction getFacing() {
        return facing;
    }

    public List<Block> getBlackList() {
        return blackList;
    }

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public Object[] getObjectsToSave() {
        List<String> auBlack = new ArrayList<>();
        for (Block black : blackList) {
            // TODO port:1.20.1 - Block.REGISTRY removed; use BuiltInRegistries.BLOCK.getKey(block) when registries port lands.
            ResourceLocation key = null;
            auBlack.add(key != null ? key.toString() : "minecraft:air");
        }
        return new Object[]{version, facing.ordinal(), diagDir, enableSlabs, auBlack};
    }

    @Override
    public void populateWithSavedObjects(DeserializedData objects) {
        version = objects.next();
        int facingOrd = objects.next();
        // Direction.from3DDataValue replaces EnumFacing.byIndex
        facing = Direction.from3DDataValue(facingOrd);
        diagDir = objects.next();
        enableSlabs = NBTSerializer.convert(objects.next());
        blackList.clear();
        List<String> auBlack = objects.next();
        for (String loc : auBlack) {
            // TODO port:1.20.1 - BuiltInRegistries.BLOCK.get(new ResourceLocation(loc)) once registries are ported
            blackList.add(null);
        }
    }

    public boolean isValidBlock(BlockState block) {
        if (blackList.contains(block.getBlock()))
            return false;
        // BlockSlab -> SlabBlock; no public "isDouble()" anymore — slabs always count as one block.
        return enableSlabs || !(block.getBlock() instanceof SlabBlock);
    }

    public CompoundTag serialize() {
        return (CompoundTag) NBTSerializer.serialize(this);
    }
}
