package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.NetHandlerLoginServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftSessionService;

import industries.angelic.wawelauth.wawelclient.lan.LanHost;

/**
 * Checks players joining a singleplayer world opened to LAN against the providers chosen for the world. The
 * dedicated server's counterpart is {@link MixinNetHandlerLoginServerAuthThread}; only one of them loads.
 */
@Mixin(targets = "net.minecraft.server.network.NetHandlerLoginServer$1")
public class MixinNetHandlerLoginServerAuthThreadLan {

    @Redirect(
        method = "run",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/authlib/minecraft/MinecraftSessionService;hasJoinedServer(Lcom/mojang/authlib/GameProfile;Ljava/lang/String;)Lcom/mojang/authlib/GameProfile;"),
        remap = false)
    private GameProfile wawelauth$checkLanPlayer(MinecraftSessionService service, GameProfile profile, String serverId)
        throws AuthenticationUnavailableException {
        return LanHost.hasJoinedServer(profile, serverId, service);
    }

    /** Vanilla lets every player it couldn't verify in as an offline player; the world decides here. */
    @Redirect(
        method = "run",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isSinglePlayer()Z"))
    private boolean wawelauth$letUnverifiedIn(MinecraftServer server) {
        return LanHost.letsUnverifiedIn(server);
    }

    @Redirect(
        method = "run",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/NetHandlerLoginServer;func_147322_a(Ljava/lang/String;)V"))
    // NetHandlerLoginServer.disconnect
    private void wawelauth$lanDisconnectReason(NetHandlerLoginServer handler, String reason) {
        handler.func_147322_a(LanHost.consumeDisconnectReason(reason)); // NetHandlerLoginServer.disconnect
    }

    /** Vanilla logs a turned-away player's name from the profile the check returned, which is null then. */
    @Redirect(
        method = "run",
        at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/GameProfile;getName()Ljava/lang/String;"),
        remap = false)
    private String wawelauth$safeProfileName(GameProfile profile) {
        if (profile == null) {
            return "<unknown>";
        }
        String name = profile.getName();
        return name == null ? "<unknown>" : name;
    }
}
