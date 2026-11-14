package industries.angelic.wawelauth.client.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.SaveFormatComparator;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.angellib.util.GuiText;
import industries.angelic.wawelauth.api.WawelFaceRendererClient;
import industries.angelic.wawelauth.api.WawelTextureResolver;
import industries.angelic.wawelauth.wawelclient.WawelClient;
import industries.angelic.wawelauth.wawelclient.WorldAuthSettings;
import industries.angelic.wawelauth.wawelclient.data.ClientAccount;
import industries.angelic.wawelauth.wawelclient.data.ClientProvider;

/**
 * The account face at the right edge of each singleplayer world. It sits in the gap between the row and the scroll
 * bar: Cherished Worlds puts its star in the left gap, and Et Futurum's thumbnail and the world's text fill the row.
 * Clicking the face opens the world's account picker; shift-clicking the face of the world's own account makes the
 * world use the default again.
 */
@SideOnly(Side.CLIENT)
public final class WorldRowIcons {

    private static final int HEAD_SIZE = 12;
    /** From the x GuiSlot draws the row's text at: the gap between the row's selection box and the scroll bar. */
    private static final int OFFSET_X = 219;
    /** Centered in the row's 32 pixels, as Cherished Worlds' star is. */
    private static final int OFFSET_Y = 10;
    private static final int OUTLINE_COLOR = 0xFFFFFFFF;
    private static final long ACCOUNT_CACHE_MS = 2_000L;

    private static final Map<String, Cached> ACCOUNTS = new HashMap<>();

    private static String hoveredFolder;
    private static String hoveredName;
    private static WorldAuthSettings.WorldAccount hoveredAccount;
    private static File hoveredWorldDirectory;

    private WorldRowIcons() {}

    /** At the start of a frame of the world list, and when it opens again. */
    public static void beginFrame() {
        hoveredFolder = null;
        hoveredName = null;
        hoveredAccount = null;
        hoveredWorldDirectory = null;
    }

    public static void forgetAccounts() {
        ACCOUNTS.clear();
    }

    /** Draws a row's face, from the world list's own row drawing so the list clips it. */
    public static void drawRow(SaveFormatComparator world, int x, int y, int mouseX, int mouseY, boolean mouseInList) {
        if (world == null) return;
        WorldAuthSettings.WorldAccount account = account(world.getFileName());
        int headX = x + OFFSET_X;
        int headY = y + OFFSET_Y;
        boolean own = account != null && account.source == WorldAuthSettings.AccountSource.WORLD;
        boolean hovered = mouseInList && mouseX >= headX
            && mouseX < headX + HEAD_SIZE
            && mouseY >= headY
            && mouseY < headY + HEAD_SIZE;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderHelper.disableStandardItemLighting();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

            // Without any account, the default face still shows where to click.
            drawAccountFace(
                account != null ? account.account : null,
                headX,
                headY,
                HEAD_SIZE,
                1.0F,
                hovered,
                hovered && own && GuiScreen.isShiftKeyDown());
        } finally {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopAttrib();
        }

        if (hovered) {
            hoveredFolder = world.getFileName();
            hoveredName = world.getDisplayName();
            hoveredAccount = account;
            hoveredWorldDirectory = WorldAuthSettings.worldDirectory(world.getFileName());
        }
    }

    /** The hovered face's tooltip lines, or null. */
    public static List<String> tooltip() {
        if (hoveredFolder == null) return null;
        List<String> lines = new ArrayList<>();
        boolean shift = GuiScreen.isShiftKeyDown();
        ClientAccount account = hoveredAccount != null ? hoveredAccount.account : null;
        if (account == null) {
            lines.add(EnumChatFormatting.WHITE + GuiText.tr("wawelauth.gui.common.no_account_selected"));
        } else {
            lines.add(EnumChatFormatting.WHITE + (account.getProfileName() != null ? account.getProfileName() : "?"));
            if (hoveredAccount.source == WorldAuthSettings.AccountSource.DEFAULT) {
                lines.add(EnumChatFormatting.GRAY + GuiText.tr("wawelauth.gui.world_row.default"));
            } else if (hoveredAccount.source == WorldAuthSettings.AccountSource.AUTOMATIC) {
                lines.add(EnumChatFormatting.GRAY + GuiText.tr("wawelauth.gui.server_tooltip.automatic_default"));
            }
        }
        lines.add(EnumChatFormatting.GRAY + GuiText.tr("wawelauth.gui.world_row.click"));
        if (shift && account != null && account.getProfileUuid() != null) {
            lines.add(
                EnumChatFormatting.GRAY + account.getProfileUuid()
                    .toString());
        }
        if (shift && hoveredAccount != null && hoveredAccount.source == WorldAuthSettings.AccountSource.WORLD) {
            lines.add(EnumChatFormatting.RED + GuiText.tr("wawelauth.gui.server_tooltip.unselect_account"));
        }
        return lines;
    }

    /**
     * A click on a face opens the world's account picker, and a shift-click on the face of the world's own account
     * drops it. True when it took the click.
     */
    public static boolean click() {
        if (hoveredFolder == null) return false;
        if (GuiScreen.isShiftKeyDown()) {
            if (hoveredAccount == null || hoveredAccount.source != WorldAuthSettings.AccountSource.WORLD) {
                return false;
            }
            WorldAuthSettings.setAccount(hoveredWorldDirectory, null);
            forgetAccounts();
            beginFrame();
            return true;
        }
        String folder = hoveredFolder;
        String name = hoveredName;
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
        beginFrame();
        ServerAccountPickerScreen.openForWorld(folder, name);
        return true;
    }

    private static WorldAuthSettings.WorldAccount account(String folder) {
        long now = System.currentTimeMillis();
        Cached cached = ACCOUNTS.get(folder);
        if (cached == null || now - cached.readAt > ACCOUNT_CACHE_MS) {
            cached = new Cached(
                WorldAuthSettings.resolveAccount(WorldAuthSettings.worldDirectory(folder), WawelClient.instance()),
                now);
            ACCOUNTS.put(folder, cached);
        }
        return cached.account;
    }

    /**
     * An account's face as world rows and the world creation screen show it: the default face without an account, a
     * white outline when hovered, and a red tint when a shift-click would drop it.
     */
    static void drawAccountFace(ClientAccount account, int x, int y, int size, float alpha, boolean hovered,
        boolean dropping) {
        WawelClient client = WawelClient.instance();
        ResourceLocation skin = WawelTextureResolver.getDefaultSkin();
        if (client != null && account != null && account.getProfileUuid() != null) {
            String profileName = account.getProfileName() != null ? account.getProfileName() : "?";
            ClientProvider provider = client.resolveProviderByName(account.getProviderName());
            skin = client.getTextureResolver()
                .getSkin(account.getProfileUuid(), profileName, provider, false);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        WawelFaceRendererClient.drawFace(skin, x, y, size, size, alpha);
        if (dropping) {
            Gui.drawRect(x, y, x + size, y + size, 0x55FF0000);
        }
        if (hovered) {
            int left = x - 1;
            int top = y - 1;
            int right = x + size + 1;
            int bottom = y + size + 1;
            Gui.drawRect(left, top, right, top + 1, OUTLINE_COLOR);
            Gui.drawRect(left, bottom - 1, right, bottom, OUTLINE_COLOR);
            Gui.drawRect(left, top + 1, left + 1, bottom - 1, OUTLINE_COLOR);
            Gui.drawRect(right - 1, top + 1, right, bottom - 1, OUTLINE_COLOR);
        }
    }

    private static final class Cached {

        final WorldAuthSettings.WorldAccount account;
        final long readAt;

        Cached(WorldAuthSettings.WorldAccount account, long readAt) {
            this.account = account;
            this.readAt = readAt;
        }
    }
}
