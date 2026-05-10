package fr.dynamx.utils.debug;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.awt.*;

/**
 * <p>TODO port:1.20.1 - {@code FontRenderer#drawString} and {@code Gui.drawRect} are gone;
 * the inner {@link Measure} now expects a {@link GuiGraphics} (provided by render events) instead
 * of a raw {@code FontRenderer}.</p>
 */
public class ProfilingData {
    private final Profiler.Profiles profileIn;

    private long max, lastDelta;
    private long delta;
    private long medium;
    private int measureCount;

    private long startTime;

    public ProfilingData(Profiler.Profiles profileIn) {
        this.profileIn = profileIn;
    }

    public void start() {
        if (startTime != 0)
            throw new IllegalStateException("Profiling of " + profileIn + " is already started !");
        startTime = System.currentTimeMillis();
    }

    public void end() {
        if (startTime == 0)
            throw new IllegalStateException("Profiling of " + profileIn + " is not started !");
        delta += System.currentTimeMillis() - startTime;
        startTime = 0;
    }

    public boolean isEmpty() {
        return medium == 0;
    }

    public void update() {
        if (startTime != 0)
            throw new IllegalStateException("Profiling of " + profileIn + " is started : cannot update it !");
        if (delta > max)
            max = delta;
        medium = (measureCount * medium + delta) / (measureCount + 1);
        lastDelta = delta;
        delta = 0;
        measureCount += 1;

        if (measureCount > 100)
            reset();
    }

    public void reset() {
        if (startTime != 0)
            throw new IllegalStateException("Profiling of " + profileIn + " is started : cannot reset it !");
        delta = 0;
        medium = 0;
        measureCount = 0;
        max = 0;
    }

    @Override
    public String toString() {
        return "ProfilingData " + profileIn.name() + " : average= " + medium + " ms, max= " + max + " ms on " + measureCount + " measures";
    }

    public Measure save() {
        return new Measure(max, medium, lastDelta);
    }

    public static class Measure {
        private final long max, lastDelta;
        private final long medium;

        public Measure(long max, long medium, long lastDelta) {
            this.max = max;
            this.medium = medium;
            this.lastDelta = lastDelta;
        }

        @OnlyIn(Dist.CLIENT)
        public void draw(GuiGraphics graphics, int x, int bottom, Font font, int count) {
            // TODO port:1.20.1 - FontRenderer.drawString -> GuiGraphics.drawString(font, text, x, y, color)
            bottom -= 9;
            graphics.drawString(font, String.valueOf(max), x, bottom, count % 2 == 0 ? Color.CYAN.getRGB() : Color.ORANGE.getRGB(), false);
            bottom -= 2;

            bottom -= 9;
            graphics.drawString(font, String.valueOf(medium), x, bottom, count % 2 == 0 ? Color.BLUE.getRGB() : Color.RED.getRGB(), false);
            bottom -= 2;

            bottom -= 9;
            graphics.drawString(font, String.valueOf(lastDelta), x, bottom, count % 2 == 0 ? Color.GREEN.getRGB() : Color.MAGENTA.getRGB(), false);
            bottom -= 2;
            drawBar(graphics, x, bottom, (int) lastDelta, count % 2 == 0 ? Color.GREEN.getRGB() : Color.MAGENTA.getRGB());
        }

        @OnlyIn(Dist.CLIENT)
        private void drawBar(GuiGraphics graphics, int x, int bottom, int height, int color) {
            // TODO port:1.20.1 - Gui.drawRect -> GuiGraphics.fill(x1, y1, x2, y2, color)
            for (int i = 0; i < height; i++) {
                graphics.fill(x, bottom - 1, x + 10, bottom, color);
                bottom -= 2;
            }
        }
    }
}
