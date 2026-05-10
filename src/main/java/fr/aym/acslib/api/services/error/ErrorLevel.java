// TODO port:1.20.1 stub - ErrorLevel enum
package fr.aym.acslib.api.services.error;

import net.minecraft.ChatFormatting;

public enum ErrorLevel {
    ADVICE("\u00a77", ChatFormatting.GRAY),
    LOW("\u00a7e", ChatFormatting.YELLOW),
    NORMAL("\u00a76", ChatFormatting.GOLD),
    HIGH("\u00a7c", ChatFormatting.RED),
    FATAL("\u00a74", ChatFormatting.DARK_RED);

    public final String colorCode;
    public final ChatFormatting color;

    ErrorLevel(String colorCode, ChatFormatting color) {
        this.colorCode = colorCode;
        this.color = color;
    }

    public String getColorCode() {
        return colorCode;
    }
}
