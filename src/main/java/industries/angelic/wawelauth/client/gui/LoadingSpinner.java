package industries.angelic.wawelauth.client.gui;

import java.util.function.BooleanSupplier;

import net.minecraft.client.gui.Gui;

import com.cleanroommc.modularui.api.drawable.IDrawable;

/**
 * A pixel arc that turns around a ring while something loads, in the style of Minecraft's own loading spinner, its
 * tail fading out. Drawn in code, one pixel per GUI unit, so it stays sharp at every GUI scale.
 */
final class LoadingSpinner {

    static final int SIZE = 7;

    /** The ring's pixels on a 7 by 7 grid, clockwise from the top middle. */
    private static final int[][] RING = { { 3, 0 }, { 4, 0 }, { 5, 1 }, { 6, 2 }, { 6, 3 }, { 6, 4 }, { 5, 5 },
        { 4, 6 }, { 3, 6 }, { 2, 6 }, { 1, 5 }, { 0, 4 }, { 0, 3 }, { 0, 2 }, { 1, 1 }, { 2, 0 } };
    /** Pixels in the turning arc, from its head to the faded end of its tail; the rest of the ring is the gap. */
    private static final int ARC = 11;
    private static final long STEP_MS = 60L;
    private static final int RGB = 0xFFFFFF;

    private LoadingSpinner() {}

    /** Draws the spinner in the middle of its area while {@code active} holds. */
    static IDrawable drawable(BooleanSupplier active) {
        return (context, x, y, width, height, widgetTheme) -> {
            if (active.getAsBoolean()) {
                draw(x + (width - SIZE) / 2, y + (height - SIZE) / 2);
            }
        };
    }

    static void draw(int x, int y) {
        int head = (int) (System.currentTimeMillis() / STEP_MS % RING.length);
        for (int i = 0; i < ARC; i++) {
            int[] pixel = RING[(head - i + RING.length) % RING.length];
            int alpha = 255 * (ARC - i) / ARC;
            Gui.drawRect(x + pixel[0], y + pixel[1], x + pixel[0] + 1, y + pixel[1] + 1, alpha << 24 | RGB);
        }
    }
}
