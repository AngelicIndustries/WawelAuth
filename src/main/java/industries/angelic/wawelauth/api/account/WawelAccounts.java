package industries.angelic.wawelauth.api.account;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.wawelclient.BuiltinProviders;
import industries.angelic.wawelauth.wawelclient.WawelClient;
import industries.angelic.wawelauth.wawelclient.data.ClientAccount;
import industries.angelic.wawelauth.wawelcore.data.SkinModel;
import industries.angelic.wawelauth.wawelcore.data.TextureType;

/** Client-side account access for integrations. Client thread only. */
@SideOnly(Side.CLIENT)
public final class WawelAccounts {

    private WawelAccounts() {}

    /** The account the client is playing with, or null. */
    public static AccountInfo active() {
        WawelClient client = WawelClient.instance();
        if (client == null) return null;
        ClientAccount account = client.getSessionBridge()
            .getActiveAccount();
        if (account == null) return null;
        return new AccountInfo(
            account.getId(),
            account.getProfileName(),
            account.getProfileUuid(),
            account.getProviderName(),
            BuiltinProviders.isOfflineProvider(account.getProviderName()));
    }

    /**
     * Uploads a skin for an account, or for an offline account stores it as the account's local skin. The future
     * completes on the client thread with the provider's message. On success WawelAuth drops its cached textures for
     * the profile, so {@link industries.angelic.wawelauth.api.skin.PlayerSkins} reports the new skin under a new
     * revision once it has loaded.
     */
    public static CompletableFuture<String> uploadSkin(AccountInfo account, BufferedImage image, SkinModel model) {
        CompletableFuture<String> result = new CompletableFuture<>();
        WawelClient client = WawelClient.instance();
        if (client == null || account == null || image == null || model == null) {
            result.completeExceptionally(new IllegalArgumentException("Client, account, image and model are required"));
            return result;
        }

        File file;
        try {
            file = account.offline() ? offlineSkinFile(client, account)
                : File.createTempFile("wawelauth-skin-", ".png");
            if (!ImageIO.write(image, "png", file)) {
                throw new IOException("No PNG writer available");
            }
        } catch (IOException e) {
            result.completeExceptionally(e);
            return result;
        }

        final File uploaded = file;
        client.getAccountManager()
            .uploadTexture(account.id(), TextureType.SKIN, uploaded, model == SkinModel.SLIM)
            .whenComplete((message, error) -> {
                // Offline accounts keep pointing at their file; online uploads are done with it.
                if (!account.offline()) uploaded.delete();
                Minecraft.getMinecraft()
                    .func_152344_a(() -> {
                        if (error != null) {
                            result.completeExceptionally(error);
                            return;
                        }
                        if (account.profileUuid() != null) {
                            client.getTextureResolver()
                                .invalidate(account.profileUuid());
                        }
                        result.complete(message);
                    });
            });
        return result;
    }

    private static File offlineSkinFile(WawelClient client, AccountInfo account) throws IOException {
        File directory = new File(client.getDataDir(), "offline-skins");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Cannot create " + directory);
        }
        String name = account.profileUuid() != null ? account.profileUuid()
            .toString() : Long.toString(account.id());
        return new File(directory, name + ".png");
    }
}
