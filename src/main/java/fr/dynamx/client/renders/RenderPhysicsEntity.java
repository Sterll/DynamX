package fr.dynamx.client.renders;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.dynamx.api.events.client.DynamXEntityRenderEvent;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.debug.renderer.DebugRenderer;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.SubClassPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base entity renderer for DynamX physics entities. <br>
 *
 * <p>TODO port:1.20.1 - This file is heavily stubbed. The 1.12-era hierarchy was:
 * <ul>
 *   <li>{@code extends Render<T>} (took {@code RenderManager} in its constructor) -&gt; now
 *       {@link EntityRenderer EntityRenderer&lt;T&gt;} (takes {@code EntityRendererProvider.Context}).</li>
 *   <li>{@code doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks)} -&gt;
 *       {@code render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)}.
 *       The (x, y, z) are now derived from the PoseStack which the engine pre-translates to the
 *       entity's interpolated position.</li>
 *   <li>{@code MinecraftForge.EVENT_BUS.post(...)} -&gt; {@code NeoForge.EVENT_BUS.post(...)}.</li>
 *   <li>{@code GlStateManager.translate / rotate / pushMatrix / popMatrix / disableLighting / disableDepth / disableTexture2D} -&gt;
 *       {@code PoseStack#translate / mulPose / pushPose / popPose} + {@code RenderType} selection
 *       on the {@link MultiBufferSource} (lighting/depth/texture state is folded into the RenderType).</li>
 *   <li>{@code MinecraftForgeClient.getRenderPass()} - the multi-pass render is gone in core profile;
 *       transparent passes are now picked up by the chunk render pipeline. Hardcoded to 0 here.</li>
 *   <li>{@code renderOffsetAABB(...)} (vanilla white-bounding-box fallback) removed -- a missing model
 *       fallback must be reauthored on top of {@code LevelRenderer.renderLineBox}.</li>
 *   <li>{@code getEntityTexture} -&gt; {@code getTextureLocation} in 1.20.1.</li>
 * </ul>
 *
 * @param <T> The entity type
 */
public abstract class RenderPhysicsEntity<T extends PhysicsEntity<?>> extends EntityRenderer<T> {
    private final List<DebugRenderer<T>> debugRenderers = new ArrayList<>();
    public static boolean shouldRenderPlayerSitting;

    public RenderPhysicsEntity(EntityRendererProvider.Context context) {
        super(context);
        addDebugRenderers(new DebugRenderer.ShapesDebug());
    }

    /**
     * Setups render translation and rotation before rendering the entity.
     *
     * <p>TODO port:1.20.1 - In 1.12 this fed values into the fixed-function matrix stack via
     * {@code GlStateManager.translate} / {@code GlStateManager.rotate}. The new entry point now
     * takes a {@link PoseStack} that should be mutated instead. The original return type was
     * {@code org.lwjgl.util.vector.Quaternion} (lwjgl2). We keep the JOML-quat returned by
     * {@code ClientDynamXUtils.computeInterpolatedJomlQuaternion} now.
     */
    public org.joml.Quaternionf setupRenderTransform(T entity, org.joml.Vector3f renderPosition, float partialTicks) {
        // TODO port:1.20.1 - was:
        //   GlStateManager.translate((float) renderPosition.x, (float) renderPosition.y, (float) renderPosition.z);
        //   Quaternion q = ClientDynamXUtils.computeInterpolatedGlQuaternion(entity.prevRenderRotation, entity.renderRotation, partialTicks);
        //   GlStateManager.rotate(q);
        return fr.dynamx.utils.client.ClientDynamXUtils.computeInterpolatedJomlQuaternion(
                entity.prevRenderRotation, entity.renderRotation, partialTicks);
    }

    /**
     * The 1.20.1 render entry point. Replaces the legacy
     * {@code doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks)}.
     *
     * <p>TODO port:1.20.1 - PoseStack / MultiBufferSource / packedLight should be passed to the
     * scene graph so leaves can call into the GLTF renderer. For now the call is forwarded to the
     * legacy {@code renderEntity / spawnParticles / renderDebug} hooks with x/y/z = 0 because the
     * engine has already translated the {@code PoseStack} to the entity position.
     */
    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        entity.wasRendered = true;
        if (!canRender(entity)) {
            return;
        }

        BaseRenderContext.EntityRenderContext context = getRenderContext(entity);
        if (context == null) {
            // TODO port:1.20.1 - was: renderOffsetAABB(entity.getEntityBoundingBox(), x - entity.lastTickPosX, ...);
            // Reauthor a missing-model fallback on top of LevelRenderer.renderLineBox.
            return;
        }
        // The engine already pre-translated the PoseStack to (entity.x - cameraX, ...) so we use 0,0,0
        // for the render position and rely on the PoseStack instead.
        context.setRenderParams(0, 0, 0, partialTicks, false);

        // TODO port:1.20.1 - render passes are gone in core profile; hardcoded to 0.
        int renderPass = 0;
        QuaternionPool.openPool(SubClassPool.ENTITY_RENDER);
        Vector3fPool.openPool(SubClassPool.ENTITY_RENDER);
        GlQuaternionPool.openPool(SubClassPool.ENTITY_RENDER);

        // Render vehicle
        DynamXEntityRenderEvent preEvent = new DynamXEntityRenderEvent(entity, context, DynamXEntityRenderEvent.Type.ENTITY, renderPass);
        // TODO port:1.20.1 - NeoForge events: post returns the event, check isCanceled() if ICancellableEvent.
        NeoForge.EVENT_BUS.post(preEvent);
        renderEntity(entity, context);

        if (renderPass == 0) {
            spawnParticles(entity, context);
            // Render debug
            DynamXEntityRenderEvent debugEvent = new DynamXEntityRenderEvent(entity, context, DynamXEntityRenderEvent.Type.DEBUG, renderPass);
            NeoForge.EVENT_BUS.post(debugEvent);
            renderDebug(entity, context);
        }
        NeoForge.EVENT_BUS.post(new DynamXEntityRenderEvent(entity, context, DynamXEntityRenderEvent.Type.POST, renderPass));

        Vector3fPool.closePool();
        QuaternionPool.closePool();
        GlQuaternionPool.closePool();
    }

    public void spawnParticles(T physicsEntity, BaseRenderContext.EntityRenderContext context) {
        // TODO port:1.20.1 - particle spawning was:
        //   if (physicsEntity instanceof PackPhysicsEntity) {
        //     PackPhysicsEntity<?, ?> packPhysicsEntity = (PackPhysicsEntity<?, ?>) physicsEntity;
        //     if (packPhysicsEntity.getPackInfo() instanceof ParticleEmitterInfo.IParticleEmitterContainer) {
        //       DynamXRenderUtils.spawnParticles((ParticleEmitterInfo.IParticleEmitterContainer) packPhysicsEntity.getPackInfo(),
        //           physicsEntity.world, physicsEntity.physicsPosition, physicsEntity.physicsRotation);
        //     }
        //   }
        // Rewrite with Level#addParticle once ParticleEmitterInfo is ported.
    }

    /**
     * @return All debug renders for this entity renderer
     */
    public List<DebugRenderer<T>> getDebugRenderers() {
        return debugRenderers;
    }

    /**
     * Adds the debug renders to the list
     *
     * @deprecated The debug should be rendered using the new {@link SceneNode}s system
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public void addDebugRenderers(DebugRenderer<?>... renderers) {
        for (DebugRenderer<?> renderer : renderers) {
            debugRenderers.add((DebugRenderer<T>) renderer);
        }
    }

    /**
     * Renders the entity in the world
     */
    public abstract void renderEntity(T entity, BaseRenderContext.EntityRenderContext context);

    /**
     * Renders the entity debug in the world
     */
    public abstract void renderEntityDebug(T entity, BaseRenderContext.EntityRenderContext context);

    /**
     * Should return an EntityRenderContext with the entity parameters set <br>
     * The render parameters are automatically set <br>
     * Returning null will cancel the render and render a vanilla white box instead <br>
     * The returned context must be as complete as possible, as it is given in render events fired
     * before the call to renderEntity
     *
     * @param entity The entity to render
     * @return A render context with the entity parameters set
     */
    @Nullable
    public abstract BaseRenderContext.EntityRenderContext getRenderContext(T entity);

    /**
     * Renders active {@link DebugRenderer}s <br>
     * Shouldn't be overridden : use
     * {@link RenderPhysicsEntity#renderEntityDebug(PhysicsEntity, BaseRenderContext.EntityRenderContext)}
     * to render your debug <br>
     * Can be cancelled via the dedicated event
     */
    public final void renderDebug(T entity, BaseRenderContext.EntityRenderContext context) {
        // TODO port:1.20.1 - was guarded by ClientDebugSystem.enableDebugDrawing which lives in
        // fr.dynamx.client.handlers (not ported yet). Defaulted to false so debug rendering is
        // disabled until that class lands; the structure of this method is preserved.
        if (false /* ClientDebugSystem.enableDebugDrawing */) {
            List<DebugRenderer<T>> validRotatedRenders = debugRenderers.stream().filter(r -> r.shouldRender(entity) && r.hasEntityRotation(entity)).collect(Collectors.toList());
            List<DebugRenderer<T>> validPureRenders = debugRenderers.stream().filter(r -> r.shouldRender(entity) && !r.hasEntityRotation(entity)).collect(Collectors.toList());
            QuaternionPool.openPool();
            Vector3fPool.openPool();

            // TODO port:1.20.1 - was wrapped in:
            //   GlStateManager.pushMatrix();
            //   GlStateManager.disableLighting();
            //   GlStateManager.disableDepth();
            //   GlStateManager.disableTexture2D();
            //   ... rendering ...
            //   GlStateManager.enableLighting();
            //   GlStateManager.enableTexture2D();
            //   GlStateManager.enableDepth();
            //   GlStateManager.popMatrix();
            // Replace with PoseStack push/pop + a RenderType that disables lighting/depth/texture.
            renderEntityDebug(entity, context);

            double x = context.getRenderPosition().x, y = context.getRenderPosition().y, z = context.getRenderPosition().z;
            float partialTicks = context.getPartialTicks();
            validRotatedRenders.forEach(renderer -> renderer.render(entity, this, x, y, z, partialTicks));
            validPureRenders.forEach(renderer -> renderer.render(entity, this, x, y, z, partialTicks));

            Vector3fPool.closePool();
            QuaternionPool.closePool();
        }
    }

    /**
     * You can return null : textures are managed by obj renderer.
     *
     * <p>TODO port:1.20.1 - was {@code getEntityTexture}; renamed to {@code getTextureLocation}.
     */
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return null;
    }

    /**
     * Checks if the entity can be rendered, before any rendering and event
     */
    public boolean canRender(T entity) {
        return entity.initialized == PhysicsEntity.EnumEntityInitState.ALL;
    }

    /**
     * Called to render this part <br>
     * Will draw a white box over the all entity if model wasn't loaded (not found for example).
     *
     * <p>TODO port:1.20.1 - the {@code renderOffsetAABB} fallback path is dropped; missing-model
     * rendering must be reauthored on top of {@code LevelRenderer.renderLineBox}.
     */
    public void renderModel(DxModelRenderer model, @Nullable net.minecraft.world.entity.Entity entity, byte textureDataId, boolean forceVanillaRender) {
        if (!model.isEmpty()) {
            model.renderModel(textureDataId, forceVanillaRender);
        }
    }

    /**
     * Called to render the main part of this model with the custom texture.
     */
    public void renderMainModel(DxModelRenderer model, @Nullable net.minecraft.world.entity.Entity entity, byte textureDataId, boolean forceVanillaRender) {
        if (model != null) {
            model.renderDefaultParts(textureDataId, forceVanillaRender);
        }
    }

    /**
     * Called to render specific parts with the custom texture.
     */
    public void renderModelGroup(DxModelRenderer model, String group, @Nullable net.minecraft.world.entity.Entity entity, byte textureDataId, boolean forceVanillaRender) {
        if (model != null) {
            model.renderGroup(group, textureDataId, forceVanillaRender);
        }
    }
}
