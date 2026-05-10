package fr.dynamx.client.gui;

import com.jme3.math.Vector3f;
import fr.aym.acsguis.api.ACsGuiFrame;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.textarea.GuiFloatField;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.aym.acsguis.event.listeners.mouse.IMouseMoveListener;
import fr.aym.acsguis.utils.ComponentRenderContext;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.network.packets.MessageSyncBlockCustomization;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.List;

/**
 * 3D-preview frame for the in-world block customization GUI.
 *
 * <p>TODO port:1.20.1 - massive rework required:</p>
 * <ul>
 *   <li>{@code BlockRendererDispatcher} -> {@code BlockRenderDispatcher}.</li>
 *   <li>{@code TextureMap.LOCATION_BLOCKS_TEXTURE} -> {@code TextureAtlas.LOCATION_BLOCKS}.</li>
 *   <li>{@code GlStateManager.pushMatrix/translate/scale/rotate/color} -> {@code PoseStack} via
 *       {@code ComponentRenderContext}'s {@code GuiGraphics}.</li>
 *   <li>{@code GlStateManager.disableTexture2D/enableTexture2D} is gone in core profile.</li>
 *   <li>{@code blockRenderer.renderBlockBrightness(state, brightness)} ->
 *       {@code blockRenderer.renderSingleBlock(BlockState, PoseStack, MultiBufferSource, int, int)}.</li>
 *   <li>{@code TileEntityRendererDispatcher.instance.render(te, x, y, z, partial)} ->
 *       {@code BlockEntityRenderDispatcher.render(BlockEntity, partial, PoseStack, MultiBufferSource)}.</li>
 *   <li>{@code org.lwjgl.input.Mouse} usage gone; LWJGL 3 polls via GLFW; need a custom drag tracker.</li>
 *   <li>{@code DxModelRenderer} ownership lives in Phase 7 - typed as {@code Object} here.</li>
 *   <li>{@code DynamXRenderUtils.gridMesh/arrowMeshX/Y/Z} are placeholder meshes (Phase 7).</li>
 * </ul>
 * Public constructor and confirm-button logic preserved; the render methods are stubbed.
 */
@ACsGuiFrame
public class GuiBlockCustomization extends GuiFrame {
    @ACsGuiFrame.RegisteredStyleSheet
    public static final ResourceLocation STYLE = new ResourceLocation(DynamXConstants.ID, "css/block_custom.css");

    private final Object model;
    private final TEDynamXBlock teBlock;
    private final GuiPanel preview;

    private final GuiFloatField translationX = new GuiFloatField(-10, 10);
    private final GuiFloatField translationY = new GuiFloatField(-10, 10);
    private final GuiFloatField translationZ = new GuiFloatField(-10, 10);
    private final GuiFloatField scaleX = new GuiFloatField(0.001f, 100);
    private final GuiFloatField scaleY = new GuiFloatField(0.001f, 100);
    private final GuiFloatField scaleZ = new GuiFloatField(0.001f, 100);
    private final GuiFloatField rotationX = new GuiFloatField(-360, 360);
    private final GuiFloatField rotationY = new GuiFloatField(-360, 360);
    private final GuiFloatField rotationZ = new GuiFloatField(-360, 360);

    private float angleX = -27, angleY = -18;
    private float scale = 20;
    private float targetScale = 0;

    public GuiBlockCustomization(TEDynamXBlock te) {
        super(new GuiScaler.Identity());
        this.teBlock = te;
        this.model = DynamXContext.getDxModelRegistry().getModel(te.getPackInfo().getModel());
        setCssClass("root");
        setPauseGame(false);

        preview = new GuiPanel() {
            @Override
            public void drawForeground(int mouseX, int mouseY, float partialTicks, ComponentRenderContext renderContext) {
                super.drawForeground(mouseX, mouseY, partialTicks, renderContext);
                // TODO port:1.20.1 - drawModelOnScreen body stubbed (PoseStack/MultiBufferSource pipeline).
            }
        };
        preview.setCssClass("preview");
        preview.addWheelListener(dWheel -> {
            float amount = dWheel / 80f;
            if (dWheel > 0) {
                amount = Mth.clamp(amount, 1.111111F, 10);
            } else {
                amount = Mth.clamp(amount, -10, -1.111111F);
                amount = -1 / amount;
            }
            targetScale = targetScale * amount;
            targetScale = Math.max(1, targetScale);
        });
        preview.addMoveListener(new IMouseMoveListener() {
            @Override
            public void onMouseMoved(int mouseX, int mouseY) {
                // TODO port:1.20.1 - LWJGL 3: Mouse.isButtonDown/getDX/getDY are gone, use GLFW input or
                // ACsGuis click-and-drag listeners to track angleX/angleY here.
                if (angleX >= 360) angleX = 0;
                if (angleY <= -360) angleY = 0;
            }

            @Override
            public void onMouseHover(int mouseX, int mouseY) {
            }

            @Override
            public void onMouseUnhover(int mouseX, int mouseY) {
            }
        });
        add(preview);

        GuiLabel rotationLabel = new GuiLabel("Rotation :");
        rotationLabel.setCssClass("rotation");
        GuiLabel scaleLabel = new GuiLabel("Scale :");
        scaleLabel.setCssClass("scale");
        GuiLabel translationLabel = new GuiLabel("Translation :");
        translationLabel.setCssClass("translation");

        translationX.setCssClass("translationX");
        translationX.setText(String.valueOf(teBlock.getRelativeTranslation().x));
        translationY.setCssClass("translationY");
        translationY.setText(String.valueOf(teBlock.getRelativeTranslation().y));
        translationZ.setCssClass("translationZ");
        translationZ.setText(String.valueOf(teBlock.getRelativeTranslation().z));

        scaleX.setCssClass("scaleX");
        scaleX.setText(teBlock.getRelativeScale().x != 0 ? String.valueOf(teBlock.getRelativeScale().x) : String.valueOf(1));
        scaleY.setCssClass("scaleY");
        scaleY.setText(teBlock.getRelativeScale().y != 0 ? String.valueOf(teBlock.getRelativeScale().y) : String.valueOf(1));
        scaleZ.setCssClass("scaleZ");
        scaleZ.setText(teBlock.getRelativeScale().z != 0 ? String.valueOf(teBlock.getRelativeScale().z) : String.valueOf(1));

        rotationX.setCssClass("rotationX");
        rotationX.setText(String.valueOf(teBlock.getRelativeRotation().x));
        rotationY.setCssClass("rotationY");
        rotationY.setText(String.valueOf(teBlock.getRelativeRotation().y));
        rotationZ.setCssClass("rotationZ");
        rotationZ.setText(String.valueOf(teBlock.getRelativeRotation().z));

        GuiLabel confirm = new GuiLabel("Confirm");
        confirm.setCssClass("confirm");

        confirm.addClickListener((mx, my, button) -> {
            Vector3f relativeTrans = new Vector3f(translationX.getValue(), translationY.getValue(), translationZ.getValue());
            Vector3f relativeScale = new Vector3f(scaleX.getValue(), scaleY.getValue(), scaleZ.getValue());
            Vector3f relativeRotation = new Vector3f(rotationX.getValue(), rotationY.getValue(), rotationZ.getValue());
            DynamXContext.getNetwork().sendToServer(new MessageSyncBlockCustomization(teBlock.getBlockPos(), relativeTrans, relativeScale, relativeRotation));
            teBlock.setRelativeTranslation(relativeTrans);
            teBlock.setRelativeScale(relativeScale);
            teBlock.setRelativeRotation(relativeRotation);
            teBlock.markCollisionsDirty(true);
        });

        add(rotationLabel);
        add(scaleLabel);
        add(translationLabel);
        add(translationX);
        add(translationY);
        add(translationZ);
        add(rotationX);
        add(rotationY);
        add(rotationZ);
        add(scaleX);
        add(scaleY);
        add(scaleZ);
        add(confirm);
    }

    public void updateScale() {
        scale = scale + (targetScale - scale) / 20;
        scale = Math.max(1, scale);
    }

    @Override
    public List<ResourceLocation> getCssStyles() {
        return Collections.singletonList(STYLE);
    }
}
