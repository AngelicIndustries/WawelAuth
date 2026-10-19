package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface AccessorMinecraft {

    @Accessor("session")
    Session wawelauth$getSession();

    @Accessor("session")
    @Mutable
    void wawelauth$setSession(Session session);
}
