package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.wawelauth.wawelserver.FallbackWhitelistLookup;
import industries.angelic.wawelauth.wawelserver.ProviderQualifiedPlayerLookup;

@Mixin(CommandBase.class)
public abstract class MixinCommandBase {

    @Inject(method = "getPlayer", at = @At("HEAD"), cancellable = true)
    private static void wawelauth$getProviderQualifiedPlayer(ICommandSender sender, String username,
        CallbackInfoReturnable<EntityPlayerMP> cir) {
        if (!FallbackWhitelistLookup.isQualifiedProviderUsername(username)) {
            return;
        }

        EntityPlayerMP player = ProviderQualifiedPlayerLookup.resolveOnlinePlayer(username);
        if (player == null) {
            throw new PlayerNotFoundException("wawelauth.commands.online_player_not_found", username);
        }

        cir.setReturnValue(player);
    }
}
