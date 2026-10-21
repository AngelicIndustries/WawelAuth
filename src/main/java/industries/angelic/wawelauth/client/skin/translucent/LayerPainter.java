package industries.angelic.wawelauth.client.skin.translucent;

import industries.angelic.wawelauth.api.skin.LayerSegment;

/** Paints one layer segment under the current GL state, flat or as voxels. */
public interface LayerPainter {

    void paint(LayerSegment segment, boolean volumetric);
}
