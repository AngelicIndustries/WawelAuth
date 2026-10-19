package industries.angelic.wawelauth.wawelcore.skin;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import industries.angelic.wawelauth.api.SkinLayersHelper;

/** Server side of the skin part protocol. Runs on dedicated and integrated servers alike. */
public final class SkinPartsServerHandler {

    private static boolean registered;

    private SkinPartsServerHandler() {}

    public static synchronized void register() {
        if (registered) return;
        SkinPartsServerHandler handler = new SkinPartsServerHandler();
        FMLCommonHandler.instance()
            .bus()
            .register(handler);
        MinecraftForge.EVENT_BUS.register(handler);
        registered = true;
    }

    /**
     * Announces protocol v2 on a raw channel. Vanilla and older WA clients ignore unknown raw channels, while an
     * unknown SimpleImpl discriminator would get them kicked.
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP player) || player.playerNetServerHandler == null) return;
        player.playerNetServerHandler.sendPacket(
            new S3FPacketCustomPayload(SkinPartFlags.CHANNEL, new byte[] { (byte) SkinPartFlags.PROTOCOL_VERSION }));
    }

    /** Vanilla does not copy slot 16 to the respawned player entity, which would reset every part to shown. */
    @SubscribeEvent
    public void onClone(Clone event) {
        if (event.original == null || event.entityPlayer == null) return;
        event.entityPlayer.getDataWatcher()
            .updateObject(SkinPartFlags.DATA_WATCHER_ID, (byte) SkinLayersHelper.readFlags(event.original));
        if (event.original instanceof IPlayerCapeState original
            && event.entityPlayer instanceof IPlayerCapeState copy) {
            copy.wawelauth$setCapeHiddenByEquipment(original.wawelauth$isCapeHiddenByEquipment());
        }
    }
}
