package industries.angelic.wawelauth.mixins.early.minecraft;

import java.io.File;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.storage.WorldInfo;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.wawelauth.WawelAuth;

/**
 * Singleplayer keeps the world owner's player in level.dat, whichever account opens the world, so every account
 * shared one inventory. level.dat records the UUID of the account that saved it; when another account opens the
 * world, it gets its own player file instead, as on a server.
 */
@Mixin(ServerConfigurationManager.class)
public abstract class MixinServerConfigurationManagerHostData {

    @Shadow
    @Final
    private MinecraftServer mcServer;

    @Redirect(
        method = { "readPlayerDataFromFile", "getPlayerNBT" },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/storage/WorldInfo;getPlayerNBTTagCompound()Lnet/minecraft/nbt/NBTTagCompound;"))
    private NBTTagCompound wawelauth$hostDataOfThisAccount(WorldInfo worldInfo, EntityPlayerMP player) {
        NBTTagCompound hostData = worldInfo.getPlayerNBTTagCompound();
        if (hostData == null || !mcServer.isSinglePlayer()
            || !hostData.hasKey("UUIDMost", 4)
            || !hostData.hasKey("UUIDLeast", 4)) {
            return hostData;
        }
        UUID savedBy = new UUID(hostData.getLong("UUIDMost"), hostData.getLong("UUIDLeast"));
        if (savedBy.equals(player.getUniqueID())) {
            return hostData;
        }
        // Only when level.dat belongs to another account that played here: worlds saved before players had UUIDs
        // carry a random one, and their owner keeps the world's player.
        File playerData = new File(
            mcServer.worldServers[0].getSaveHandler()
                .getWorldDirectory(),
            "playerdata/" + savedBy + ".dat");
        if (!playerData.isFile()) {
            return hostData;
        }
        WawelAuth.debug(
            "level.dat holds another account's player, loading " + player.getCommandSenderName() + "'s own instead");
        return null;
    }
}
