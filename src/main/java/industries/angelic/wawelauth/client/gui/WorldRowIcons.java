package industries.angelic.wawelauth.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
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
 * The account face and castle icon at the right edge of each singleplayer world, as server rows have. They sit in
 * the gap between the row and the scroll bar: Cherished Worlds puts its star in the left one, and Et Futurum's
 * thumbnail and the world's text fill the row.
 */
@SideOnly(Side.CLIENT)
public final class WorldRowIcons {

    private static final ResourceLocation CASTLE = new ResourceLocation("wawelauth", "textures/gui/logo_2_outline.png");
    private static final int SIZE = 12;
    /** From the x GuiSlot draws the row's text at to just past the row's selection box. */
    private static final int OFFSET_X = 220;
    private static final int HEAD_OFFSET_Y = 3;
    private static final int CASTLE_OFFSET_Y = 19;
    private static final float DEFAULT_HEAD_ALPHA = 0.55F;
    private static final long ACCOUNT_CACHE_MS = 2_000L;

    private static final Map<String, Cached> ACCOUNTS = new HashMap<>();

    private static String hoveredFolder;
    private static String hoveredName;
    private static boolean hoveredHead;
    private static WorldAuthSettings.WorldAccount hoveredAccount;

    private WorldRowIcons() {}

    /** At the start of a frame of the world list, and when it opens again. */
    public static void beginFrame() {
        hoveredFolder = null;
        hoveredName = null;
        hoveredAccount = null;
    }

    public static void forgetAccounts() {
        ACCOUNTS.clear();
    }

    /** Draws a row's icons, from the world list's own row drawing so the list clips them. */
    public static void drawRow(SaveFormatComparator world, int x, int y, int mouseX, int mouseY, boolean mouseInList) {
        if (world == null) return;
        WorldAuthSettings.WorldAccount account = account(world.getFileName());
        int left = x + OFFSET_X;
        int headTop = y + HEAD_OFFSET_Y;
        int castleTop = y + CASTLE_OFFSET_Y;
        boolean overHead = mouseInList && within(mouseX, mouseY, left, headTop);
        boolean overCastle = mouseInList && within(mouseX, mouseY, left, castleTop);

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderHelper.disableStandardItemLighting();

            if (account != null && account.account.getProfileUuid() != null) {
                drawFace(
                    account.account,
                    left,
                    headTop,
                    account.source == WorldAuthSettings.AccountSource.WORLD ? 1.0F : DEFAULT_HEAD_ALPHA);
            }

            float shade = overCastle ? 1.0F : 0.75F;
            GL11.glColor4f(shade, shade, shade, 1.0F);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(CASTLE);
            Gui.func_146110_a(/* drawModalRectWithCustomSizedTexture */left, castleTop, 0, 0, SIZE, SIZE, SIZE, SIZE);
        } finally {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopAttrib();
        }

        if (overHead && account != null || overCastle) {
            hoveredFolder = world.getFileName();
            hoveredName = world.getDisplayName();
            hoveredHead = overHead;
            hoveredAccount = account;
        }
    }

    /** The hovered icon's tooltip lines, or null. */
    public static List<String> tooltip() {
        if (hoveredFolder == null) return null;
        List<String> lines = new ArrayList<>();
        if (hoveredHead && hoveredAccount != null) {
            String name = hoveredAccount.account.getProfileName();
            lines.add(EnumChatFormatting.WHITE + (name != null ? name : "?"));
            if (hoveredAccount.source == WorldAuthSettings.AccountSource.DEFAULT) {
                lines.add(EnumChatFormatting.GRAY + GuiText.tr("wawelauth.gui.world_row.default"));
            } else if (hoveredAccount.source == WorldAuthSettings.AccountSource.AUTOMATIC) {
                lines.add(EnumChatFormatting.GRAY + GuiText.tr("wawelauth.gui.server_tooltip.automatic_default"));
            }
            lines.add(EnumChatFormatting.GRAY + GuiText.tr("wawelauth.gui.server_tooltip.choose_account"));
        } else {
            lines.add(GuiText.tr("wawelauth.gui.world_row.castle"));
        }
        return lines;
    }

    /** Opens the world's account picker for a click on an icon; true when it took the click. */
    public static boolean click() {
        if (hoveredFolder == null) return false;
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

    private static boolean within(int mouseX, int mouseY, int left, int top) {
        return mouseX >= left && mouseX < left + SIZE && mouseY >= top && mouseY < top + SIZE;
    }

    private static void drawFace(ClientAccount account, int x, int y, float alpha) {
        WawelClient client = WawelClient.instance();
        ResourceLocation skin = WawelTextureResolver.getDefaultSkin();
        if (client != null) {
            String profileName = account.getProfileName() != null ? account.getProfileName() : "?";
            ClientProvider provider = client.resolveProviderByName(account.getProviderName());
            skin = client.getTextureResolver()
                .getSkin(account.getProfileUuid(), profileName, provider, false);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        WawelFaceRendererClient.drawFace(skin, x, y, SIZE, SIZE, alpha);
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
