package industries.angelic.wawelauth.mixins.late.Botania;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import vazkii.botania.client.core.proxy.ClientProxy;

/**
 * Taken from <a href="https://github.com/Roadhog360/SimpleSkinBackport">SimpleSkinBackport</a>
 * [MIT License]
 */
@Mixin(value = ClientProxy.class, priority = 999)
public class MixinClientProxy {

    @WrapOperation(
        method = "initRenderers",
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Lcpw/mods/fml/client/registry/ClientRegistry;bindTileEntitySpecialRenderer(Ljava/lang/Class;Lnet/minecraft/client/renderer/tileentity/TileEntitySpecialRenderer;)V"))
    private void dontReplaceSkullRender(Class<? extends TileEntity> tileEntityClass,
        TileEntitySpecialRenderer specialRenderer, Operation<Void> original) {
        if (tileEntityClass != TileEntitySkull.class) {
            original.call(tileEntityClass, specialRenderer);
        }
    }

}
