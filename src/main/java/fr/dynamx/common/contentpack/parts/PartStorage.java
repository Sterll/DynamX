package fr.dynamx.common.contentpack.parts;

import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.utils.DynamXConstants;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Storage part of a pack object.
 *
 * TODO port:1.20.1 - Original referenced (Phase 6 / Phase 4):
 *   - fr.dynamx.api.entities.modules.ModuleListBuilder
 *   - fr.dynamx.common.entities.modules.StorageModule
 *   - fr.dynamx.common.entities.{IDynamXObject, PackPhysicsEntity}
 *   - fr.dynamx.common.blocks.TEDynamXBlock
 *   - net.minecraftforge.fml.common.FMLCommonHandler (removed) - now FMLEnvironment.dist.isClient()
 *   - EntityPlayer#openGui (removed in 1.20.1) - now NetworkHooks#openScreen or MenuProvider pattern
 *   The interact() / addModules() / addBlockModules() / postLoad() bodies are stubbed.
 *
 * TODO port:1.20.1 - The first generic A of InteractivePart was IDynamXObject (Phase 6); relaxed to Object.
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
        // TODO port:1.20.1 - Original:
        //   if (!modules.hasModuleOfClass(StorageModule.class))
        //       modules.add(new StorageModule(entity, this));
        //   else modules.getByClass(StorageModule.class).addInventory(entity, this);
        //   StorageModule lives in Phase 6.
    }

    @Override
    public void addBlockModules(Object blockEntity, Object modules) {
        // TODO port:1.20.1 - Original called blockEntity.getPos() and instantiated StorageModule
        //   with that BlockPos. TEDynamXBlock lives in Phase 4 and StorageModule in Phase 6.
    }

    @Override
    public ResourceLocation getHudCursorTexture() {
        return new ResourceLocation(DynamXConstants.ID, "textures/storage.png");
    }

    @Override
    public boolean interact(Object entity, Player player) {
        // TODO port:1.20.1 - Original opened a GUI via player.openGui(DynamXMain.instance, ...).
        //   In 1.20.1 we need to use NetworkHooks.openScreen / a MenuProvider; both depend on
        //   Phase 5 (network) and Phase 6 (modules) being ported.
        return false;
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
        //   on the client side only. FMLCommonHandler is removed; use FMLEnvironment.dist.isClient().
        //   Skipping until ContentPackUtils and the lang-injection pipeline are ported.
    }
}
