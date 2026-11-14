package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.management.ServerConfigurationManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.GameProfile;

import industries.angelic.wawelauth.wawelclient.lan.LanHost;
import industries.angelic.wawelauth.wawelclient.lan.LanWorld;

/**
 * Tells players joining a singleplayer world opened to LAN which provider each player is from, as
 * {@link MixinServerConfigurationManagerJoinSync} does on a dedicated server.
 */
@Mixin(ServerConfigurationManager.class)
public class MixinServerConfigurationManagerLanSync {

    @Shadow
    public java.util.List<?> playerEntityList;

    @Inject(
        method = "initializeConnectionToPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/management/ServerConfigurationManager;playerLoggedIn(Lnet/minecraft/entity/player/EntityPlayerMP;)V"))
    private void wawelauth$sendLanProviders(NetworkManager netManager, EntityPlayerMP player,
        NetHandlerPlayServer netHandler, CallbackInfo ci) {
        LanHost.onPlayerJoining(netManager, player, netHandler, playerEntityList);
        if (!netManager.isLocalChannel()) {
            LanWorld.onRemotePlayerJoined(player.getGameProfile());
        }
    }

    /** Commands for players who joined while cheats were on for new players, instead of vanilla's switch for all. */
    @Inject(method = "func_152596_g", at = @At("RETURN"), cancellable = true) // canSendCommands
    private void wawelauth$lanPlayerCommands(GameProfile profile, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && LanWorld.allowsCommands(profile)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "playerLoggedOut", at = @At("HEAD"))
    private void wawelauth$forgetLanPlayer(EntityPlayerMP player, CallbackInfo ci) {
        LanHost.onPlayerLeft(player);
        LanWorld.onPlayerLeft(player.getGameProfile());
    }
}
