package industries.angelic.wawelauth.api.skin;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.client.render.skinlayers.SkinLayers3DConfig;

/** How this client draws other players' layers. Local settings, never synced. */
@SideOnly(Side.CLIENT)
public final class ViewerSkinSettings {

    private ViewerSkinSettings() {}

    /** Whether this viewer draws the part's layer as voxels where possible. */
    public static boolean volumetric(SkinPart part) {
        if (!SkinLayers3DConfig.enabled3D) return false;
        return switch (part) {
            case HAT -> SkinLayers3DConfig.enableHat3D;
            case JACKET -> SkinLayers3DConfig.enableJacket3D;
            case LEFT_SLEEVE -> SkinLayers3DConfig.enableLeftSleeve3D;
            case RIGHT_SLEEVE -> SkinLayers3DConfig.enableRightSleeve3D;
            case LEFT_PANTS -> SkinLayers3DConfig.enableLeftPants3D;
            case RIGHT_PANTS -> SkinLayers3DConfig.enableRightPants3D;
            case CAPE -> false;
        };
    }

    /** Beyond this many blocks, layers are drawn flat. */
    public static int lodDistance() {
        return SkinLayers3DConfig.renderDistanceLOD;
    }

    /** Whether a layer covered by armor is hidden instead of drawn flat. */
    public static boolean hideOverlayUnderArmor() {
        return SkinLayers3DConfig.hideOverlayArmor;
    }
}
