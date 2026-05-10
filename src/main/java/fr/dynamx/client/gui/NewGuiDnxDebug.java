package fr.dynamx.client.gui;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.aym.acsguis.api.ACsGuiFrame;
import fr.aym.acsguis.component.button.GuiCheckBox;
import fr.aym.acsguis.component.layout.GridLayout;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.panel.GuiScrollPane;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.aym.acsguis.cssengine.positionning.Size;
import fr.aym.acsguis.event.listeners.mouse.IMouseMoveListener;
import fr.aym.acsguis.utils.GuiConstants;
import fr.aym.acsguis.utils.GuiCssError;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom debug GUI (entry point for /dynamx debug_gui).
 *
 * <p>TODO port:1.20.1 - mostly ACsGuis bindings:</p>
 * <ul>
 *   <li>{@code TextFormatting} -> {@code ChatFormatting}.</li>
 *   <li>{@code Minecraft.getMinecraft().addScheduledTask} -> {@code Minecraft.getInstance().execute}.</li>
 *   <li>{@code Minecraft.getMinecraft().displayGuiScreen} -> {@code Minecraft.getInstance().setScreen}.</li>
 *   <li>{@code net.minecraft.util.ResourceLocation} -> {@code net.minecraft.resources.ResourceLocation}.</li>
 * </ul>
 */
@ACsGuiFrame
public class NewGuiDnxDebug extends GuiFrame {
    private final GuiPanel homePanel;
    private final GuiLabel homeButton;

    private final GuiScrollPane debugPanel;
    private final GuiLabel debugButton;

    private static Panel activePanel = Panel.NONE;

    @ACsGuiFrame.RegisteredStyleSheet
    public static final ResourceLocation STYLE = new ResourceLocation(DynamXConstants.ID, "css/new_dnx_debug.css");

    public NewGuiDnxDebug() {
        super(new GuiScaler.Identity());
        setCssId("home");

        GuiScrollPane header = new GuiScrollPane();
        header.setCssId("header");
        header.setLayout(new GridLayout(new Size.SizeValue(60, GuiConstants.ENUM_SIZE.ABSOLUTE),
                new Size.SizeValue(1, GuiConstants.ENUM_SIZE.RELATIVE), new Size.SizeValue(0, GuiConstants.ENUM_SIZE.RELATIVE), GridLayout.GridDirection.VERTICAL, 1));
        add(header);

        header.add((homeButton = new GuiLabel("Home")).addClickListener((mx, my, button) -> {
            if (button == 0)
                setHomePanel();
        }));
        header.add((debugButton = new GuiLabel("Debug")).addClickListener((mx, my, button) -> {
            if (button == 0)
                setDebugPanel();
        }));
        header.add(new GuiLabel("Errors").addClickListener((mx, my, button) -> {
            if (button == 0)
                ACsGuiApi.asyncLoadThenShowGui("LoadingErrors", GuiLoadingErrors::new);
        }));
        header.add(new GuiLabel("Css").addClickListener((mx, my, button) -> {
            if (button == 0)
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new GuiCssError().getGuiScreen()));
        }));

        homePanel = new GuiPanel();
        homePanel.setCssId("homePanel").setCssClass("debug_menu_panel");
        homePanel.setLayout(GridLayout.columnLayout(20, 5));
        generateDebugCategory(homePanel, DynamXDebugOptions.DebugCategories.HOME);
        GuiLabel box = new GuiLabel("Reload packs");
        box.setCssId("reload_packs").setCssClass("reload_button");
        box.addClickListener((x, y, bu) -> {
            box.setEnabled(false);
            box.setText("Reloading...");
            DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.CLIENT, DynamXLoadingTasks.PACK).thenAccept(empty -> {
                box.setEnabled(true);
                if (DynamXErrorManager.getErrorManager().hasErrors(DynamXErrorManager.INIT_ERRORS, DynamXErrorManager.PACKS_ERRORS))
                    box.setText(ChatFormatting.RED + "Some packs have errors");
                else
                    box.setText("Packs reloaded");
            });
        });
        homePanel.add(box);
        GuiLabel box2 = new GuiLabel("Reload models");
        box2.setCssId("reload_models").setCssClass("reload_button");
        box2.addClickListener((x, y, bu) -> {
            box2.setEnabled(false);
            box2.setText("Reloading...");
            DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.CLIENT, DynamXLoadingTasks.MODEL).thenAccept(empty -> {
                box2.setEnabled(true);
                if (DynamXErrorManager.getErrorManager().hasErrors(DynamXErrorManager.MODEL_ERRORS))
                    box2.setText(ChatFormatting.RED + "Some models have problems");
                else
                    box2.setText("Models reloaded");
            });
        });
        homePanel.add(box2);
        GuiLabel box3 = new GuiLabel("Reload css styles");
        box3.setCssId("reload_css").setCssClass("reload_button");
        box3.addClickListener((x, y, bu) -> {
            box3.setEnabled(false);
            box3.setText("Reloading...");
            DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.CLIENT, DynamXLoadingTasks.CSS).thenAccept(empty -> {
                box3.setEnabled(true);
                if (DynamXErrorManager.getErrorManager().hasErrors(ACsGuiApi.getCssErrorType()))
                    box3.setText(ChatFormatting.RED + "Some css styles have errors");
                else
                    box3.setText("Css styles reloaded");
            });
        });
        homePanel.add(box3);
        GuiLabel box4 = new GuiLabel("Reload all");
        box4.setCssId("reload_all").setCssClass("reload_button");
        box4.addClickListener((x, y, bu) -> {
            box4.setEnabled(false);
            box4.setText("Reloading...");
            DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.CLIENT, DynamXLoadingTasks.PACK, DynamXLoadingTasks.MODEL, DynamXLoadingTasks.CSS).thenAccept(empty -> {
                box4.setEnabled(true);
                if (DynamXErrorManager.getErrorManager().hasErrors(DynamXErrorManager.INIT_ERRORS, DynamXErrorManager.PACKS_ERRORS, DynamXErrorManager.MODEL_ERRORS, ACsGuiApi.getCssErrorType()))
                    box4.setText(ChatFormatting.RED + "Check the errors menu");
                else
                    box4.setText("Reloading finished");
            });
        });
        homePanel.add(box4);

        debugPanel = new GuiScrollPane();
        debugPanel.setCssId("debugPanel").setCssClass("debug_menu_panel");
        debugPanel.setLayout(GridLayout.columnLayout(20, 2));
        generateDebugCategory(debugPanel, DynamXDebugOptions.DebugCategories.GENERAL);
        generateDebugCategory(debugPanel, DynamXDebugOptions.DebugCategories.VEHICLES);
        generateDebugCategory(debugPanel, DynamXDebugOptions.DebugCategories.TERRAIN);

        switch (activePanel) {
            case DEBUG:
                activePanel = Panel.NONE;
                setDebugPanel();
                break;
            case HOME:
                activePanel = Panel.NONE;
            default:
                setHomePanel();
        }

        setEnableDebugPanel(true);
    }

    @Override
    public List<ResourceLocation> getCssStyles() {
        return Collections.singletonList(STYLE);
    }

    @Override
    public boolean doesPauseGame() {
        return false;
    }

    @Override
    public boolean needsCssReload() {
        return false;
    }

    protected void setHomePanel() {
        switch (activePanel) {
            case HOME:
                break;
            case DEBUG:
                remove(debugPanel);
            default:
                add(homePanel);
                homeButton.setCssClass("header_selected");
                debugButton.setCssClass("");
                activePanel = Panel.HOME;
                break;
        }
    }

    protected void setDebugPanel() {
        switch (activePanel) {
            case DEBUG:
                break;
            case HOME:
                remove(homePanel);
            default:
                add(debugPanel);
                debugButton.setCssClass("header_selected");
                homeButton.setCssClass("");
                activePanel = Panel.DEBUG;
                break;
        }
    }

    protected void generateDebugCategory(GuiPanel debugPanel, DynamXDebugOptions.DebugCategories category) {
        String subCategory = null;
        Map<DynamXDebugOption, GuiCheckBox> terrainButtons = new HashMap<>();
        for (DynamXDebugOption option : category.getOptions()) {
            if (option.getSubCategory() != null && !option.getSubCategory().equals(subCategory)) {
                subCategory = option.getSubCategory();
                GuiLabel label1 = new GuiLabel(subCategory + " :");
                debugPanel.add(label1.setCssClass("option-subcategory"));
            }
            GuiPanel line = new GuiPanel();
            line.setCssClass("option-desc");
            boolean active = option.isActive();

            GuiCheckBox b1 = new GuiCheckBox(option.getDisplayName());
            b1.setCheckedSymbol("");
            if (option.getDescription() != null)
                b1.setHoveringText(Collections.singletonList(option.getDescription()));
            b1.setChecked(active);
            terrainButtons.put(option, b1);
            DynamXDebugOptions.DebugCategories finalCategory = category;
            line.add(b1.setCssClass("switch-button-chk-" + (active ? "active" : "inactive")).addClickListener((mx, my, button) -> {
                if (!b1.isChecked())
                    option.disable();
                else
                    option.enable();
                for (DynamXDebugOption noption : finalCategory.getOptions()) {
                    boolean nactive = noption.isActive();
                    terrainButtons.get(noption).setChecked(nactive);
                    terrainButtons.get(noption).setCssClass("switch-button-chk-" + (nactive ? "active" : "inactive"));
                }
            }));
            if (option.serverRequestMask() == 0) {
                b1.addMoveListener(new IMouseMoveListener() {
                    @Override
                    public void onMouseMoved(int i, int i1) {
                    }

                    @Override
                    public void onMouseHover(int i, int i1) {
                        if (!b1.isChecked())
                            option.enable();
                        else option.disable();
                    }

                    @Override
                    public void onMouseUnhover(int i, int i1) {
                        if (!b1.isChecked())
                            option.disable();
                        else option.enable();
                    }
                });
            }
            debugPanel.add(line);
        }
    }

    public enum Panel {
        NONE, HOME, DEBUG
    }
}
