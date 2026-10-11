package org.fentanylsolutions.wawelauth.client.skin;

import java.awt.image.BufferedImage;

/** Image helpers that need no AWT graphics environment. */
public final class SkinImages {

    private SkinImages() {}

    /** An independent ARGB copy, or null. */
    public static BufferedImage copy(BufferedImage image) {
        if (image == null) return null;
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        copy.setRGB(0, 0, width, height, image.getRGB(0, 0, width, height, null, 0, width), 0, width);
        return copy;
    }
}
