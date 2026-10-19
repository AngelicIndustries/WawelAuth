package industries.angelic.wawelauth.mixins.late.dynmap;

import org.dynmap.PlayerFaces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PlayerFaces.class, remap = false)
public interface AccessorPlayerFaces {

    @Accessor("fetchskins")
    boolean wawelauth$getFetchskins();

    @Accessor("refreshskins")
    boolean wawelauth$getRefreshskins();

    @Accessor("skinurl")
    String wawelauth$getSkinurl();
}
