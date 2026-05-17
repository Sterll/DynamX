package fr.dynamx.common.contentpack.parts;

import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.StorageModule;
import fr.dynamx.utils.DynamXConstants;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.network.NetworkHooks;

/**
 * Storage part of a pack object.
 *
 * TODO port:1.20.1 - postLoad lang injection still needs ContentPackUtils + FMLEnvironment.dist.isClient() port.
 */
@Getter
@Setter
@RegisteredSubInfoType(name = "storage", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS}, strictName = false)
public class PartStorage<T extends ISubInfoTypeOwner<T>> extends InteractivePart<Object, T> {
    @PackFileProperty(configNames = "StorageSize")
    protected int storageSize;

    public PartStorage(T owner, String partName) {
        super(owner, partName, 0.5f, 0.5f);
    }

    @Override
    public void appendTo(T owner) {
        if (storageSize % 9 != 0)
            throw new IllegalArgumentException("StorageSize must be a multiple of 9 !");
        super.appendTo(owner);
    }

    @Override
    public void addModules(Object entity, Object modules) {
        if (!(modules instanceof ModuleListBuilder) || !(entity instanceof PackPhysicsEntity)) return;
        ModuleListBuilder list = (ModuleListBuilder) modules;
        PackPhysicsEntity<?, ?> packEntity = (PackPhysicsEntity<?, ?>) entity;
        if (!list.hasModuleOfClass(StorageModule.class)) {
            list.add(new StorageModule(packEntity, this));
        } else {
            list.getByClass(StorageModule.class).addInventory(packEntity, this);
        }
    }

    @Override
    public void addBlockModules(Object blockEntity, Object modules) {
        if (!(modules instanceof ModuleListBuilder) || !(blockEntity instanceof TEDynamXBlock)) return;
        ModuleListBuilder list = (ModuleListBuilder) modules;
        TEDynamXBlock block = (TEDynamXBlock) blockEntity;
        if (!list.hasModuleOfClass(StorageModule.class)) {
            list.add(new StorageModule(block, block.getBlockPos(), this));
        } else {
            list.getByClass(StorageModule.class).addInventory(block, block.getBlockPos(), this);
        }
    }

    @Override
    public ResourceLocation getHudCursorTexture() {
        return new ResourceLocation(DynamXConstants.ID, "textures/storage.png");
    }

    @Override
    public boolean interact(Object entity, Player player) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer))
            return false;
        Container inventory = resolveInventory(entity);
        if (inventory == null)
            return false;
        int rows = Math.max(1, Math.min(6, storageSize / 9));
        MenuProvider provider = new SimpleMenuProvider(
                (containerId, playerInv, p) -> createChestMenu(rows, containerId, playerInv, inventory),
                Component.literal(getPartName())
        );
        NetworkHooks.openScreen((ServerPlayer) player, provider);
        return true;
    }

    private Container resolveInventory(Object entity) {
        if (entity instanceof PackPhysicsEntity) {
            PackPhysicsEntity<?, ?> packEntity = (PackPhysicsEntity<?, ?>) entity;
            return packEntity.hasModuleOfType(StorageModule.class)
                    ? packEntity.getModuleByType(StorageModule.class).getInventory(getId())
                    : null;
        }
        if (entity instanceof TEDynamXBlock) {
            TEDynamXBlock block = (TEDynamXBlock) entity;
            return block.hasModuleOfType(StorageModule.class)
                    ? block.getModuleByType(StorageModule.class).getInventory(getId())
                    : null;
        }
        return null;
    }

    private static AbstractContainerMenu createChestMenu(int rows, int containerId, Inventory playerInv, Container inv) {
        MenuType<ChestMenu> type;
        switch (rows) {
            case 1:
                type = MenuType.GENERIC_9x1;
                break;
            case 2:
                type = MenuType.GENERIC_9x2;
                break;
            case 3:
                type = MenuType.GENERIC_9x3;
                break;
            case 4:
                type = MenuType.GENERIC_9x4;
                break;
            case 5:
                type = MenuType.GENERIC_9x5;
                break;
            default:
                type = MenuType.GENERIC_9x6;
                break;
        }
        return new ChestMenu(type, containerId, playerInv, inv, rows);
    }

    @Override
    public Object getDebugOption() {
        // TODO port:1.20.1 - return DynamXDebugOptions.SEATS_AND_STORAGE once the debug package is ported.
        return null;
    }

    @Override
    public String getName() {
        return "PartStorage named " + getPartName();
    }

    @Override
    public void postLoad(T owner, boolean hot) {
        super.postLoad(owner, hot);
        // TODO port:1.20.1 - Original added a missing lang translation via ContentPackUtils
        //   on the client side only. Skipping until ContentPackUtils + FMLEnvironment dist check are ported.
    }
}
