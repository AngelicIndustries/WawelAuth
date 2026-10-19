package industries.angelic.wawelauth.packet;

import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import industries.angelic.wawelauth.common.ServerTaskScheduler;
import industries.angelic.wawelauth.mixins.early.minecraft.AccessorPlayerProfileCache;
import industries.angelic.wawelauth.wawelserver.LocalSessionVerifier;
import io.netty.buffer.ByteBuf;

/**
 * Client -> Server. Which of this server's players goes by a name, for skins shown by name only. The server answers
 * from its online players and user cache, never the network. Only sent after the server advertised it.
 */
public class NameLookupPacket implements IMessage {

    static final int MAX_NAME_LENGTH = 64;
    /** Lookups a player may ask for per window. */
    private static final int LIMIT = 32;
    private static final long WINDOW_MS = 10_000L;
    private static final Map<UUID, long[]> WINDOWS = new ConcurrentHashMap<>();

    private String name;

    public NameLookupPacket() {}

    public NameLookupPacket(String name) {
        this.name = name;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.name = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.name == null ? "" : this.name);
    }

    public static class Handler implements IMessageHandler<NameLookupPacket, IMessage> {

        @Override
        public IMessage onMessage(NameLookupPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            String name = message.name;
            if (player == null || name == null || name.isEmpty() || name.length() > MAX_NAME_LENGTH) return null;
            if (!allowed(player.getUniqueID())) return null;

            ServerTaskScheduler.schedule(() -> {
                if (player.playerNetServerHandler == null) return;
                GameProfile profile = find(name);
                String sessionUrl = profile == null ? null
                    : LocalSessionVerifier.getPlayerProviderSessionUrl(profile.getId());
                PacketHandler.sendToPlayer(new NameLookupReplyPacket(name, profile, sessionUrl), player);
            });
            return null;
        }

        private static boolean allowed(UUID player) {
            if (player == null) return false;
            long now = System.currentTimeMillis();
            long[] window = WINDOWS.computeIfAbsent(player, id -> new long[] { now, 0 });
            synchronized (window) {
                if (now - window[0] > WINDOW_MS) {
                    window[0] = now;
                    window[1] = 0;
                }
                return ++window[1] <= LIMIT;
            }
        }

        private static GameProfile find(String name) {
            MinecraftServer server = MinecraftServer.getServer();
            if (server == null) return null;

            EntityPlayerMP online = server.getConfigurationManager()
                .func_152612_a(name); // getPlayerForUsername
            if (online != null && online.getGameProfile() != null
                && online.getGameProfile()
                    .getId() != null) {
                return online.getGameProfile();
            }

            LinkedList<GameProfile> cached = ((AccessorPlayerProfileCache) server.func_152358_ax())
                .wawelauth$getProfiles();
            synchronized (cached) {
                for (GameProfile profile : cached) {
                    if (profile != null && profile.getId() != null && name.equalsIgnoreCase(profile.getName())) {
                        return profile;
                    }
                }
            }
            return null;
        }
    }
}
