package industries.angelic.wawelauth.mixins.early.minecraft;

import java.net.SocketAddress;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.wawelauth.wawelcore.config.ClientConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;

/**
 * Mojang's list of blocked servers, which launchers load into 1.7.10 as {@code com.mojang:netty} (patchy): it
 * replaces Netty's {@code Bootstrap} with one that checks every connection against the list. With Wawel Auth the
 * check only runs when the player turns it on, and the list isn't downloaded otherwise. Without patchy there is no
 * {@code checkAddress} and nothing to do.
 */
@Mixin(value = Bootstrap.class, remap = false)
public class MixinNettyBootstrapBlockedServers {

    @Inject(method = "checkAddress", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void wawelauth$skipMojangBlockedServers(SocketAddress address, CallbackInfoReturnable<ChannelFuture> cir) {
        if (!ClientConfig.enforceMojangBlockedServers) {
            cir.setReturnValue(null);
        }
    }
}
