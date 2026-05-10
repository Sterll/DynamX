// TODO port:1.20.1 stub - ACsGuiApi static facade
package fr.aym.acsguis.api;

import fr.aym.acslib.api.services.error.ErrorCategory;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class ACsGuiApi {
    private static final ErrorCategory CSS_ERRORS = new ErrorCategory();

    public static ErrorCategory getCssErrorType() { return CSS_ERRORS; }

    public static void asyncLoadThenShowGui(String name, Supplier<?> supplier) {
        // TODO port:1.20.1 stub - no-op
    }

    public static void asyncLoadThenShowHudGui(String name, Supplier<?> supplier) {
        // TODO port:1.20.1 stub - no-op
    }

    public static void closeHudGui(Class<?> clazz) {
        // TODO port:1.20.1 stub
    }

    public static void reloadCssStyles(Object o) {
        // TODO port:1.20.1 stub
    }

    public static void registerStyleSheet(ResourceLocation loc) {
        // TODO port:1.20.1 stub
    }
}
