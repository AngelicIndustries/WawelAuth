package industries.angelic.wawelauth.client.gui;

import java.io.File;

import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A world's LAN Access list on its own, over the Open to LAN and Manage LAN screens. */
@SideOnly(Side.CLIENT)
public class LanAccessScreen extends ParentAwareModularScreen {

    /** ModularScreen's constructor builds the UI before the subclass's fields are set. */
    private static File pendingWorldDirectory;

    private final File worldDirectory;

    public static void open(File worldDirectory) {
        if (worldDirectory == null) return;
        pendingWorldDirectory = worldDirectory;
        ClientGUI.open(new LanAccessScreen());
    }

    private LanAccessScreen() {
        super("wawelauth");
        openParentOnClose(true);
        this.worldDirectory = pendingWorldDirectory;
        pendingWorldDirectory = null;
    }

    @Override
    public ModularPanel buildUI(ModularGuiContext context) {
        File world = pendingWorldDirectory != null ? pendingWorldDirectory : worldDirectory;
        ModularPanel panel = ModularPanel.defaultPanel("wawelauth_lan_access");
        return LanProvidersDialog.fill(panel, world, panel::closeIfOpen);
    }
}
