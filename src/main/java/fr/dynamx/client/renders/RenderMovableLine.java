package fr.dynamx.client.renders;

/**
 * Renders the movable pick-up line from a player to a held physics entity.
 *
 * <p>TODO port:1.20.1 - This entire file is stubbed. The 1.12 implementation relied on:
 * <ul>
 *   <li>{@code DynamXContext.getPlayerPickingObjects()} (Phase 5).</li>
 *   <li>{@code Minecraft.getMinecraft().player / world.getEntityByID(...)} - now
 *       {@code Minecraft.getInstance().player / level().getEntity(id)}.</li>
 *   <li>{@code MovableModule#pickObjects#getHitBody} from {@code fr.dynamx.common.entities.modules}
 *       (Phase 6).</li>
 *   <li>{@code GlStateManager.glBegin(GL_LINE_STRIP) / glVertex3f / glEnd} - replaced by
 *       {@code MultiBufferSource.getBuffer(RenderType.lines())} + {@code VertexConsumer#vertex(...)}.</li>
 *   <li>{@code GlStateManager.disableLighting / disableTexture2D} - folded into the lines RenderType.</li>
 *   <li>{@code DynamXUtils.getCameraTranslation(Minecraft, partialTicks)} - {@code GameRenderer#getMainCamera()}.</li>
 * </ul>
 *
 * Public method signatures are kept; bodies are stubbed.
 */
public class RenderMovableLine {
    public static boolean hasMovableLines() {
        // TODO port:1.20.1 - was: return !DynamXContext.getPlayerPickingObjects().isEmpty();
        return false;
    }

    public static void renderLine(float partialTicks) {
        // TODO port:1.20.1 - rewrite on top of PoseStack/MultiBufferSource and RenderType.lines().
        // The 1.12 code iterated DynamXContext.getPlayerPickingObjects(), resolved the player and
        // physics entity, looked up the MovableModule#pickObjects.getHitBody(), and drew a single
        // line strip from the player's first-person hand offset to the local pick position on the
        // hit body. See legacy RenderMovableLine for the reference math.
    }
}
