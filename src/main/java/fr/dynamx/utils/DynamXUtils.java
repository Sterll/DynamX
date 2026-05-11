package fr.dynamx.utils;

import com.google.common.base.Predicates;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
// TODO port:1.20.1 - de.javagl.jgltf.dynamx.model package not available in 1.20.1 dependency set;
// methods that consumed NodeModel are stubbed to Object below.
import fr.dynamx.DynamX;
import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.EnumDxModelFormats;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.TrailerAttachModule;
import fr.dynamx.common.entities.modules.engines.BasicEngineModule;
import fr.dynamx.common.entities.vehicles.TrailerEntity;
import fr.dynamx.common.objloader.data.DxModelData;
import fr.dynamx.common.objloader.data.GltfModelData;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.EntityJointsHandler;
import fr.dynamx.common.physics.utils.StairsBox;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import fr.dynamx.utils.physics.PhysicsRaycastResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.joml.Quaternionf;
import org.lwjgl.BufferUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.function.Predicate;

/**
 * General utility methods
 *
 * @see fr.dynamx.utils.maths.DynamXMath
 * @see fr.dynamx.utils.maths.DynamXGeometry
 * @see DynamXPhysicsHelper
 */
public class DynamXUtils {
    public static void writeBlockPos(ByteBuf buf, BlockPos blockPos) {
        buf.writeDouble(blockPos.getX());
        buf.writeDouble(blockPos.getY());
        buf.writeDouble(blockPos.getZ());
    }

    public static BlockPos readBlockPos(ByteBuf buf) {
        return new BlockPos((int) buf.readDouble(), (int) buf.readDouble(), (int) buf.readDouble());
    }

    public static void writeVector3f(ByteBuf buf, Vector3f vector3f) {
        buf.writeFloat(vector3f.x);
        buf.writeFloat(vector3f.y);
        buf.writeFloat(vector3f.z);
    }

    public static Vector3f readVector3f(ByteBuf buf) {
        return new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void writeQuaternion(ByteBuf buf, Quaternion quaternion) {
        buf.writeFloat(quaternion.getX());
        buf.writeFloat(quaternion.getY());
        buf.writeFloat(quaternion.getZ());
        buf.writeFloat(quaternion.getW());
    }

    public static Quaternion readQuaternion(ByteBuf buf) {
        return new Quaternion(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void writeQuaternionNBT(CompoundTag compound, Quaternion quaternion) {
        compound.putFloat("QuatX", quaternion.getX());
        compound.putFloat("QuatY", quaternion.getY());
        compound.putFloat("QuatZ", quaternion.getZ());
        compound.putFloat("QuatW", quaternion.getW());
    }

    public static Quaternion readQuaternionNBT(CompoundTag compound) {
        return QuaternionPool.get(compound.getFloat("QuatX"), compound.getFloat("QuatY"), compound.getFloat("QuatZ"), compound.getFloat("QuatW"));
    }

    /**
     * @return A new {@link DxModelPath} for this model
     */
    public static DxModelPath getModelPath(String packName, ResourceLocation model) {
        List<PackInfo> packLocations = DynamXObjectLoaders.PACKS.findPackLocations(packName);
        if (packLocations.isEmpty()) {
            DynamX.LOGGER.error("Pack info {} not found. This should not happen.", packName);
            return new DxModelPath(PackInfo.forAddon(packName).setPackType(ContentPackType.FOLDER), model);
        }
        return new DxModelPath(packLocations, model);
    }

    public static byte[] readInputStream(InputStream resource) throws IOException {
        int i;
        byte[] buffer = new byte[65565];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((i = resource.read(buffer, 0, buffer.length)) != -1) {
            out.write(buffer, 0, i);
        }
        out.flush();
        out.close();
        resource.close();
        return out.toByteArray();
    }

    public static Vector3f toVector3f(Vec3 pos) {
        return Vector3fPool.get((float) pos.x, (float) pos.y, (float) pos.z);
    }

    public static Vector3f toVector3f(org.joml.Vector3f pos) {
        return Vector3fPool.get(pos.x, pos.y, pos.z);
    }

    public static Vector3f toVector3f(BlockPos pos) {
        return Vector3fPool.get((float) pos.getX(), (float) pos.getY(), (float) pos.getZ());
    }

    public static org.joml.Vector3f toVector3f(Vector3f pos) {
        return new org.joml.Vector3f(pos.x, pos.y, pos.z);
    }

    public static org.joml.Vector3f toVector3f(float x, float y, float z) {
        return new org.joml.Vector3f(x, y, z);
    }


    public static Quaternionf toQuaternion(Quaternion quat) {
        return new Quaternionf(quat.getX(), quat.getY(), quat.getZ(), quat.getW());
    }

    public static Quaternionf toQuaternion(org.joml.Quaternionf quat) {
        // TODO port:1.20.1 - JOML Quaternionf exposes x/y/z/w as public fields, not getX/getY/...
        return new Quaternionf(quat.x, quat.y, quat.z, quat.w);
    }

    public static Vector3f getPositionEyes(Entity entity) {
        return Vector3fPool.get((float) entity.getX(), (float) entity.getY() + entity.getEyeHeight(), (float) entity.getZ());
    }

    public static Vector3f calculateRay(Entity base, float distance, Vector3f offset) {
        Vec3 vec3 = base.position();
        Vec3 vec31 = base.getViewVector(1);
        Vec3 vec32 = vec3.add(vec31.x * distance, vec31.y * distance, vec31.z * distance);
        Vector3f lookAt = Vector3fPool.get((float) vec32.x, (float) vec32.y, (float) vec32.z);
        lookAt.subtractLocal(offset.x, offset.y, offset.z);
        return lookAt;
    }

    public static PhysicsRaycastResult castRayFromEntity(Entity entity, float distanceMax, Predicate<EnumBulletShapeType> ignoredPredicate) {
        Vector3f eyePos = DynamXUtils.getPositionEyes(entity); //from
        Vector3f eyeLook = DynamXUtils.toVector3f(entity.getViewVector(1)); //to
        Vector3f lookAt = new Vector3f(eyePos.x, eyePos.y, eyePos.z);
        eyeLook.multLocal(distanceMax);
        lookAt.addLocal(eyeLook);

        return DynamXPhysicsHelper.castRay(DynamXContext.getPhysicsWorld(entity.level()), eyePos, lookAt, ignoredPredicate);
    }

    public static ListTag newDoubleNBTList(double... numbers) {
        ListTag nbttaglist = new ListTag();

        for (double d0 : numbers) {
            nbttaglist.add(DoubleTag.valueOf(d0));
        }

        return nbttaglist;
    }

    public static ListTag newFloatNBTList(float... numbers) {
        ListTag nbttaglist = new ListTag();

        for (float f : numbers) {
            nbttaglist.add(FloatTag.valueOf(f));
        }

        return nbttaglist;
    }

    public static Vector3f getCameraTranslation(Minecraft mc, float delta) {
        return Vector3fPool.get((float) mc.player.xo + (float) (mc.player.getX() - mc.player.xo) * delta, (float) mc.player.yo + (float) (mc.player.getY() - (float) mc.player.yo) * delta, (float) mc.player.zo + (float) (mc.player.getZ() - (float) mc.player.zo) * delta);
    }

    public static HitResult rayTraceEntitySpawn(Level worldIn, Player playerIn, InteractionHand hand) {
        return getMouseOver(playerIn, 1);
    }

    /**
     * We put that here, because smart people of Forge think a ray trace is a client thing
     */
    private static BlockHitResult rayTrace(Entity entity, double blockReachDistance, float partialTicks) {
        Vec3 vec3d = entity.getEyePosition(partialTicks);
        Vec3 vec3d1 = entity.getViewVector(partialTicks);
        Vec3 vec3d2 = vec3d.add(vec3d1.x * blockReachDistance, vec3d1.y * blockReachDistance, vec3d1.z * blockReachDistance);
        return entity.level().clip(new ClipContext(vec3d, vec3d2, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity));
    }

    public static HitResult getMouseOver(Entity entity, float partialTicks) {
        HitResult objectMouseOver = null;
        if (entity != null) {
            if (entity.level() != null) {
                Entity pointedEntity = null;
                double d0 = 5;
                objectMouseOver = rayTrace(entity, d0, partialTicks);
                Vec3 vec3d = entity.getEyePosition(partialTicks);
                int i = 3;
                double d1 = d0;

                if (objectMouseOver != null) {
                    d1 = objectMouseOver.getLocation().distanceTo(vec3d);
                }

                Vec3 vec3d1 = entity.getViewVector(1.0F);
                Vec3 vec3d2 = vec3d.add(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0);
                Vec3 vec3d3 = null;
                float f = 1.0F;
                List<Entity> list = entity.level().getEntities(entity, entity.getBoundingBox().expandTowards(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0).inflate(1.0D, 1.0D, 1.0D), EntitySelector.NO_SPECTATORS.and(e -> e != null && e.isPickable()));
                double d2 = d1;

                for (Entity entity1 : list) {
                    AABB axisalignedbb = entity1.getBoundingBox().inflate(entity1.getPickRadius());
                    Optional<Vec3> optional = axisalignedbb.clip(vec3d, vec3d2);

                    if (axisalignedbb.contains(vec3d)) {
                        if (d2 >= 0.0D) {
                            pointedEntity = entity1;
                            vec3d3 = optional.orElse(vec3d);
                            d2 = 0.0D;
                        }
                    } else if (optional.isPresent()) {
                        Vec3 raytraceresultHit = optional.get();
                        double d3 = vec3d.distanceTo(raytraceresultHit);

                        if (d3 < d2 || d2 == 0.0D) {
                            // TODO port:1.20.1 canRiderInteract() removed in 1.20; using getRootVehicle equality only
                            if (entity1.getRootVehicle() == entity.getRootVehicle()) {
                                if (d2 == 0.0D) {
                                    pointedEntity = entity1;
                                    vec3d3 = raytraceresultHit;
                                }
                            } else {
                                pointedEntity = entity1;
                                vec3d3 = raytraceresultHit;
                                d2 = d3;
                            }
                        }
                    }
                }

                if (pointedEntity != null && vec3d.distanceTo(vec3d3) > 3.0D) {
                    pointedEntity = null;
                    objectMouseOver = BlockHitResult.miss(vec3d3, Direction.UP, BlockPos.containing(vec3d3));
                }

                if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
                    objectMouseOver = new EntityHitResult(pointedEntity, vec3d3);
                    /*if (pointedEntity instanceof EntityLivingBase || this.pointedEntity instanceof EntityItemFrame)
                    {
                        this.mc.pointedEntity = this.pointedEntity;
                    }*/
                }
            }
        }
        return objectMouseOver;
    }

    public static BasePart<?> rayTestPart(Player player, PackPhysicsEntity<?, ?> entityPart, IPartContainer<?> packInfo, Predicate<BasePart<?>> wantedPart) {
        Vector3fPool.openPool();
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 hitVec = player.position().add(0, player.getEyeHeight(), 0);
        BasePart<?> nearest = null;
        Vector3f nearestPos = null;
        Vector3f playerPos = Vector3fPool.get((float) player.getX(), (float) player.getY(), (float) player.getZ());
        for (float f = 1.0F; f < 4.0F; f += 0.1F) {
            for (BasePart<?> part : packInfo.getAllParts()) {
                if (wantedPart != null && !wantedPart.test(part)) {
                    continue;
                }
                Vector3f partPos = DynamXGeometry.rotateVectorByQuaternion(part.getPosition(), entityPart.physicsRotation);
                Vector3fPool.openPool();
                partPos.addLocal(toVector3f(entityPart.position()));
                Vector3fPool.closePool();
                if ((nearestPos == null || DynamXGeometry.distanceBetween(partPos, playerPos) < DynamXGeometry.distanceBetween(nearestPos, playerPos))
                        && vecInsideBox(hitVec, part, partPos)) {
                    nearest = part;
                    nearestPos = partPos;
                }
            }
            hitVec = hitVec.add(lookVec.x * 0.1F, lookVec.y * 0.1F, lookVec.z * 0.1F);
        }
        Vector3fPool.closePool();
        return nearest;
    }

    public static boolean vecInsideBox(Vec3 vec1, BasePart<?> part, Vector3f pos) {
        float minX = -part.getScale().x + pos.x;
        float minY = pos.y;
        float minZ = -part.getScale().z + pos.z;
        float maxX = part.getScale().x + pos.x;
        float maxY = part.getScale().y + pos.y;
        float maxZ = part.getScale().z + pos.z;
        return vec1.x > minX && vec1.x < maxX && vec1.y > minY && vec1.y < maxY && vec1.z > minZ && vec1.z < maxZ;
    }

    public static List<Vector3f> floatBufferToVec3f(FloatBuffer buffer, Vector3f offset) {
        List<Vector3f> vector3fList = new ArrayList<>();
        for (int i = 0; i < buffer.limit() / 3; i++) {
            float xF = buffer.get(i * 3);
            float yF = buffer.get(i * 3 + 1);
            float zF = buffer.get(i * 3 + 2);
            vector3fList.add(new Vector3f(xF + offset.x, yF + offset.y, zF + offset.z));
        }
        return vector3fList;
    }

    public static IntBuffer createIntBuffer(int[] data) {
        IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }

    public static FloatBuffer createFloatBuffer(float[] data) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }

    //DUPLICATE (function is already in the BasicsAddon)
    public static int getSpeed(BaseVehicleEntity<?> entity) {
        BasicEngineModule engine = entity.getModuleByType(BasicEngineModule.class);
        if (engine != null) {
            float[] ab = engine.getEngineProperties();
            if (ab == null) return 0;
            return (int) Math.abs(ab[VehicleEntityProperties.EnumEngineProperties.SPEED.ordinal()]);
        }
        return -1;
    }

    public static void attachTrailer(Player player, BaseVehicleEntity<?> carEntity, BaseVehicleEntity<?> trailer) {
        Vector3fPool.openPool();
        Vector3f p1r = DynamXGeometry.rotateVectorByQuaternion(carEntity.getModuleByType(TrailerAttachModule.class).getAttachPoint(), carEntity.physicsRotation);
        Vector3f p2r = DynamXGeometry.rotateVectorByQuaternion(trailer.getModuleByType(TrailerAttachModule.class).getAttachPoint(), trailer.physicsRotation);
        if (p1r.addLocal(carEntity.physicsPosition).subtract(p2r.addLocal(trailer.physicsPosition)).lengthSquared() < 60) {
            if (carEntity.getJointsHandler() == null) {
                return;
            }
            EntityJointsHandler handler = carEntity.getJointsHandler();
            Collection<EntityJoint<?>> curJoints = handler.getJoints();
            TrailerEntity trailerIsAttached = null;
            for (EntityJoint<?> joint : curJoints) {
                if (joint.getEntity2() instanceof TrailerEntity) {
                    trailerIsAttached = (TrailerEntity) joint.getEntity2();
                    break;
                }
            }
            if (trailerIsAttached == null) {
                if (TrailerAttachModule.HANDLER.createJoint(carEntity, trailer, (byte) 0)) {
                    MutableComponent msg = Component.translatable("trailer.attached", trailer.getPackInfo().getName(), carEntity.getPackInfo().getName());
                    msg.setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                    player.sendSystemMessage(msg);
                    if (player.level().isClientSide && trailer instanceof TrailerEntity)
                        ((TrailerEntity<?>) trailer).playAttachSound();
                } else {
                    MutableComponent msg = Component.translatable("trailer.attach.fail", trailer.getPackInfo().getName(), carEntity.getPackInfo().getName());
                    msg.setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
                    player.sendSystemMessage(msg);
                }
            } else {
                carEntity.getJointsHandler().removeJointWith(trailerIsAttached, TrailerAttachModule.JOINT_NAME, (byte) 0);
                player.sendSystemMessage(Component.translatable("trailer.detached"));
            }
        } else {
            player.sendSystemMessage(Component.translatable("trailer.attach.toofar"));
        }
        Vector3fPool.closePool();
    }


    public static void hotswapWorldPackInfos(Level w) {
        DynamX.LOGGER.info("Hot-swapping pack infos in models and spawn entities/tile entities in world " + w);
        // TODO port:1.20.1 loadedEntityList removed; iterate via ServerLevel's getEntities() or use the broader getAllEntities API.
        if (w instanceof net.minecraft.server.level.ServerLevel sl) {
            for (Entity e : sl.getAllEntities()) {
                if (e instanceof IPackInfoReloadListener)
                    ((IPackInfoReloadListener) e).onPackInfosReloaded();
            }
        }
        // TODO port:1.20.1 no global block-entity list in 1.20; reloading existing block entities requires chunk iteration
        if (w.isClientSide)
            DynamXContext.getDxModelRegistry().onPackInfosReloaded();
    }

    /**
     * Shorthand to get a secured input stream able to read terrain data
     *
     * @param is The input stream to read
     * @return An ObjectInputStream that can only read primitives and terrain elements
     * @throws IOException If an I/O error occurs
     */
    public static ObjectInputStream getTerrainObjectsIS(InputStream is) throws IOException {
        Set<String> classesSet = Collections.unmodifiableSet(new HashSet(Arrays.asList(byte[].class.getName(), MutableBoundingBox.class.getName(), StairsBox.class.getName(), Direction.class.getName(), Enum.class.getName())));
        return new ObjectInputStream(is) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                if (!classesSet.contains(desc.getName())) {
                    throw new InvalidClassException("Unauthorized deserialization attempt", desc.getName());
                }
                return super.resolveClass(desc);
            }
        };
    }

    /**
     * Gets the position of the given object in the given 3D model
     *
     * @param modelData       The 3D model
     * @param objectName      The name of the object to get the pos of
     * @param allowPartCenter If true, the center of the object will be used as position for obj models (and the translation for gltf models) <br>
     *                        If false, the position can only be read from gltf models
     * @return The translation of the object, is this is a gltf model, or the center of the object if this is an obj model and allowPartCenter is true
     */
    @Nullable
    public static Vector3f readPartPosition(DxModelData modelData, String objectName, boolean allowPartCenter) {
        return readPartPosition(modelData, objectName, allowPartCenter, false);
    }

    /**
     * Gets the position of the given object in the given 3D model
     *
     * @param modelData       The 3D model
     * @param objectName      The name of the object to get the pos of
     * @param allowPartCenter If true, the center of the object will be used as position for obj models (and the translation for gltf models) <br>
     *                        If false, the position can only be read from gltf models
     * @param forceCenter     If true, the center of the object will be returned for both obj and gltf models
     * @return The translation of the object, is this is a gltf model and forceCenter is false, or the center of the object if this is an obj model and allowPartCenter is true, or forceCenter is true
     */
    @Nullable
    public static Vector3f readPartPosition(DxModelData modelData, String objectName, boolean allowPartCenter, boolean forceCenter) {
        assert !forceCenter || allowPartCenter : "forceCenter is true but allowPartCenter is false";
        if (!modelData.getMeshNames().contains(objectName.toLowerCase()))
            return null;
        if (forceCenter || modelData.getFormat() == EnumDxModelFormats.OBJ) {
            return allowPartCenter ? modelData.getMeshCenter(objectName, new Vector3f()) : null;
        } else if (modelData.getFormat() == EnumDxModelFormats.GLTF) {
            // TODO port:1.20.1 - NodeModel-based GLTF position resolution is disabled until the
            // jgltf-dynamx dependency is repackaged for 1.20.1; return null to skip the lookup.
            return null;
        }
        return null;
    }

    /**
     * Gets the rotation of the given object in the given 3D model <br>
     * Note: This method only works for gltf models
     *
     * @param modelData  The 3D model
     * @param objectName The name of the object to get the rotation of
     * @return The rotation of the object, or null if the model is not a gltf model or if the object has no rotation
     */
    @Nullable
    public static Quaternion readPartRotation(DxModelData modelData, String objectName) {
        if (modelData.getFormat() != EnumDxModelFormats.GLTF || !modelData.getMeshNames().contains(objectName.toLowerCase()))
            return null;
        // TODO port:1.20.1 - NodeModel-based GLTF rotation resolution disabled (see readPartPosition).
        return null;
    }

    /**
     * Gets the scale (size) of the given object in the given 3D model
     *
     * @param modelData  The 3D model
     * @param objectName The name of the object to get the scale of
     * @return The scale of the object, or an empty vector if the object isn't found in the model
     */
    @Nonnull
    public static Vector3f readPartScale(DxModelData modelData, String objectName) {
        if (!modelData.getMeshNames().contains(objectName.toLowerCase()))
            return new Vector3f();
        return modelData.getMeshDimension(objectName, new Vector3f());
    }

    public static void addItemTooltip(List<String> tooltip, AbstractItemObject<?, ?> itemInfo, byte itemVariant) {
        if (DynamXConfig.disableItemTooltips) {
            return;
        }

        tooltip.add(ChatFormatting.GOLD + I18n.get("dynamx.item.description", itemInfo.getDescription()));
        tooltip.add(ChatFormatting.DARK_PURPLE + I18n.get("dynamx.item.pack", itemInfo.getPackName()));

        // TODO port:1.20.1 - cast to IModelTextureVariantsSupplier; AbstractItemObject is not yet
        // declared to implement it in the ported version of IModelPackObject.
        fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier variants = (fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier) (Object) itemInfo;
        if (variants.getMaxVariantId() <= 1) {
            return;
        }
        String variantName = variants.getMainObjectVariantNameOrDefault(itemVariant, null);
        if (variantName == null) {
            tooltip.add(ChatFormatting.RED + "Texture not found, check your pack errors");
            return;
        }
        tooltip.add(ChatFormatting.GREEN + I18n.get("dynamx.item.variant", variantName));
    }

    /**
     * Reads a UTF-8 string from a ByteBuf using a length-prefixed encoding (int length + bytes).
     * TODO port:1.20.1 - simple stand-alone helper to replace the legacy ByteBufUtils.readUTF8String wrapper.
     */
    public static String readUtf8String(io.netty.buffer.ByteBuf in) {
        int len = in.readInt();
        if (len < 0) return null;
        byte[] bytes = new byte[len];
        in.readBytes(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Writes a UTF-8 string to a ByteBuf using the length-prefixed encoding accepted by readUtf8String.
     * TODO port:1.20.1 - paired with readUtf8String above.
     */
    public static void writeUtf8String(io.netty.buffer.ByteBuf out, String s) {
        if (s == null) {
            out.writeInt(-1);
            return;
        }
        byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}
