package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.client.multiplayer.ThreadLanServerPing;
import net.minecraft.server.integrated.IntegratedServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IntegratedServer.class)
public interface AccessorIntegratedServer {

    @Accessor("isPublic")
    void wawelauth$setPublic(boolean isPublic);

    @Accessor("lanServerPing")
    ThreadLanServerPing wawelauth$getLanServerPing();

    @Accessor("lanServerPing")
    void wawelauth$setLanServerPing(ThreadLanServerPing ping);
}
