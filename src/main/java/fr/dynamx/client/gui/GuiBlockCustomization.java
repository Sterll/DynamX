package fr.dynamx.client.gui;

import com.jme3.math.Vector3f;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.network.packets.MessageSyncBlockCustomization;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Ecran de personnalisation 3D d'un bloc DynamX. Port 1.20.1 : remplace l'ancien
 * {@code GuiFrame} ACsGuis par un {@link Screen} vanilla a base de champs {@link EditBox}.
 * Le preview 3D est stubbe (a brancher quand le pipeline Phase 7 sera dispo).
 */
public class GuiBlockCustomization extends Screen {
    private final TEDynamXBlock teBlock;

    private EditBox translationX;
    private EditBox translationY;
    private EditBox translationZ;
    private EditBox scaleX;
    private EditBox scaleY;
    private EditBox scaleZ;
    private EditBox rotationX;
    private EditBox rotationY;
    private EditBox rotationZ;

    public GuiBlockCustomization(TEDynamXBlock te) {
        super(Component.literal("Block customization"));
        this.teBlock = te;
    }

    @Override
    protected void init() {
        super.init();
        int leftX = 8;
        int fieldW = 60;
        int y = 30;

        translationX = addField(leftX, y, fieldW, String.valueOf(teBlock.getRelativeTranslation().x));
        translationY = addField(leftX + 64, y, fieldW, String.valueOf(teBlock.getRelativeTranslation().y));
        translationZ = addField(leftX + 128, y, fieldW, String.valueOf(teBlock.getRelativeTranslation().z));

        y += 40;
        scaleX = addField(leftX, y, fieldW, teBlock.getRelativeScale().x != 0 ? String.valueOf(teBlock.getRelativeScale().x) : "1");
        scaleY = addField(leftX + 64, y, fieldW, teBlock.getRelativeScale().y != 0 ? String.valueOf(teBlock.getRelativeScale().y) : "1");
        scaleZ = addField(leftX + 128, y, fieldW, teBlock.getRelativeScale().z != 0 ? String.valueOf(teBlock.getRelativeScale().z) : "1");

        y += 40;
        rotationX = addField(leftX, y, fieldW, String.valueOf(teBlock.getRelativeRotation().x));
        rotationY = addField(leftX + 64, y, fieldW, String.valueOf(teBlock.getRelativeRotation().y));
        rotationZ = addField(leftX + 128, y, fieldW, String.valueOf(teBlock.getRelativeRotation().z));

        y += 40;
        addRenderableWidget(Button.builder(Component.literal("Confirm"), b -> applyChanges())
                .bounds(leftX, y, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(leftX + 96, y, 80, 20).build());
    }

    private EditBox addField(int x, int y, int w, String initial) {
        EditBox field = new EditBox(this.font, x, y, w, 18, Component.empty());
        field.setValue(initial);
        addRenderableWidget(field);
        return field;
    }

    private void applyChanges() {
        Vector3f relativeTrans = new Vector3f(parse(translationX), parse(translationY), parse(translationZ));
        Vector3f relativeScale = new Vector3f(parse(scaleX), parse(scaleY), parse(scaleZ));
        Vector3f relativeRotation = new Vector3f(parse(rotationX), parse(rotationY), parse(rotationZ));
        DynamXContext.getNetwork().sendToServer(new MessageSyncBlockCustomization(teBlock.getBlockPos(), relativeTrans, relativeScale, relativeRotation));
        teBlock.setRelativeTranslation(relativeTrans);
        teBlock.setRelativeScale(relativeScale);
        teBlock.setRelativeRotation(relativeRotation);
        teBlock.markCollisionsDirty(true);
        onClose();
    }

    private float parse(EditBox field) {
        try {
            return Float.parseFloat(field.getValue());
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);
        graphics.drawString(font, "Translation :", 8, 18, 0xFFFFFFFF);
        graphics.drawString(font, "Scale :", 8, 58, 0xFFFFFFFF);
        graphics.drawString(font, "Rotation :", 8, 98, 0xFFFFFFFF);
        // TODO port:1.20.1 - preview 3D (model rendering) a brancher quand BlockRenderDispatcher + DxModelRenderer sont prets.
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
