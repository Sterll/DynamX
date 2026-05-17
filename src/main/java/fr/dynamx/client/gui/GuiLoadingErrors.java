package fr.dynamx.client.gui;

import fr.aym.acslib.api.services.error.ErrorData;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.aym.acslib.api.services.error.LocatedErrorList;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.LogicalSide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Ecran de navigation des erreurs de chargement DynamX. Port 1.20.1 : remplace l'ancien
 * {@code GuiFrame} ACsGuis par un {@link Screen} vanilla. Les erreurs sont resumees sous forme
 * de liste textuelle, le detail s'affiche au clic sur une categorie.
 */
public class GuiLoadingErrors extends Screen {
    private ResourceLocation selectedCategory;
    private final List<String> lines = new ArrayList<>();
    private int scroll = 0;

    public GuiLoadingErrors() {
        super(Component.literal("DynamX errors"));
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
            if (selectedCategory != null) {
                selectedCategory = null;
                rebuildLines();
            } else {
                onClose();
            }
        }).bounds(this.width - 70, 8, 60, 20).build());
        DynamXErrorManager.printErrors(LogicalSide.CLIENT, ErrorLevel.ADVICE);
        rebuildLines();
    }

    private void rebuildLines() {
        lines.clear();
        scroll = 0;
        Map<ResourceLocation, LocatedErrorList> allErrors = DynamXErrorManager.getErrorManager().getAllErrors();
        if (selectedCategory == null) {
            lines.add(ChatFormatting.DARK_AQUA + "Errors while loading DynamX and the content packs");
            lines.add(ChatFormatting.GRAY + "Click on any category to view it, press escape to go back");
            if (allErrors.isEmpty()) {
                lines.add("No error found here");
                return;
            }
            allErrors.entrySet().stream()
                    .sorted(Comparator.comparingInt(e -> -e.getValue().getHighestErrorLevel().ordinal()))
                    .forEachOrdered(entry -> {
                        LocatedErrorList list = entry.getValue();
                        StringBuilder title = new StringBuilder(list.getHighestErrorLevel().color.toString())
                                .append(entry.getKey()).append(" : ");
                        appendCounts(title, list.getErrors());
                        lines.add("+ " + title);
                    });
        } else {
            LocatedErrorList list = allErrors.get(selectedCategory);
            lines.add(ChatFormatting.DARK_AQUA + "Errors while loading " + selectedCategory);
            if (list == null) {
                lines.add("No error in this category");
                return;
            }
            for (ErrorData err : list.getErrors()) {
                lines.add(err.getLevel().color + "[" + err.getLevel() + "] " + err.getObject() + " : " + err.getGenericType());
            }
        }
    }

    private void appendCounts(StringBuilder out, java.util.Collection<ErrorData> errors) {
        appendIf(out, errors, ErrorLevel.FATAL, " fatal error(s) ");
        appendIf(out, errors, ErrorLevel.HIGH, " error(s) ");
        appendIf(out, errors, ErrorLevel.LOW, " warning(s) ");
        appendIf(out, errors, ErrorLevel.ADVICE, " advice(s) ");
    }

    private void appendIf(StringBuilder out, java.util.Collection<ErrorData> errors, ErrorLevel level, String label) {
        long count = errors.stream().filter(er -> er.getLevel() == level).count();
        if (count > 0)
            out.append(level.color).append(count).append(label);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && selectedCategory == null) {
            int idx = clickedLineIndex(mouseY);
            if (idx >= 0) {
                Map<ResourceLocation, LocatedErrorList> allErrors = DynamXErrorManager.getErrorManager().getAllErrors();
                List<ResourceLocation> ordered = allErrors.entrySet().stream()
                        .sorted(Comparator.comparingInt(e -> -e.getValue().getHighestErrorLevel().ordinal()))
                        .map(Map.Entry::getKey)
                        .toList();
                int categoryIdx = idx - 2;
                if (categoryIdx >= 0 && categoryIdx < ordered.size()) {
                    selectedCategory = ordered.get(categoryIdx);
                    rebuildLines();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int clickedLineIndex(double mouseY) {
        int firstY = 40;
        int line = (int) ((mouseY - firstY) / (font.lineHeight + 2)) + scroll;
        if (line < 0 || line >= lines.size())
            return -1;
        return line;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, scroll - (int) Math.signum(delta));
        scroll = Math.min(Math.max(0, lines.size() - 1), scroll);
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);
        graphics.drawString(font, getTitle(), 8, 12, 0xFFFFFFFF);
        int y = 40;
        for (int i = scroll; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), 8, y, 0xFFFFFFFF, false);
            y += font.lineHeight + 2;
            if (y > this.height - 24)
                break;
        }
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            if (selectedCategory != null) {
                selectedCategory = null;
                rebuildLines();
                return true;
            } else if (Minecraft.getInstance().level != null) {
                Minecraft.getInstance().setScreen(new NewGuiDnxDebug());
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
