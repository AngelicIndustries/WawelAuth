package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import cpw.mods.fml.common.FMLCommonHandler;
import industries.angelic.wawelauth.wawelserver.ServerStartup;

/**
 * A server that Wawel Auth refused to start ends its startup as when it fails to: before loading any world, it stops
 * and exits normally.
 */
@Mixin(DedicatedServer.class)
public abstract class MixinDedicatedServerStartup {

    @WrapOperation(
        method = "startServer",
        at = @At(
            value = "INVOKE",
            target = "Lcpw/mods/fml/common/FMLCommonHandler;handleServerAboutToStart(Lnet/minecraft/server/MinecraftServer;)Z",
            remap = false))
    private boolean wawelauth$refuseStartup(FMLCommonHandler handler, MinecraftServer server,
        Operation<Boolean> original) {
        return original.call(handler, server) && !ServerStartup.refused();
    }
}
