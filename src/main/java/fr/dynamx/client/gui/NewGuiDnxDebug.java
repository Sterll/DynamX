package fr.dynamx.client.gui;

import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Ecran de debug DynamX (entree de /dynamx debug_gui). Port 1.20.1 : remplace l'ancien
 * {@code GuiFrame} ACsGuis par un {@link Screen} vanilla. Trois sections : accueil (reload),
 * options de debug et lien vers les erreurs.
 */
public class NewGuiDnxDebug extends Screen {
    private static Panel activePanel = Panel.HOME;

    private Button reloadPacksBtn;
    private Button reloadModelsBtn;
    private Button reloadAllBtn;

    public NewGuiDnxDebug() {
        super(Component.literal("DynamX Debug"));
    }

    @Override
    protected void init() {
        super.init();
        int headerY = 8;
        int x = 8;
        addRenderableWidget(Button.builder(Component.literal("Home"), b -> {
            activePanel = Panel.HOME;
            rebuild();
        }).bounds(x, headerY, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Debug"), b -> {
            activePanel = Panel.DEBUG;
            rebuild();
        }).bounds(x + 64, headerY, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Errors"), b -> {
            Minecraft.getInstance().setScreen(new GuiLoadingErrors());
        }).bounds(x + 128, headerY, 60, 20).build());
        buildActive();
    }

    private void rebuild() {
        Minecraft.getInstance().setScreen(this);
    }

    private void buildActive() {
        int startY = 40;
        int x = 8;
        if (activePanel == Panel.HOME) {
            reloadPacksBtn = addRenderableWidget(Button.builder(Component.literal("Reload packs"), b -> {
                reloadPacksBtn.active = false;
                reloadPacksBtn.setMessage(Component.literal("Reloading..."));
                DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.CLIENT, DynamXLoadingTasks.PACK).thenAccept(empty -> {
                    reloadPacksBtn.active = true;
                    if (DynamXErrorManager.getErrorManager().hasErrors(DynamXErrorManager.INIT_ERRORS, DynamXErrorManager.PACKS_ERRORS))
                        reloadPacksBtn.setMessage(Component.literal(ChatFormatting.RED + "Some packs have errors"));
                    else
                        reloadPacksBtn.setMessage(Component.literal("Packs reloaded"));
                });
            }).bounds(x, startY, 160, 20).build());

            reloadModelsBtn = addRenderableWidget(Button.builder(Component.literal("Reload models"), b -> {
                reloadModelsBtn.active = false;
                reloadModelsBtn.setMessage(Component.literal("Reloading..."));
                DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.CLIENT, DynamXLoadingTasks.MODEL).thenAccept(empty -> {
                    reloadModelsBtn.active = true;
                    if (DynamXErrorManager.getErrorManager().hasErrors(DynamXErrorManager.MODEL_ERRORS))
                        reloadModelsBtn.setMessage(Component.literal(ChatFormatting.RED + "Some models have problems"));
                    else
                        reloadModelsBtn.setMessage(Component.literal("Models reloaded"));
                });
            }).bounds(x, startY + 24, 160, 20).build());

            reloadAllBtn = addRenderableWidget(Button.builder(Component.literal("Reload all"), b -> {
                reloadAllBtn.active = false;
                reloadAllBtn.setMessage(Component.literal("Reloading..."));
                DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.CLIENT, DynamXLoadingTasks.PACK, DynamXLoadingTasks.MODEL).thenAccept(empty -> {
                    reloadAllBtn.active = true;
                    if (DynamXErrorManager.getErrorManager().hasErrors(DynamXErrorManager.INIT_ERRORS, DynamXErrorManager.PACKS_ERRORS, DynamXErrorManager.MODEL_ERRORS))
                        reloadAllBtn.setMessage(Component.literal(ChatFormatting.RED + "Check the errors menu"));
                    else
                        reloadAllBtn.setMessage(Component.literal("Reloading finished"));
                });
            }).bounds(x, startY + 48, 160, 20).build());
            generateDebugCategory(DynamXDebugOptions.DebugCategories.HOME, startY + 80);
        } else {
            int y = startY;
            y = generateDebugCategory(DynamXDebugOptions.DebugCategories.GENERAL, y);
            y = generateDebugCategory(DynamXDebugOptions.DebugCategories.VEHICLES, y);
            generateDebugCategory(DynamXDebugOptions.DebugCategories.TERRAIN, y);
        }
    }

    private int generateDebugCategory(DynamXDebugOptions.DebugCategories category, int startY) {
        int x = 8;
        int y = startY;
        String subCategory = null;
        for (DynamXDebugOption option : category.getOptions()) {
            if (option.getSubCategory() != null && !option.getSubCategory().equals(subCategory)) {
                subCategory = option.getSubCategory();
                y += 14;
            }
            Checkbox box = new Checkbox(x, y, 200, 18, Component.literal(option.getDisplayName()), option.isActive()) {
                @Override
                public void onPress() {
                    super.onPress();
                    if (selected())
                        option.enable();
                    else
                        option.disable();
                }
            };
            addRenderableWidget(box);
            y += 22;
        }
        return y + 8;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawString(font, getTitle(), 8, this.height - 14, 0xFFAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public enum Panel {
        NONE, HOME, DEBUG
    }
}
