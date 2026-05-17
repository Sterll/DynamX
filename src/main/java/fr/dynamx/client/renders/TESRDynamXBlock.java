package fr.dynamx.client.renders;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Block-entity renderer for DynamX blocks.
 *
 * <p>TODO port:1.20.1 -
 * <ul>
 *   <li>{@code extends TileEntitySpecialRenderer<T>} -&gt; {@link BlockEntityRenderer BlockEntityRenderer&lt;T&gt;}.
 *       The constructor now takes a {@link BlockEntityRendererProvider.Context}.</li>
 *   <li>{@code render(T te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)} -&gt;
 *       {@code render(T be, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay)}.
 *       The (x, y, z) translation is folded into the PoseStack by the engine.</li>
 *   <li>{@code te.getBlockMetadata()} - metadata is gone; use {@code BlockState} properties.</li>
 *   <li>{@code DynamXContext.getDxModelRegistry().getModel(...)} - DynamXContext isn't ported yet (Phase 5/8).</li>
 *   <li>{@code MinecraftForge.EVENT_BUS.post(new DynamXBlockEvent.RenderTileEntity(...))} -&gt; NeoForge bus.</li>
 *   <li>{@code GlStateManager.disableLighting / disableDepth / disableTexture2D} - folded into RenderType.</li>
 * </ul>
 *
 * @param <T> The block entity type
 */
public class TESRDynamXBlock<T extends TEDynamXBlock> implements BlockEntityRenderer<T> {
    protected final BaseRenderContext.BlockRenderContext context = new BaseRenderContext.BlockRenderContext();

    public TESRDynamXBlock(BlockEntityRendererProvider.Context context) {
        // TODO port:1.20.1 - constructor injection point for the BlockEntityRendererProvider.Context
        // (font, blockRenderDispatcher, modelSet, ...). Saved for later use.
    }

    @Override
    public void render(T te, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockObject<?> packInfo = te.getPackInfo();
        if (packInfo == null) {
            return;
        }
        // TODO port:1.20.1 - DynamXContext.getDxModelRegistry().getModel(...) is the next dep.
        // For now, leave the renderer wired but with a null model; the scene node will skip drawing.
        Vector3fPool.openPool();
        QuaternionPool.openPool();
        BaseRenderContext.BlockRenderContext blockContext = this.context.setModelParams(te, null, (byte) 0);
        blockContext.setRenderParams(0, 0, 0, partialTicks, false);
        @SuppressWarnings({"rawtypes"})
        SceneNode sceneNode = packInfo.getSceneGraph();
        if (sceneNode != null) {
            // TODO port:1.20.1 - was wrapped in MinecraftForge.EVENT_BUS.post(DynamXBlockEvent.RenderTileEntity(...))
            // pre/post pair; rewrite once DynamXBlockEvent is migrated.
            sceneNode.render(blockContext, packInfo, null);
            // TODO port:1.20.1 - particle spawn for blocks. Was:
            //   DynamXRenderUtils.spawnParticles(packInfo, te.getWorld(), pos, rot);
            if (shouldRenderDebug()) {
                sceneNode.renderDebug(blockContext, packInfo);
            }
        }
        QuaternionPool.closePool();
        Vector3fPool.closePool();
    }

    public boolean shouldRenderDebug() {
        // TODO port:1.20.1 - was: return ClientDebugSystem.enableDebugDrawing;
        // ClientDebugSystem (fr.dynamx.client.handlers) isn't ported yet; return false until then.
        return false;
    }
}
