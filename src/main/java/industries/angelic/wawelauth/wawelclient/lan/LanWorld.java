package industries.angelic.wawelauth.wawelclient.lan;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.multiplayer.ThreadLanServerPing;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.WorldSettings;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import industries.angelic.angellib.util.StringUtil;
import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.mixins.early.minecraft.AccessorIntegratedServer;
import industries.angelic.wawelauth.mixins.early.minecraft.AccessorNetworkSystemEndpoints;
import industries.angelic.wawelauth.mixins.early.minecraft.AccessorServerConfigurationManagerGameType;
import industries.angelic.wawelauth.wawelclient.WorldAuthSettings;
import io.netty.channel.ChannelFuture;

/**
 * A singleplayer world while it's open to LAN: its port and e4mc address, the game mode and cheats players get when
 * they join from now on, and closing it again.
 * <p>
 * Vanilla's "allow cheats" is one switch the server checks for everyone, every time, so changing it would change it
 * for players already playing. Instead, players get commands when they join while cheats are on for new players, and
 * keep that until they leave.
 */
public final class LanWorld {

    private static final String E4MC_DOMAIN_MESSAGE = "text.e4mc_minecraft.domainAssigned";
    private static final Queue<Runnable> SERVER_TASKS = new ConcurrentLinkedQueue<>();
    /** One second of server ticks: time for a kicked player's message to get through a relay. */
    private static final int STOP_STEP_TICKS = 20;
    private static final Set<UUID> PLAYERS_WITH_COMMANDS = ConcurrentHashMap.newKeySet();

    private static volatile MinecraftServer trackedServer;
    private static volatile String port;
    private static volatile String e4mcDomain;
    private static volatile boolean cheatsForNewPlayers;

    private LanWorld() {}

    /** Runs the queued work on the integrated server's own thread, which 1.7.10 has no queue for. */
    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(TICKS);
    }

    public static final class TickHandler {

        private final List<Delayed> delayed = new ArrayList<>();

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Runnable task;
            while ((task = SERVER_TASKS.poll()) != null) {
                run(task);
            }
            for (Iterator<Delayed> it = delayed.iterator(); it.hasNext();) {
                Delayed later = it.next();
                if (--later.ticks <= 0) {
                    it.remove();
                    run(later.task);
                }
            }
        }

        void later(int ticks, Runnable task) {
            delayed.add(new Delayed(ticks, task));
        }

        private static void run(Runnable task) {
            try {
                task.run();
            } catch (RuntimeException e) {
                WawelAuth.LOG.warn("LAN world task failed", e);
            }
        }
    }

    private static final class Delayed {

        int ticks;
        final Runnable task;

        Delayed(int ticks, Runnable task) {
            this.ticks = ticks;
            this.task = task;
        }
    }

    private static final TickHandler TICKS = new TickHandler();

    /** The integrated server while it's open to LAN, or null. */
    public static IntegratedServer server() {
        MinecraftServer server = MinecraftServer.getServer();
        if (!(server instanceof IntegratedServer integrated)) {
            return null;
        }
        if (server != trackedServer) {
            synchronized (LanWorld.class) {
                if (server != trackedServer) {
                    PLAYERS_WITH_COMMANDS.clear();
                    port = null;
                    e4mcDomain = null;
                    cheatsForNewPlayers = false;
                    trackedServer = server;
                }
            }
        }
        return integrated.getPublic() ? integrated : null;
    }

    public static File worldDirectory() {
        MinecraftServer server = MinecraftServer.getServer();
        return server instanceof IntegratedServer ? WorldAuthSettings.worldDirectory(server.getFolderName()) : null;
    }

    /** After the world was opened to LAN: cheats move from vanilla's switch for everyone to players who join. */
    public static void onOpened(IntegratedServer opened, String openedPort, boolean allowCheats) {
        server();
        port = openedPort;
        cheatsForNewPlayers = allowCheats;
        opened.getConfigurationManager()
            .setCommandsAllowedForAll(false);
    }

    public static String port() {
        return server() != null ? port : null;
    }

    public static String e4mcDomain() {
        return server() != null ? e4mcDomain : null;
    }

    public static boolean cheatsForNewPlayers() {
        return cheatsForNewPlayers;
    }

    public static void setCheatsForNewPlayers(boolean cheats) {
        cheatsForNewPlayers = cheats;
    }

    public static WorldSettings.GameType gameModeForNewPlayers() {
        IntegratedServer server = server();
        if (server == null) return WorldSettings.GameType.SURVIVAL;
        WorldSettings.GameType type = ((AccessorServerConfigurationManagerGameType) server.getConfigurationManager())
            .wawelauth$getGameType();
        return type != null ? type
            : server.worldServers[0].getWorldInfo()
                .getGameType();
    }

    public static void setGameModeForNewPlayers(WorldSettings.GameType type) {
        IntegratedServer server = server();
        if (server != null) {
            SERVER_TASKS.add(
                () -> server.getConfigurationManager()
                    .func_152604_a(type)); // ServerConfigurationManager.setGameType
        }
    }

    public static void onRemotePlayerJoined(GameProfile profile) {
        if (server() != null && cheatsForNewPlayers && profile != null && profile.getId() != null) {
            PLAYERS_WITH_COMMANDS.add(profile.getId());
        }
    }

    public static void onPlayerLeft(GameProfile profile) {
        if (profile != null && profile.getId() != null) {
            PLAYERS_WITH_COMMANDS.remove(profile.getId());
        }
    }

    public static boolean allowsCommands(GameProfile profile) {
        return profile != null && profile.getId() != null
            && server() != null
            && PLAYERS_WITH_COMMANDS.contains(profile.getId());
    }

    /** e4mc posts the address it got to the chat, and keeps it nowhere else. */
    public static void noteChat(IChatComponent message) {
        if (!(message instanceof ChatComponentTranslation translation)
            || !E4MC_DOMAIN_MESSAGE.equals(translation.getKey())) {
            return;
        }
        Object[] args = translation.getFormatArgs();
        if (args.length == 0) return;
        String domain = StringUtil.trimToNull(
            args[0] instanceof IChatComponent component ? component.getUnformattedText() : String.valueOf(args[0]));
        if (domain != null) {
            e4mcDomain = domain;
        }
    }

    /**
     * Closes the world to LAN again: players who joined are sent back, then e4mc ends its session, then the LAN and
     * relay ports close and the world stops being announced. The host keeps playing. The steps are a second apart,
     * so the message reaches players connected through a relay before the relay goes.
     */
    public static void stop() {
        IntegratedServer server = server();
        if (server == null) return;
        port = null;
        e4mcDomain = null;
        SERVER_TASKS.add(() -> {
            for (Object raw : new ArrayList<Object>(server.getConfigurationManager().playerEntityList)) {
                if (raw instanceof EntityPlayerMP player && player.playerNetServerHandler != null
                    && !player.playerNetServerHandler.func_147362_b() // NetHandlerPlayServer.getNetworkManager
                        .isLocalChannel()) {
                    player.playerNetServerHandler.kickPlayerFromServer("The host closed the LAN world.");
                }
            }
            PLAYERS_WITH_COMMANDS.clear();
            TICKS.later(STOP_STEP_TICKS, LanWorld::stopE4mc);
            TICKS.later(STOP_STEP_TICKS * 2, () -> closeEndpoints(server));
        });
    }

    private static void closeEndpoints(IntegratedServer server) {
        List<?> endpoints = ((AccessorNetworkSystemEndpoints) server.func_147137_ag()) // getNetworkSystem
            .wawelauth$getEndpoints();
        synchronized (endpoints) {
            for (Iterator<?> it = endpoints.iterator(); it.hasNext();) {
                Object raw = it.next();
                // The host's own connection listens on a local address and stays open.
                if (raw instanceof ChannelFuture future && future.channel()
                    .localAddress() instanceof InetSocketAddress) {
                    future.channel()
                        .close()
                        .syncUninterruptibly();
                    it.remove();
                }
            }
        }

        AccessorIntegratedServer accessor = (AccessorIntegratedServer) server;
        ThreadLanServerPing ping = accessor.wawelauth$getLanServerPing();
        if (ping != null) {
            ping.interrupt();
            accessor.wawelauth$setLanServerPing(null);
        }
        accessor.wawelauth$setPublic(false);
        WawelAuth.LOG.info("Closed the world to LAN");
    }

    private static void stopE4mc() {
        try {
            Class<?> client = Class.forName("link.e4mc.E4mcClient");
            Object session = client.getField("session")
                .get(null);
            if (session == null) return;
            Object state = session.getClass()
                .getField("state")
                .get(session);
            if (state == null || !"STOPPED".equals(state.toString())) {
                session.getClass()
                    .getMethod("stop")
                    .invoke(session);
            }
        } catch (ClassNotFoundException e) {
            // e4mc isn't installed.
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            WawelAuth.LOG.warn("Couldn't stop e4mc: {}", e.toString());
        }
    }
}
