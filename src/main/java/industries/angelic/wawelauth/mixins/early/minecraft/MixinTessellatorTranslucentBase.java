package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.wawelauth.client.skin.translucent.TranslucentLayers;

/** Splits the semi-transparent texels off a see-through player's base, for renderers that draw it immediately. */
@Mixin(Tessellator.class)
public abstract class MixinTessellatorTranslucentBase {

    @WrapMethod(method = "draw")
    private int wawelauth$splitSeeThroughBase(Operation<Integer> original) {
        if (!TranslucentLayers.splitsBase()) return original.call();
        return TranslucentLayers.splitBase((AccessorTessellator) this, original::call);
    }
}
