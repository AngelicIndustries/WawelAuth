package industries.angelic.wawelauth;

import java.io.File;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import industries.angelic.angellib.util.drop.GuiTransitionScheduler;
import industries.angelic.wawelauth.client.WindowDropHandler;
import industries.angelic.wawelauth.client.gui.GuiSkinCustomizationHandler;
import industries.angelic.wawelauth.client.gui.WawelAuthKeybind;
import industries.angelic.wawelauth.client.gui.WawelAuthTooltipStyle;
import industries.angelic.wawelauth.client.render.SkinResolverClientHandler;
import industries.angelic.wawelauth.client.render.animatedcape.AnimatedCapeClientHandler;
import industries.angelic.wawelauth.client.skin.SkinSnapshots;
import industries.angelic.wawelauth.client.skin.layers.LayerDraws;
import industries.angelic.wawelauth.client.skin.sync.ClientSkinSync;
import industries.angelic.wawelauth.wawelclient.WawelClient;
import industries.angelic.wawelauth.wawelclient.WawelPingClientHooks;
import industries.angelic.wawelauth.wawelcore.config.LocalConfig;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        // Resolve client data directory
        File dataDir;
        LocalConfig local = Config.local();
        if (local != null && local.isUseOsConfigDir()) {
            dataDir = Config.getDataConfigDir();
        } else {
            dataDir = new File(Config.getConfigDir(), "client");
        }

        // WawelClient persists for the entire game session.
        // JVM shutdown hook handles cleanup: we must NOT stop here because
        // init() only runs once and won't re-run after leaving singleplayer.
        WawelClient.start(dataDir);
        WawelPingClientHooks.register();
        GuiTransitionScheduler.register();
        AnimatedCapeClientHandler.register();
        SkinResolverClientHandler.register();
        ClientSkinSync.register();
        SkinSnapshots.register();
        LayerDraws.register();
        WawelAuthTooltipStyle.register();

        // Window drag-and-drop detection (lwjgl3ify check is inside AngelLib)
        WindowDropHandler.register();

        // Register keybind for account manager
        WawelAuthKeybind keybind = new WawelAuthKeybind();
        ClientRegistry.registerKeyBinding(keybind.getKeyBinding());
        FMLCommonHandler.instance()
            .bus()
            .register(keybind);

    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new GuiSkinCustomizationHandler());
    }

    @Override
    public void onServerModList(String wawelVersion) {
        ClientSkinSync.onServerModList(wawelVersion);
    }

    @Override
    public void onConfigReload() {
        super.onConfigReload();
    }
}
