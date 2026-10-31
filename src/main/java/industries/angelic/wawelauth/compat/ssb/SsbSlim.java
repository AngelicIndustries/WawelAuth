package industries.angelic.wawelauth.compat.ssb;

import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.api.skin.PlayerSkins;
import industries.angelic.wawelauth.wawelcore.data.SkinModel;

/**
 * SimpleSkinBackport draws players on its own when Salamander is not there. It learns slim arms from the texture
 * metadata vanilla's SkinManager passes along, a path Wawel Auth's own skin loading skips, so Wawel Auth hands it each
 * player's arms just before SSB reads them for a draw.
 */
@SideOnly(Side.CLIENT)
public final class SsbSlim {

    private static final String ARMS = "roadhog360.simpleskinbackport.ducks.IArmsState";
    private static Class<?> arms;
    private static Method setSlim;

    private SsbSlim() {}

    public static void register() {
        if (!Loader.isModLoaded("simpleskinbackport") || Loader.isModLoaded("salamander")) return;
        try {
            arms = Class.forName(ARMS);
            setSlim = arms.getMethod("ssb$setSlim", boolean.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            WawelAuth.LOG.warn("SimpleSkinBackport's arm state is not available; its players keep their own arms", e);
            return;
        }
        MinecraftForge.EVENT_BUS.register(new SsbSlim());
        WawelAuth.LOG.info("SimpleSkinBackport draws players; Wawel Auth hands it their arms");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforePlayer(RenderLivingEvent.Pre event) {
        if (event.entity instanceof AbstractClientPlayer player) apply(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeHand(RenderHandEvent event) {
        if (Minecraft.getMinecraft().thePlayer != null) apply(Minecraft.getMinecraft().thePlayer);
    }

    private static void apply(AbstractClientPlayer player) {
        if (setSlim == null || !arms.isInstance(player)) return;
        try {
            setSlim.invoke(
                player,
                PlayerSkins.get(player)
                    .model() == SkinModel.SLIM);
        } catch (ReflectiveOperationException | RuntimeException e) {
            setSlim = null;
            WawelAuth.LOG.warn("Could not hand SimpleSkinBackport a player's arms", e);
        }
    }
}
