package fr.dynamx.client.gui;

import com.jme3.math.FastMath;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.aym.acsguis.utils.ComponentRenderContext;
import fr.dynamx.common.items.tools.ItemWrench;
import fr.dynamx.common.items.tools.WrenchMode;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Radial wrench-mode selector overlay.
 *
 * <p>TODO port:1.20.1 - the disk + sector drawing used immediate-mode {@code GL_TRIANGLE_FAN/GL_LINE_STRIP} via
 * {@code GlStateManager.glBegin/glVertex2f}. All of this is gone in core profile; the replacement requires
 * a {@code Tesselator} with a {@code BufferBuilder} setup against {@code DefaultVertexFormat.POSITION_COLOR}
 * (or a custom {@code RenderType}) and a shader. We keep the API surface and the mode-finder math but
 * stub the actual {@code drawDisk} call.</p>
 *
 * <p>Other API changes:</p>
 * <ul>
 *   <li>{@code I18n.format} -> {@link I18n#get(String, Object...)}.</li>
 *   <li>{@code mc.player.getHeldItemMainhand()} -> {@code mc.player.getMainHandItem()}.</li>
 *   <li>{@code Minecraft.getMinecraft().displayGuiScreen} -> {@code Minecraft.getInstance().setScreen}.</li>
 *   <li>{@code mc.fontRenderer.getStringWidth/FONT_HEIGHT} -> {@code mc.font.width/lineHeight}.</li>
 * </ul>
 */
public class GuiWrenchSelection extends GuiFrame {
    private final List<String> infos = new ArrayList<>();
    private WrenchMode currentMode;

    public GuiWrenchSelection() {
        super(new GuiScaler.Identity());
        setPauseGame(false);

        addClickListener((mouseX1, mouseY1, mouseButton1) -> {
            WrenchMode wrenchMode = getModeWithMousePos(mouseX1, mouseY1);
            WrenchMode.sendWrenchMode(wrenchMode);
            Minecraft.getInstance().setScreen(null);
        });

        ItemStack itemStack = mc.player.getMainHandItem();
        if (itemStack.getItem() instanceof ItemWrench) {
            currentMode = WrenchMode.getCurrentMode(itemStack);
        }
    }

    /**
     * <p>TODO port:1.20.1 - body fully stubbed. Replace the immediate-mode disk + sector draws with
     * {@code Tesselator.getInstance()} + {@code BufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, ...)}
     * inside a {@code GuiGraphics} pose stack.</p>
     */
    public static void drawDisk(float x, float y, float innerRadius, float outerRadius, Color color, float alpha) {
        // TODO port:1.20.1 - re-implement using Tesselator + DefaultVertexFormat.POSITION_COLOR + GameRenderer.getPositionColorShader().
    }

    @Override
    public void drawBackground(int mouseX, int mouseY, float partialTicks, ComponentRenderContext renderContext) {
        super.drawBackground(mouseX, mouseY, partialTicks, renderContext);
        drawDisk(getWidth() / 2f, getHeight() / 2f, 110, 60, Color.BLACK, 0.5f);

        infos.clear();
        WrenchMode wrenchMode = getModeWithMousePos(mouseX, mouseY);
        infos.add(I18n.get(wrenchMode.getLabel()));
        // TODO port:1.20.1 - GuiAPIClientHelper.drawHoveringText replaced with GuiGraphics.renderTooltip.

        // TODO port:1.20.1 - text overlay stubbed. Replace with GuiGraphics.drawString + Pose scale.
    }

    private WrenchMode getModeWithMousePos(int mouseX, int mouseY) {
        float mx = mouseX - getWidth() / 2;
        float my = mouseY - getHeight() / 2;
        int maxModes = WrenchMode.getWrenchModes().size() - 1;
        double theta = FastMath.atan2(my, mx);
        theta += FastMath.PI / maxModes;
        theta += FastMath.PI;
        theta = theta % (FastMath.PI * 2);

        int mode = (int) (theta / (2 * FastMath.PI / maxModes));

        return WrenchMode.getWrenchModes().get(mode == 5 ? 6 : maxModes - mode - 1);
    }

    @Override
    public List<ResourceLocation> getCssStyles() {
        return Collections.singletonList(new ResourceLocation(DynamXConstants.ID, "css/wrench_selection.css"));
    }

    @Override
    public boolean needsCssReload() {
        return true;
    }
}
