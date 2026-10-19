package industries.angelic.wawelauth.mixins.early.minecraft;

import java.util.LinkedList;

import net.minecraft.server.management.PlayerProfileCache;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.authlib.GameProfile;

@Mixin(PlayerProfileCache.class)
public interface AccessorPlayerProfileCache {

    /** Every cached profile, most recently used first. Synchronize on it to read. */
    @Accessor("field_152663_e")
    LinkedList<GameProfile> wawelauth$getProfiles();
}
