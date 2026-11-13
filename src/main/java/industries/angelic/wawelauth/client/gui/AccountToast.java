package industries.angelic.wawelauth.client.gui;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import industries.angelic.angellib.util.GuiText;
import industries.angelic.wawelauth.api.WawelFaceRendererClient;
import industries.angelic.wawelauth.api.WawelTextureResolver;
import industries.angelic.wawelauth.wawelclient.WawelClient;
import industries.angelic.wawelauth.wawelclient.data.ClientProvider;

/**
 * A notice in the top right corner, framed like an achievement, with an account's face and two lines of text. It
 * slides in once a screen or world is up, stays while the mouse is on it, slides out on its own, and opens the
 * account manager on that account when clicked.
 */
public final class AccountToast {

    private static final ResourceLocation FRAME = new ResourceLocation(
        "textures/gui/achievement/achievement_background.png");
    // The achievement frame in that texture, drawn in three slices so it can grow with the text.
    private static final int FRAME_U = 96;
    private static final int FRAME_V = 202;
    private static final int FRAME_WIDTH = 160;
    private static final int FRAME_EDGE = 8;
    private static final int HEIGHT = 32;
    private static final int TEXT_X = 30;
    private static final int TEXT_RIGHT_PADDING = 8;
    private static final long SLIDE_MS = 300L;
    private static final long SHOW_MS = 5000L;
    private static final long AFTER_HOVER_MS = 1500L;
    /** Lightens the toast while the mouse is on it, so it reads as clickable. */
    private static final int HOVER_TINT = 0x30FFFFFF;

    private static final Queue<AccountToast> QUEUE = new ConcurrentLinkedQueue<>();
    private static final Gui GUI = new Gui();
    private static AccountToast current;

    private final Supplier<String> titleSource;
    private final Supplier<String> textSource;
    private String title;
    private String text;
    private String hint;
    private boolean hovered;
    private final UUID profileId;
    private final String profileName;
    private final ClientProvider provider;
    private final long accountId;

    private long hideAtMs = -1L;
    private long lastFrameMs;
    /** How far it has slid in, from 0 to 1. */
    private float shown;
    private int x;
    private int y;
    private int width;

    private AccountToast(Supplier<String> title, Supplier<String> text, UUID profileId, String profileName,
        ClientProvider provider, long accountId) {
        this.titleSource = title;
        this.textSource = text;
        this.profileId = profileId;
        this.profileName = profileName;
        this.provider = provider;
        this.accountId = accountId;
    }

    /**
     * Queues a toast for this account; safe from any thread. The text is read when the toast appears, so it can be
     * translated even when queued before the game's languages are loaded.
     */
    public static void show(Supplier<String> title, Supplier<String> text, UUID profileId, String profileName,
        ClientProvider provider, long accountId) {
        QUEUE.add(new AccountToast(title, text, profileId, profileName, provider, accountId));
    }

    /**
     * Opens the account manager on the toast's account when a click at these display coordinates lands on it.
     * Returns whether it did, so the screen underneath doesn't get the click too.
     */
    public static boolean click(int displayX, int displayY) {
        AccountToast toast = current;
        if (toast == null) return false;
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int mouseX = displayX * resolution.getScaledWidth() / mc.displayWidth;
        int mouseY = resolution.getScaledHeight() - displayY * resolution.getScaledHeight() / mc.displayHeight - 1;
        if (!toast.contains(mouseX, mouseY)) return false;
        toast.hideAtMs = 0L;
        toast.shown = 0F;
        current = null;
        // The same click sound as a button
        mc.getSoundHandler()
            .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
        AccountManagerScreen.openForProvider(toast.provider.getName(), toast.accountId);
        return true;
    }

    private boolean contains(int mouseX, int mouseY) {
        return shown > 0.5F && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + HEIGHT;
    }

    /** Draws the toast after everything else each frame. */
    public static final class Renderer {

        @SubscribeEvent
        public void render(TickEvent.RenderTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getMinecraft();
            long now = Minecraft.getSystemTime();
            if (current == null) {
                // Wait for the main menu or a world, not the loading screen.
                if (mc.currentScreen == null && mc.theWorld == null) return;
                current = QUEUE.poll();
                if (current == null) return;
                current.title = current.titleSource.get();
                current.text = current.textSource.get();
                current.hint = GuiText.tr("wawelauth.gui.account_toast.click_to_open");
                current.hideAtMs = now + SLIDE_MS + SHOW_MS;
                current.lastFrameMs = now;
            }
            current.update(mc, now);
            if (current != null) {
                current.draw(mc);
            }
        }
    }

    private void update(Minecraft mc, long now) {
        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        hovered = false;
        if (!Mouse.isGrabbed()) {
            int mouseX = Mouse.getX() * resolution.getScaledWidth() / mc.displayWidth;
            int mouseY = resolution.getScaledHeight() - Mouse.getY() * resolution.getScaledHeight() / mc.displayHeight
                - 1;
            if (contains(mouseX, mouseY)) {
                hovered = true;
                hideAtMs = Math.max(hideAtMs, now + AFTER_HOVER_MS);
            }
        }

        float step = (now - lastFrameMs) / (float) SLIDE_MS;
        lastFrameMs = now;
        shown = now < hideAtMs ? Math.min(1F, shown + step) : Math.max(0F, shown - step);
        if (shown <= 0F && now >= hideAtMs) {
            current = null;
            return;
        }

        FontRenderer font = mc.fontRenderer;
        // Wide enough for the hint too, so the toast keeps its size when hovered.
        int textWidth = Math
            .max(font.getStringWidth(title), Math.max(font.getStringWidth(text), font.getStringWidth(hint)));
        width = Math.max(FRAME_WIDTH, TEXT_X + textWidth + TEXT_RIGHT_PADDING);
        x = resolution.getScaledWidth() - width;
        float eased = 1F - (1F - shown) * (1F - shown);
        y = Math.round(-(HEIGHT + 4) * (1F - eased));
    }

    private void draw(Minecraft mc) {
        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(
            0.0D,
            resolution.getScaledWidth_double(),
            resolution.getScaledHeight_double(),
            0.0D,
            1000.0D,
            3000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

            mc.getTextureManager()
                .bindTexture(FRAME);
            int middle = FRAME_WIDTH - 2 * FRAME_EDGE;
            GUI.drawTexturedModalRect(x, y, FRAME_U, FRAME_V, FRAME_EDGE, HEIGHT);
            for (int drawn = FRAME_EDGE; drawn < width - FRAME_EDGE; drawn += middle) {
                int piece = Math.min(middle, width - FRAME_EDGE - drawn);
                GUI.drawTexturedModalRect(x + drawn, y, FRAME_U + FRAME_EDGE, FRAME_V, piece, HEIGHT);
            }
            GUI.drawTexturedModalRect(
                x + width - FRAME_EDGE,
                y,
                FRAME_U + FRAME_WIDTH - FRAME_EDGE,
                FRAME_V,
                FRAME_EDGE,
                HEIGHT);

            if (hovered) {
                Gui.drawRect(x + 2, y + 2, x + width - 2, y + HEIGHT - 2, HOVER_TINT);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            }
            mc.fontRenderer.drawString(title, x + TEXT_X, y + 7, 0xFFFFFF00);
            mc.fontRenderer.drawString(hovered ? hint : text, x + TEXT_X, y + 18, 0xFFFFFFFF);

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            WawelFaceRendererClient.drawFace(skin(), x + 8, y + 8, 16, 16, 1.0F);
        } finally {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private ResourceLocation skin() {
        WawelClient client = WawelClient.instance();
        return client == null ? WawelTextureResolver.getDefaultSkin()
            : client.getTextureResolver()
                .getSkin(profileId, profileName, provider, false);
    }
}
