package org.fentanylsolutions.wawelauth.client.skin.sync;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import org.fentanylsolutions.wawelauth.WawelAuth;
import org.fentanylsolutions.wawelauth.api.SkinLayersHelper;
import org.fentanylsolutions.wawelauth.api.skin.SkinPart;
import org.fentanylsolutions.wawelauth.client.gui.PlayerPreviewEntity;
import org.fentanylsolutions.wawelauth.client.skin.WearerSkinSettings;
import org.fentanylsolutions.wawelauth.packet.PacketHandler;
import org.fentanylsolutions.wawelauth.packet.SetSkinPartsPacket;
import org.fentanylsolutions.wawelauth.packet.UpdateSkinLayersPacket;
import org.fentanylsolutions.wawelauth.wawelcore.skin.SkinPartFlags;
import org.fentanylsolutions.wawelauth.wawelcore.skin.SkinPartLayout;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client side of the skin part protocol for the current connection.
 * <p>
 * The server's generation decides how slot 16 is read and what is sent:
 * <ul>
 * <li>announced v2 on {@link SkinPartFlags#CHANNEL}: v2 layout, parts sent with {@link SetSkinPartsPacket}</li>
 * <li>WA in the server's mod list but no announcement: an older WA, layout guessed from its version</li>
 * <li>neither: a server without WA, only vanilla's cape bit means anything</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class ClientSkinSync {

    private static volatile String serverWawelVersion;
    private static volatile int serverProtocol;

    private static boolean registered;

    private ClientSkinSync() {}

    public static synchronized void register() {
        if (registered) return;
        SkinLayersHelper.setPartReader(ClientSkinSync::isHidden);
        FMLCommonHandler.instance()
            .bus()
            .register(new ClientSkinSync());
        registered = true;
    }

    /** From the FML handshake. Null when the server does not run WA. */
    public static void onServerModList(String wawelVersion) {
        serverWawelVersion = wawelVersion;
    }

    /** From the server's announcement on {@link SkinPartFlags#CHANNEL}. */
    public static void onServerProtocol(byte[] payload) {
        if (payload == null || payload.length < 1) return;
        serverProtocol = payload[0] & 0xFF;
        WawelAuth.debug("[SkinParts] Server announced protocol " + serverProtocol);
        sendPartsToServer();
    }

    public static SkinPartLayout currentLayout() {
        if (serverProtocol >= SkinPartFlags.PROTOCOL_VERSION) return SkinPartLayout.V2;
        String version = serverWawelVersion;
        if (version != null) return SkinPartLayout.forLegacyVersion(version);
        return SkinPartLayout.VANILLA;
    }

    /** Sends the local choices in the form the current server understands. Called after every settings send. */
    public static void sendPartsToServer() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        SkinPartLayout layout = currentLayout();
        if (layout == SkinPartLayout.V2) {
            PacketHandler.sendToServer(new SetSkinPartsPacket(SkinPartFlags.encodeParts(WearerSkinSettings::isHidden)));
        } else if (serverStoresParts(layout)) {
            PacketHandler.sendToServer(new UpdateSkinLayersPacket((byte) layout.encode(WearerSkinSettings::isHidden)));
        }
    }

    /** Whether the server keeps our part choices, as opposed to only vanilla's cape bit. */
    private static boolean serverStoresParts(SkinPartLayout layout) {
        return layout == SkinPartLayout.V2
            || (layout != SkinPartLayout.VANILLA && SkinPartLayout.acceptsLegacyPacket(serverWawelVersion));
    }

    private static boolean isHidden(EntityPlayer player, SkinPart part) {
        // Preview entities are local and written by WA itself in the current layout.
        if (player instanceof PlayerPreviewEntity) {
            return SkinPartLayout.V2.isHidden(SkinLayersHelper.readFlags(player), part);
        }

        SkinPartLayout layout = currentLayout();
        // A server that does not keep our choices would show our own model with every part, so use them directly.
        if (!serverStoresParts(layout) && player == Minecraft.getMinecraft().thePlayer) {
            return WearerSkinSettings.isHidden(part);
        }
        return layout.isHidden(SkinLayersHelper.readFlags(player), part);
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        serverWawelVersion = null;
        serverProtocol = 0;
    }
}
