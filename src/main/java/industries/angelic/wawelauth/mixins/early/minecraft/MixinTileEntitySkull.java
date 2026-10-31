package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;

import industries.angelic.wawelauth.wawelserver.ServerNameLookup;

/**
 * A player head given only its owner's name finds the owner by Wawel Auth's rules for names, not at Mojang, where the
 * name may belong to someone else: one of this server's players, or a {@code name@provider} at that provider, which
 * updates the head when the answer comes. A name nobody here knows stays a name, which clients look up with their own
 * providers. The skin comes from the owner's provider on each client, so Mojang is not asked for it either.
 */
@Mixin(TileEntitySkull.class)
public abstract class MixinTileEntitySkull extends TileEntity {

    @Shadow
    private GameProfile field_152110_j;

    @WrapOperation(
        method = "func_152109_d",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/management/PlayerProfileCache;func_152655_a(Ljava/lang/String;)Lcom/mojang/authlib/GameProfile;"))
    private GameProfile wawelauth$findOwner(PlayerProfileCache cache, String name, Operation<GameProfile> original) {
        if (!ServerNameLookup.isQualified(name)) return ServerNameLookup.known(name);
        ServerNameLookup.qualified(name, owner -> wawelauth$setOwner(name, owner));
        return null;
    }

    @WrapOperation(
        method = "func_152109_d",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/authlib/minecraft/MinecraftSessionService;fillProfileProperties(Lcom/mojang/authlib/GameProfile;Z)Lcom/mojang/authlib/GameProfile;",
            remap = false))
    private GameProfile wawelauth$keepProfile(MinecraftSessionService sessions, GameProfile profile, boolean secure,
        Operation<GameProfile> original) {
        return profile;
    }

    /** The owner found for a {@code name@provider}, if the head still waits for that name. */
    @Unique
    private void wawelauth$setOwner(String name, GameProfile owner) {
        GameProfile current = this.field_152110_j;
        if (owner == null || this.isInvalid()
            || this.worldObj == null
            || current == null
            || current.getId() != null
            || !name.equals(current.getName())) {
            return;
        }
        this.field_152110_j = owner;
        this.markDirty();
        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
    }
}
