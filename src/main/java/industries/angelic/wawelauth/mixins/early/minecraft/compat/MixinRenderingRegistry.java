package industries.angelic.wawelauth.mixins.early.minecraft.compat;

import java.util.Map;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cpw.mods.fml.client.registry.RenderingRegistry;
import industries.angelic.wawelauth.client.skin.model.ModelPlayer;
import industries.angelic.wawelauth.mixins.early.minecraft.AccessorRendererLivingEntity;

/**
 * Renderers that draw some other entity with a player's skin (Botania's Gaia Guardian, the Twilight Forest giant) get
 * a {@link ModelPlayer} too, so 64x64 skins map correctly. Taken from
 * <a href="https://github.com/Roadhog360/SimpleSkinBackport">SimpleSkinBackport</a> [MIT License].
 */
@Mixin(value = RenderingRegistry.class, priority = 999)
public class MixinRenderingRegistry {

    @Inject(method = "loadEntityRenderers", at = @At(value = "TAIL"), remap = false)
    private void setupModernModels(Map<Class<? extends Entity>, Render> rendererMap, CallbackInfo ci) {
        RenderManager.instance.entityRenderMap.forEach((entityClass, renderer) -> {
            if (!wawelAuth$rendererCopiesPlayerSkin(renderer)) return;
            ModelBiped model = new ModelPlayer(0.0F);
            if (renderer instanceof RenderPlayer renderPlayer) {
                renderPlayer.modelBipedMain = model;
            } else if (renderer instanceof RenderBiped renderBiped) {
                renderBiped.modelBipedMain = model;
            } else {
                return;
            }
            ((AccessorRendererLivingEntity) renderer).wawelauth$setMainModel(model);
        });
    }

    @Unique
    private static boolean wawelAuth$rendererCopiesPlayerSkin(Render renderer) {
        String name = renderer.getClass()
            .getName();
        return name.equals("vazkii.botania.client.render.entity.RenderDoppleganger")
            || (name.equals("twilightforest.client.renderer.entity.RenderTFGiant"));
    }

}
