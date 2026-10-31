package industries.angelic.wawelauth.api.skin;

import java.awt.image.BufferedImage;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.api.SkinLayersHelper;
import industries.angelic.wawelauth.api.WawelTextureResolver;
import industries.angelic.wawelauth.client.skin.SkinSnapshots;
import industries.angelic.wawelauth.wawelclient.WawelClient;
import industries.angelic.wawelauth.wawelclient.data.ClientProvider;

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

    /**
     * The skin for a player head of this profile, from any trusted provider, or from Mojang in singleplayer. A profile
     * with only a name is found like any other bare name, and a {@code name@provider} through the server. Null while
     * that is looked up, for a profile without a skin, or with a default skin: the head then keeps what vanilla
     * resolved. Never blocks.
     */
    public static ResourceLocation skull(GameProfile profile) {
        if (profile == null) return null;
        WawelClient client = WawelClient.instance();
        if (client == null) return null;
        if (profile.getId() == null) {
            // Only the owner's name: found like any other bare name, or a name@provider through the server
            return profile.getName() == null ? null
                : client.getNameProfileResolver()
                    .skin(profile.getName());
        }
        ResourceLocation skin = null;
        List<ClientProvider> trusted = client.getSessionBridge()
            .getTrustedProviders();
        if (!trusted.isEmpty()) {
            skin = client.getTextureResolver()
                .getSkinFromAnyProvider(profile.getId(), profile.getName(), trusted);
        } else if (Minecraft.getMinecraft()
            .isSingleplayer()) {
                ClientProvider mojang = client.getMojangProvider();
                if (mojang != null) {
                    skin = client.getTextureResolver()
                        .getSkin(profile.getId(), profile.getName(), mojang, false);
                }
            }
        if (skin == null || skin.equals(WawelTextureResolver.getDefaultSkin())
            || skin.equals(WawelTextureResolver.getLegacyDefaultSkin())) {
            return null;
        }
        return skin;
    }

    /**
     * The image a loaded skin texture was uploaded with, such as a player head's: 64x64 or square HD, or vanilla's
     * 64x32 when no modern skin renderer is loaded. Null when the texture has none; not a copy, do not change it.
     */
    public static BufferedImage image(ResourceLocation texture) {
        return SkinSnapshots.image(texture);
    }
}
