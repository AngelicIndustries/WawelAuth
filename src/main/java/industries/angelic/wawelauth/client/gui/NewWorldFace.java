package industries.angelic.wawelauth.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.angellib.util.GuiText;
import industries.angelic.wawelauth.wawelclient.NewWorldAccount;
import industries.angelic.wawelauth.wawelclient.WawelClient;
import industries.angelic.wawelauth.wawelclient.WorldAuthSettings;
import industries.angelic.wawelauth.wawelclient.data.ClientAccount;

/**
 * The account face beside the world name on the world creation screen, across from Cherished Worlds' star: the
 * account the new world will be played with. Clicking it picks one; shift-clicking drops the pick.
 */
@SideOnly(Side.CLIENT)
public final class NewWorldFace {

    private static final int SIZE = 16;
    /** As far right of the name field as Cherished Worlds' star is left of it. */
    private static final int OFFSET_X = 107;
    /** Centered on the name field, which is 20 high at y 60. */
    private static final int Y = 62;

    private NewWorldFace() {}

    private static int left(int screenWidth) {
        return screenWidth / 2 + OFFSET_X;
    }

    public static boolean isOver(int screenWidth, int mouseX, int mouseY) {
        int x = left(screenWidth);
        return mouseX >= x && mouseX < x + SIZE && mouseY >= Y && mouseY < Y + SIZE;
    }

    public static void draw(GuiScreen screen, int mouseX, int mouseY) {
        WawelClient client = WawelClient.instance();
        if (client == null) return;
        ClientAccount picked = NewWorldAccount.get(client.getAccountManager());
        WorldAuthSettings.WorldAccount fallback = picked == null ? WorldAuthSettings.resolveAccount(null, client)
            : null;
        boolean hovered = isOver(screen.width, mouseX, mouseY);

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderHelper.disableStandardItemLighting();
            WorldRowIcons.drawAccountFace(
                picked != null ? picked : fallback != null ? fallback.account : null,
                left(screen.width),
                Y,
                SIZE,
                1.0F,
                hovered,
                hovered && picked != null && GuiScreen.isShiftKeyDown());
        } finally {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopAttrib();
        }
    }

    /** The face's tooltip lines when hovered, or null. */
    public static List<String> tooltip(GuiScreen screen, int mouseX, int mouseY) {
        WawelClient client = WawelClient.instance();
        if (client == null || !isOver(screen.width, mouseX, mouseY)) return null;
        ClientAccount picked = NewWorldAccount.get(client.getAccountManager());
        WorldAuthSettings.WorldAccount fallback = picked == null ? WorldAuthSettings.resolveAccount(null, client)
            : null;
        ClientAccount account = picked != null ? picked : fallback != null ? fallback.account : null;
        boolean shift = GuiScreen.isShiftKeyDown();

        List<String> lines = new ArrayList<>();
        if (account == null) {
            lines.add(EnumChatFormatting.WHITE + GuiText.tr("wawelauth.gui.common.no_account_selected"));
        } else {
            lines.add(EnumChatFormatting.WHITE + (account.getProfileName() != null ? account.getProfileName() : "?"));
            if (fallback != null && fallback.source == WorldAuthSettings.AccountSource.DEFAULT) {
                lines.add(EnumChatFormatting.GRAY + GuiText.tr("wawelauth.gui.world_row.default"));
            } else if (fallback != null && fallback.source == WorldAuthSettings.AccountSource.AUTOMATIC) {
                lines.add(EnumChatFormatting.GRAY + GuiText.tr("wawelauth.gui.server_tooltip.automatic_default"));
            }
        }
        lines.add(EnumChatFormatting.GRAY + GuiText.tr("wawelauth.gui.new_world.click"));
        if (shift && account != null && account.getProfileUuid() != null) {
            lines.add(
                EnumChatFormatting.GRAY + account.getProfileUuid()
                    .toString());
        }
        if (shift && picked != null) {
            lines.add(EnumChatFormatting.RED + GuiText.tr("wawelauth.gui.server_tooltip.unselect_account"));
        }
        return lines;
    }

    /** True when the click was on the face and handled. */
    public static boolean click(GuiScreen screen, int mouseX, int mouseY, String worldName) {
        if (WawelClient.instance() == null || !isOver(screen.width, mouseX, mouseY)) return false;
        if (GuiScreen.isShiftKeyDown()) {
            NewWorldAccount.reset();
            return true;
        }
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
        ServerAccountPickerScreen.openForNewWorld(worldName);
        return true;
    }
}
