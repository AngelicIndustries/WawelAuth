package org.fentanylsolutions.wawelauth.api.skin;

/**
 * One draw of a player's skin layers, from {@link SkinLayers#begin}. Close it in the same call frame that opened it,
 * ideally with try-with-resources.
 */
public interface LayerDraw extends AutoCloseable {

    /**
     * Draws one overlay segment under the current GL transform, which must be the segment's part space (after the
     * host part's offset and {@code postRender}), with the player's skin bound. The draw decides 2D or 3D, applies the
     * wearer's visibility, the viewer's settings, armor and distance, and does nothing once suppressed.
     */
    void submit(LayerSegment segment);

    /** Stops this draw, and only this draw, from drawing anything else. For integrations that replace the model. */
    void suppress();

    boolean suppressed();

    @Override
    void close();
}
