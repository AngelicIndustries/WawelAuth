package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.WorldSettings;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerConfigurationManager.class)
public interface AccessorServerConfigurationManagerGameType {

    /** The game mode players get when they join, which opening a world to LAN sets. */
    @Accessor("gameType")
    WorldSettings.GameType wawelauth$getGameType();
}
