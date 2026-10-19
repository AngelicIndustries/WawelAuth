package industries.angelic.wawelauth.client.skin;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.api.skin.SkinPart;
import industries.angelic.wawelauth.client.render.skinlayers.SkinLayersConfig;

/**
 * The local player's own part visibility, as chosen in Skin Customization. The cape is vanilla's "Show Cape" option,
 * so vanilla servers honor it too; the other parts live in the WA config.
 */
@SideOnly(Side.CLIENT)
public final class WearerSkinSettings {

    private WearerSkinSettings() {}

    public static boolean isHidden(SkinPart part) {
        return switch (part) {
            case CAPE -> !Minecraft.getMinecraft().gameSettings.showCape;
            case JACKET -> !SkinLayersConfig.enableJacket;
            case LEFT_SLEEVE -> !SkinLayersConfig.enableLeftSleeve;
            case RIGHT_SLEEVE -> !SkinLayersConfig.enableRightSleeve;
            case LEFT_PANTS -> !SkinLayersConfig.enableLeftPants;
            case RIGHT_PANTS -> !SkinLayersConfig.enableRightPants;
            case HAT -> !SkinLayersConfig.enableHat;
        };
    }

    public static void setHidden(SkinPart part, boolean hidden) {
        switch (part) {
            case CAPE -> Minecraft.getMinecraft().gameSettings.showCape = !hidden;
            case JACKET -> SkinLayersConfig.enableJacket = !hidden;
            case LEFT_SLEEVE -> SkinLayersConfig.enableLeftSleeve = !hidden;
            case RIGHT_SLEEVE -> SkinLayersConfig.enableRightSleeve = !hidden;
            case LEFT_PANTS -> SkinLayersConfig.enableLeftPants = !hidden;
            case RIGHT_PANTS -> SkinLayersConfig.enableRightPants = !hidden;
            case HAT -> SkinLayersConfig.enableHat = !hidden;
        }
    }
}
