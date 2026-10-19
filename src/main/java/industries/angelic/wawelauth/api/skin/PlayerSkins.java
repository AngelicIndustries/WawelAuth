package industries.angelic.wawelauth.api.skin;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.api.SkinLayersHelper;
import industries.angelic.wawelauth.client.skin.SkinSnapshots;

/** Client-side access to players' skins. Client thread only. */
@SideOnly(Side.CLIENT)
public final class PlayerSkins {

    private PlayerSkins() {}

    /** The player's current skin; never null. */
    public static PlayerSkin get(AbstractClientPlayer player) {
        return SkinSnapshots.get(player);
    }

    /**
     * Whether the wearer shows this part. This is live state: it follows the wearer's settings as the server reports
     * them and is not part of a {@link PlayerSkin} snapshot.
     */
    public static boolean isShown(EntityPlayer player, SkinPart part) {
        return !SkinLayersHelper.isPartHidden(player, part);
    }
}
