package industries.angelic.wawelauth.api.skin;

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

    /**
     * Stops this draw from drawing anything else, along with draws opened inside it for the same entity. Other draws
     * are unaffected. For integrations that replace the model, or draw the arm without its sleeve.
     */
    void suppress();

    boolean suppressed();

    /**
     * Takes over one part's layers: the code that opened this draw leaves the part out of its own layer pass, while
     * {@link #submit} still draws it. For integrations that replace a part's overlay geometry and submit its
     * vanilla-shaped pieces on their own bones. Claiming the hat lets the vanilla hat part draw again, so the
     * replacement can route it. Draws opened inside this one are unaffected.
     */
    void claim(SkinPart part);

    boolean claimed(SkinPart part);

    @Override
    void close();
}
