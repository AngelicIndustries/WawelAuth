package industries.angelic.wawelauth.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldSettings;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.angellib.util.GuiText;
import industries.angelic.wawelauth.wawelclient.lan.LanWorld;

/**
 * A world open to LAN, from the pause menu: its port and e4mc address, the game mode and cheats of players who join
 * from now on, who may join, and closing it again. Laid out like vanilla's Open to LAN screen.
 */
@SideOnly(Side.CLIENT)
public class GuiManageLan extends GuiScreen {

    private static final int COPY = 1;
    private static final int GAME_MODE = 2;
    private static final int CHEATS = 3;
    private static final int LAN_ACCESS = 4;
    private static final int STOP = 5;
    private static final int DONE = 6;
    private static final long COPIED_SHOWN_MS = 1500L;

    private final GuiScreen parent;
    private WorldSettings.GameType gameMode;
    private boolean cheats;
    private GuiButton copyButton;
    private GuiButton gameModeButton;
    private GuiButton cheatsButton;
    private long copiedAt;

    public GuiManageLan(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.gameMode = LanWorld.gameModeForNewPlayers();
        this.cheats = LanWorld.cheatsForNewPlayers();
        int center = this.width / 2;
        this.buttonList.clear();
        this.copyButton = new GuiButton(COPY, center - 75, 70, 150, 20, "");
        this.copyButton.visible = LanWorld.e4mcDomain() != null;
        this.buttonList.add(this.copyButton);
        this.buttonList.add(this.gameModeButton = new GuiButton(GAME_MODE, center - 155, 112, 150, 20, ""));
        this.buttonList.add(this.cheatsButton = new GuiButton(CHEATS, center + 5, 112, 150, 20, ""));
        this.buttonList.add(
            new GuiButton(LAN_ACCESS, center - 75, 136, 150, 20, GuiText.tr("wawelauth.gui.lan_providers.button")));
        this.buttonList
            .add(new GuiButton(STOP, center - 155, this.height - 28, 150, 20, GuiText.tr("wawelauth.gui.lan.stop")));
        this.buttonList.add(new GuiButton(DONE, center + 5, this.height - 28, 150, 20, I18n.format("gui.done")));
        updateLabels();
    }

    private void updateLabels() {
        this.copyButton.displayString = GuiText.tr(
            System.currentTimeMillis() - this.copiedAt < COPIED_SHOWN_MS ? "wawelauth.gui.lan.copied"
                : "wawelauth.gui.lan.copy_e4mc");
        this.gameModeButton.displayString = I18n.format("selectWorld.gameMode") + " "
            + I18n.format("selectWorld.gameMode." + this.gameMode.getName());
        this.cheatsButton.displayString = I18n.format("selectWorld.allowCommands") + " "
            + I18n.format(this.cheats ? "options.on" : "options.off");
    }

    @Override
    public void updateScreen() {
        if (LanWorld.server() == null) {
            this.mc.displayGuiScreen(this.parent);
            return;
        }
        this.copyButton.visible = LanWorld.e4mcDomain() != null;
        updateLabels();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case COPY -> {
                String domain = LanWorld.e4mcDomain();
                if (domain != null) {
                    setClipboardString(domain);
                    this.copiedAt = System.currentTimeMillis();
                }
            }
            case GAME_MODE -> {
                // The same order as vanilla's Open to LAN screen.
                this.gameMode = switch (this.gameMode) {
                    case SURVIVAL -> WorldSettings.GameType.CREATIVE;
                    case CREATIVE -> WorldSettings.GameType.ADVENTURE;
                    default -> WorldSettings.GameType.SURVIVAL;
                };
                LanWorld.setGameModeForNewPlayers(this.gameMode);
            }
            case CHEATS -> {
                this.cheats = !this.cheats;
                LanWorld.setCheatsForNewPlayers(this.cheats);
            }
            case LAN_ACCESS -> LanAccessScreen.open(LanWorld.worldDirectory());
            case STOP -> {
                LanWorld.stop();
                this.mc.ingameGUI.getChatGUI()
                    .printChatMessage(new ChatComponentTranslation("wawelauth.gui.lan.stopped"));
                this.mc.displayGuiScreen(null);
                this.mc.setIngameFocus();
                return;
            }
            case DONE -> {
                this.mc.displayGuiScreen(this.parent);
                return;
            }
            default -> {}
        }
        updateLabels();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int center = this.width / 2;
        this.drawCenteredString(
            this.fontRendererObj,
            GuiText.tr("wawelauth.gui.lan.manage_title"),
            center,
            30,
            0xFFFFFF);
        String port = LanWorld.port();
        if (port != null) {
            this.drawCenteredString(
                this.fontRendererObj,
                GuiText.tr("wawelauth.gui.lan.port", port),
                center,
                46,
                0xA0A0A0);
        }
        String domain = LanWorld.e4mcDomain();
        if (domain != null) {
            this.drawCenteredString(
                this.fontRendererObj,
                GuiText.tr("wawelauth.gui.lan.e4mc", domain),
                center,
                58,
                0xA0A0A0);
        }
        this.drawCenteredString(
            this.fontRendererObj,
            GuiText.tr("wawelauth.gui.lan.new_players"),
            center,
            98,
            0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
