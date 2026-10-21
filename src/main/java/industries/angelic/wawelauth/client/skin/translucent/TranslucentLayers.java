package industries.angelic.wawelauth.client.skin.translucent;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.entity.Entity;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.api.skin.DrawKind;
import industries.angelic.wawelauth.api.skin.LayerSegment;
import industries.angelic.wawelauth.api.skin.PlayerSkin;
import industries.angelic.wawelauth.api.skin.PlayerSkins;
import industries.angelic.wawelauth.client.render.skinlayers.SkinLayers3DConfig;
import industries.angelic.wawelauth.compat.angelica.AngelicaShaders;
import industries.angelic.wawelauth.mixins.early.minecraft.AccessorTessellator;

/**
 * See-through skins: semi-transparent skin texels blend with what is behind them instead of drawing solid. Opaque
 * texels draw as before, writing depth. The rest is held per player and drawn blended, without writing depth, far
 * sides first: when the player's draw ends if water, ice or stained glass lies between the camera and the player, so
 * that terrain lands on top; otherwise after the translucent terrain, farthest player first, so it lands on top of what
 * is behind the player.
 * <p>
 * The layers split here as they are painted. The base splits where its geometry is drawn, through display lists or the
 * Tessellator, while a layer draw is open for the player and the player's skin is bound. This covers Wawel Auth's
 * model, Even More Bends and Salamander alike, and draws from the skin's base with its semi-transparent alpha
 * ({@link SeeThroughBase}).
 * <p>
 * Improved transparency in common scenes, not correct transparency: glass on both sides of a player cannot be ordered.
 * Under Angelica's shaders the held texels are drawn as translucent entities ({@link AngelicaShaders}), and nothing is
 * split for the shadow map. All of it is here and behind {@link #opened}, {@link #paint}, {@link #splitsBase} and
 * {@link #flush}, switched by {@link SkinLayers3DConfig#translucentLayers} and
 * {@link SkinLayers3DConfig#translucentBase}.
 */
@SideOnly(Side.CLIENT)
public final class TranslucentLayers {

    /** Vanilla's hurt and death flash, as a tint: it would otherwise miss texels that write no depth. */
    private static final float HURT_TINT = 0.6F;
    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer COLOR = BufferUtils.createFloatBuffer(16);
    private static final List<Open> QUEUE = new ArrayList<>();
    private static final Comparator<Open> FARTHEST_FIRST = (a, b) -> Float.compare(b.distance, a.distance);
    private static long frame;
    private static float partialTicks;
    private static boolean deferring = true;
    private static boolean flushed;
    private static boolean registered;
    /** The innermost open player draw. */
    private static Open current;
    /** Above zero while a layer segment paints: what it draws isn't the base. */
    private static int layerDepth;
    private static boolean replaying;

    private TranslucentLayers() {}

    public static synchronized void register() {
        if (registered) return;
        FMLCommonHandler.instance()
            .bus()
            .register(new TranslucentLayers());
        registered = true;
    }

    private static boolean active() {
        return SkinLayers3DConfig.translucentLayers;
    }

    /**
     * A player's outermost layer draw opened: until {@link #closed}, its semi-transparent texels are held. Null when
     * this draw has nothing to split.
     */
    public static Open opened(Entity entity, DrawKind kind) {
        if (!active() || !(entity instanceof AbstractClientPlayer player)) return null;
        // The shadow map is drawn from the sun: its transforms would land in the world.
        if (AngelicaShaders.shadowPass()) return null;
        PlayerSkin skin = PlayerSkins.get(player);
        int base = SkinLayers3DConfig.translucentBase ? SeeThroughBase.texture(skin) : SeeThroughBase.NONE;
        int skinTexture = base == SeeThroughBase.NONE ? 0 : glTexture(skin);
        Open open = new Open(player, skin, kind, base, skinTexture, current);
        current = open;
        return open;
    }

    /** The draw {@link #opened} closed: its held texels are drawn now, or queued for after the translucent terrain. */
    public static void closed(Open open) {
        if (current == open) current = open.outer;
        if (open.records.isEmpty()) return;
        if (open.deferred()) QUEUE.add(open);
        else paint(Collections.singletonList(open), null);
    }

    /** Paints a player's layer segment: its semi-transparent texels split off if it has any. */
    public static void paint(Entity entity, LayerSegment segment, boolean volumetric, LayerPainter painter) {
        layerDepth++;
        try {
            Open open = current;
            // Vanilla's hurt flash repaints the model untextured: left as before.
            if (open == null || open.player != entity
                || !GL11.glIsEnabled(GL11.GL_TEXTURE_2D)
                || !TranslucentTexels.present(open.skin, segment)) {
                painter.paint(segment, volumetric);
                return;
            }
            opaque(() -> painter.paint(segment, volumetric));
            open.hold(tint -> painter.paint(segment, volumetric), GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
        } finally {
            layerDepth--;
        }
    }

    /**
     * Whether the draw about to happen is the open player's base: drawn textured from its skin, while the skin's base
     * has semi-transparent texels, outside layer segments and display list compiles.
     */
    public static boolean splitsBase() {
        Open open = current;
        if (open == null || open.base == SeeThroughBase.NONE || layerDepth > 0 || replaying) return false;
        if (!GL11.glIsEnabled(GL11.GL_TEXTURE_2D) || GL11.glGetInteger(GL11.GL_LIST_INDEX) != 0) return false;
        return GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D) == open.skinTexture;
    }

    /** A base display list, after {@link #splitsBase}: its opaque texels drawn through {@code call}, the rest held. */
    public static void splitBase(int list, Runnable call) {
        splitBase(call, tint -> GL11.glCallList(list));
    }

    /**
     * A base Tessellator draw, after {@link #splitsBase}: its vertices copied, its opaque texels drawn through
     * {@code draw}, the rest held, to be handed to the Tessellator again.
     */
    public static int splitBase(AccessorTessellator tessellator, IntSupplier draw) {
        TessellatorRecord record = new TessellatorRecord(tessellator);
        int[] drawn = new int[1];
        splitBase(() -> drawn[0] = draw.getAsInt(), record);
        return drawn[0];
    }

    private static void splitBase(Runnable draw, Replay replay) {
        Open open = current;
        int texture = open.base == SeeThroughBase.SKIN ? open.skinTexture : open.base;
        if (texture != open.skinTexture) GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        try {
            opaque(draw);
        } finally {
            if (texture != open.skinTexture) GL11.glBindTexture(GL11.GL_TEXTURE_2D, open.skinTexture);
        }
        open.hold(replay, texture);
    }

    /** The opaque texels only, writing depth as usual. */
    private static void opaque(Runnable draw) {
        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT);
        try {
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GEQUAL, 1.0F);
            draw.run();
        } finally {
            GL11.glPopAttrib();
        }
    }

    /** Paints the queued players' held texels: after the translucent terrain and the second entity pass. */
    public static void flush() {
        flushed = true;
        if (QUEUE.isEmpty()) return;
        QUEUE.sort(FARTHEST_FIRST);
        // The entity passes turn the lightmap off as they end.
        Minecraft.getMinecraft().entityRenderer.enableLightmap(partialTicks);
        AngelicaShaders.Scope shaders = AngelicaShaders.beginEntities();
        try {
            paint(QUEUE, shaders);
        } finally {
            QUEUE.clear();
            AngelicaShaders.endEntities(shaders);
            Minecraft.getMinecraft().entityRenderer.disableLightmap(partialTicks);
        }
    }

    /**
     * Each player's held texels, blended over what is drawn without writing depth, opaque ones left out, each under
     * the transform, texture, color and light it was held with. Far sides first, outermost first; then near sides,
     * innermost first, so a layer's far side lands behind the base and its near side in front. Each player is tagged
     * for the shader pack while {@code shaders} is open.
     */
    private static void paint(List<Open> opens, AngelicaShaders.Scope shaders) {
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_TEXTURE_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_CURRENT_BIT
                | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_POLYGON_BIT);
        replaying = true;
        try {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_LESS, 1.0F);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            GL11.glEnable(GL11.GL_CULL_FACE);
            for (Open open : opens) {
                AngelicaShaders.entity(shaders, open.player);
                // Nothing belongs in front of the first-person hand, but shader packs draw water after it: it writes
                // depth, as its opaque texels do.
                GL11.glDepthMask(open.kind == DrawKind.FIRST_PERSON);
                float tint = open.hurt ? HURT_TINT : 1.0F;
                List<Held> records = open.records;
                for (int i = records.size() - 1; i >= 0; i--) records.get(i)
                    .paint(tint, true);
                for (Held held : records) held.paint(tint, false);
            }
        } finally {
            replaying = false;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    private static int glTexture(PlayerSkin skin) {
        ITextureObject texture = Minecraft.getMinecraft()
            .getTextureManager()
            .getTexture(skin.texture());
        return texture == null ? 0 : texture.getGlTextureId();
    }

    /** Whether a column-major transform mirrors: its linear part has a negative determinant. */
    static boolean mirrors(float[] m) {
        float det = m[0] * (m[5] * m[10] - m[6] * m[9]) - m[4] * (m[1] * m[10] - m[2] * m[9])
            + m[8] * (m[1] * m[6] - m[2] * m[5]);
        return det < 0;
    }

    private static void toggle(int capability, boolean on) {
        if (on) GL11.glEnable(capability);
        else GL11.glDisable(capability);
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!QUEUE.isEmpty() && !flushed && deferring) {
            // Another mod changed the world render so the flush never ran: paint everything in place from now on.
            deferring = false;
            WawelAuth.LOG
                .warn("See-through skin texels could not be sorted after translucent terrain; drawing them in place");
        }
        QUEUE.clear();
        current = null;
        layerDepth = 0;
        flushed = false;
        frame++;
        partialTicks = event.renderTickTime;
        SeeThroughBase.deleteRetired();
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> {
                TranslucentTexels.clear();
                SeeThroughBase.clear();
            });
    }

    /** Draws held geometry again: under the state it was held with, with the hurt tint for vertex colors. */
    interface Replay {

        void paint(float tint);
    }

    /** A player's open draw and the texels it holds back. */
    public static final class Open {

        final AbstractClientPlayer player;
        final PlayerSkin skin;
        final DrawKind kind;
        /** {@link SeeThroughBase#texture}. */
        final int base;
        /** The GL name of the skin's own texture, while {@link #base} has texels to split. */
        final int skinTexture;
        final Open outer;
        final boolean hurt;
        /** Whether the projection mirrors the screen, as the GUI's does: it turns front faces into back faces. */
        final boolean flipped;
        /** From the camera, to paint the farthest player first. */
        final float distance;
        final List<Held> records = new ArrayList<>();
        private Boolean deferred;

        Open(AbstractClientPlayer player, PlayerSkin skin, DrawKind kind, int base, int skinTexture, Open outer) {
            this.player = player;
            this.skin = skin;
            this.kind = kind;
            this.base = base;
            this.skinTexture = skinTexture;
            this.outer = outer;
            this.hurt = player.hurtTime > 0 || player.deathTime > 0;
            MATRIX.clear();
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, MATRIX);
            this.flipped = MATRIX.get(0) * MATRIX.get(5) < 0;
            MATRIX.clear();
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX);
            float x = MATRIX.get(12), y = MATRIX.get(13), z = MATRIX.get(14);
            this.distance = (float) Math.sqrt(x * x + y * y + z * z);
        }

        /** Whether the held texels wait for the translucent terrain: none lies between the camera and the player. */
        boolean deferred() {
            if (deferred == null)
                deferred = kind == DrawKind.WORLD && deferring && !TerrainBetween.between(player, frame, partialTicks);
            return deferred;
        }

        void hold(Replay replay, int texture) {
            deferred();
            records.add(new Held(replay, texture, flipped));
        }
    }

    /** A held draw and the state it was drawn under, copied: later draws may change any of it. */
    private static final class Held {

        final Replay replay;
        final int texture;
        final float[] matrix = new float[16];
        final float[] color = new float[4];
        final float lightX, lightY;
        final boolean rescale, lighting;
        /** Whether its faces wind the other way on screen, so its far sides are the front faces. */
        final boolean mirrored;

        Held(Replay replay, int texture, boolean flipped) {
            this.replay = replay;
            this.texture = texture;
            MATRIX.clear();
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX);
            MATRIX.get(matrix);
            mirrored = mirrors(matrix) != flipped;
            COLOR.clear();
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, COLOR);
            COLOR.get(color, 0, 4);
            lightX = OpenGlHelper.lastBrightnessX;
            lightY = OpenGlHelper.lastBrightnessY;
            rescale = GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL);
            lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        }

        /** Its far sides, or its near ones. */
        void paint(float tint, boolean far) {
            GL11.glCullFace(far != mirrored ? GL11.GL_FRONT : GL11.GL_BACK);
            MATRIX.clear();
            MATRIX.put(matrix)
                .flip();
            GL11.glLoadMatrix(MATRIX);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            toggle(GL12.GL_RESCALE_NORMAL, rescale);
            toggle(GL11.GL_LIGHTING, lighting);
            GL11.glColor4f(color[0], color[1] * tint, color[2] * tint, color[3]);
            replay.paint(tint);
        }
    }
}
