package fr.dynamx.client;

import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.items.DynamXItemBlock;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Virtual client resource pack that generates, on demand, the blockstate / model JSONs that DynamX
 * blocks need but that the content packs do not ship.
 *
 * <p>The legacy 1.12 code produced these through {@code ModelLoader.setCustomStateMapper} +
 * {@code ContentPackUtils.createBlockJson}. Both the state-mapper API and on-disk generation (which
 * only ever worked for folder packs, never zips/{@code .dnxpack}) are gone in 1.20.1. Instead we
 * answer the resource manager directly:
 * <ul>
 *   <li>{@code blockstates/<name>.json} - for every registered {@link DynamXBlock}: 16 metadata
 *       variants all pointing at a generated empty block model. The real 3D render is handled by
 *       {@code TESRDynamXBlock}; this just satisfies the block-model baker.</li>
 *   <li>{@code models/block/<name>.json} - empty model {@code {}} for those blocks.</li>
 *   <li>{@code models/item/<name>.json} - for {@link DynamXItemBlock} block-items only, parented to
 *       the generated block model.</li>
 * </ul>
 *
 * <p>Every other path returns {@code null}, so the real content packs keep serving their own item
 * models (e.g. the {@code <pack>.vehicle_<name>_default.json} icons) without interference.
 */
public class DynamXGeneratedResourcePack implements PackResources {

    public static final String PACK_ID = "dynamx_generated";

    private static final String EMPTY_MODEL = "{ }";

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation location) {
        if (packType != PackType.CLIENT_RESOURCES || !DynamXConstants.ID.equals(location.getNamespace())) {
            return null;
        }
        String json = generate(location.getPath());
        if (json == null) {
            return null;
        }
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return () -> new ByteArrayInputStream(bytes);
    }

    @Nullable
    private String generate(String path) {
        if (!path.endsWith(".json")) {
            return null;
        }
        if (path.startsWith("blockstates/")) {
            String name = strip(path, "blockstates/");
            return isDynamXBlock(name) ? blockstateJson(name) : null;
        }
        if (path.startsWith("models/block/")) {
            String name = strip(path, "models/block/");
            return isDynamXBlock(name) ? EMPTY_MODEL : null;
        }
        if (path.startsWith("models/item/")) {
            String name = strip(path, "models/item/");
            return isDynamXBlockItem(name)
                    ? "{ \"parent\": \"" + DynamXConstants.ID + ":block/" + name + "\" }"
                    : null;
        }
        return null;
    }

    private static String strip(String path, String prefix) {
        return path.substring(prefix.length(), path.length() - ".json".length());
    }

    private static boolean isDynamXBlock(String name) {
        ResourceLocation id = tryId(name);
        return id != null && ForgeRegistries.BLOCKS.getValue(id) instanceof DynamXBlock;
    }

    private static boolean isDynamXBlockItem(String name) {
        ResourceLocation id = tryId(name);
        return id != null && ForgeRegistries.ITEMS.getValue(id) instanceof DynamXItemBlock;
    }

    @Nullable
    private static ResourceLocation tryId(String name) {
        try {
            return new ResourceLocation(DynamXConstants.ID, name);
        } catch (net.minecraft.ResourceLocationException e) {
            return null;
        }
    }

    /** 16 metadata variants, all mapped to the empty generated block model (rotation handled by the TE). */
    private static String blockstateJson(String name) {
        StringBuilder sb = new StringBuilder("{ \"variants\": {");
        for (int meta = 0; meta <= 15; meta++) {
            sb.append("\"metadata=").append(meta).append("\": { \"model\": \"")
                    .append(DynamXConstants.ID).append(":block/").append(name).append("\" }");
            if (meta != 15) {
                sb.append(',');
            }
        }
        sb.append("} }");
        return sb.toString();
    }

    @Override
    public void listResources(PackType packType, String namespace, String path, ResourceOutput resourceOutput) {
        // Generated lazily by name in getResource(); the block-model baker looks each blockstate up by
        // the block's registry id, so there is nothing to enumerate here.
    }

    @Override
    public Set<String> getNamespaces(PackType packType) {
        return packType == PackType.CLIENT_RESOURCES ? Set.of(DynamXConstants.ID) : Set.of();
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
        if (deserializer == PackMetadataSection.TYPE) {
            return (T) new PackMetadataSection(Component.literal("DynamX generated models"), 15);
        }
        return null;
    }

    @Override
    public String packId() {
        return PACK_ID;
    }

    @Override
    public boolean isBuiltin() {
        return true;
    }

    @Override
    public void close() {
        // Nothing to release.
    }
}
