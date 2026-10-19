package industries.angelic.wawelauth.client.render;

import net.minecraft.client.resources.SkinManager;
import net.minecraft.util.ResourceLocation;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import industries.angelic.wawelauth.wawelclient.data.ClientProvider;

public interface IProviderAwareSkinManager {

    ResourceLocation wawelauth$loadTexture(MinecraftProfileTexture texture, Type textureType,
        SkinManager.SkinAvailableCallback callback, ClientProvider provider);

    void wawelauth$loadProfileTextures(GameProfile profile, SkinManager.SkinAvailableCallback callback,
        boolean requireSecure, ClientProvider provider);
}
