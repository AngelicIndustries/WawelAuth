package industries.angelic.wawelauth.compat.ears;

import java.nio.FloatBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.api.skin.LayerSegment;
import industries.angelic.wawelauth.api.skin.SkinPart;
import industries.angelic.wawelauth.client.skin.layers.LayerDraws;

/**
 * Ears' features on the bones a player's skin layers were drawn on. A renderer that bends limbs, such as Even More
 * Bends
 * or a CEM pack through Salamander, submits each overlay part on its own bone; Ears then anchors its ears, tail, wings
 * and claws to those transforms instead of the vanilla model's parts, and leaves the second layer to the draw that
 * already drew it. Limb features take the lowest slice of the limb, so claws ride the forearm and shin.
 * <p>
 * Only while Ears draws in {@link RenderPlayerEvent.Specials.Post}, right after the body, and only for transforms
 * recorded this frame. Elsewhere Ears anchors to the model as usual.
 */
@SideOnly(Side.CLIENT)
public final class EarsAnchors {

    private static final float PIXEL = 0.0625F;
    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
    private static final Map<Entity, Record> RECORDS = new WeakHashMap<>();
    private static long frame;
    private static Record current;
    private static boolean registered;

    private EarsAnchors() {}

    /** Whether Ears' renderer was opened to this anchoring. */
    public static boolean enabled() {
        return EarsTransformerAccess.opened();
    }

    public static synchronized void register() {
        if (registered || !enabled()) return;
        EarsAnchors handler = new EarsAnchors();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance()
            .bus()
            .register(handler);
        registered = true;
    }

    /** An entity's outermost layer draw opened: what follows is this frame's record of it. */
    public static void opened(Entity entity) {
        if (!enabled() || entity == null) return;
        RECORDS.put(entity, new Record(frame));
    }

    /** The transform a segment is submitted under, whether or not it then draws. For a limb the lowest slice wins. */
    public static void submitted(Entity entity, LayerSegment segment) {
        if (!enabled() || entity == null) return;
        Record record = RECORDS.get(entity);
        if (record == null || record.frame != frame) return;
        Capture old = record.parts.get(segment.part());
        if (old != null && old.segment.rowEnd() > segment.rowEnd()) return;
        MATRIX.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX);
        float[] matrix = new float[16];
        MATRIX.get(matrix);
        record.parts.put(segment.part(), new Capture(segment, matrix));
    }

    /**
     * Ears' anchor for one of its body parts, by name: the bone that part's layer was drawn on, at the box's lower
     * front
     * corner, where Ears measures its features from, in pixels. False to let Ears anchor to its model as usual.
     */
    public static boolean anchor(String bodyPart, ModelRenderer modelPart) {
        Record record = current;
        if (record == null) return false;
        SkinPart part = part(bodyPart);
        Capture capture = part == null ? null : record.parts.get(part);
        if (capture != null) {
            MATRIX.clear();
            MATRIX.put(capture.matrix)
                .flip();
            GL11.glLoadMatrix(MATRIX);
            GL11.glScalef(PIXEL, PIXEL, PIXEL);
            LayerSegment segment = capture.segment;
            GL11.glTranslatef(segment.originX(), segment.originY() + segment.height(), segment.originZ());
            return true;
        }
        // The renderer drew this player but not this part: its own transform, at the vanilla corner, without reading
        // boxes the renderer's parts may not have.
        float[] corner = corner(bodyPart, Boolean.TRUE.equals(slim()));
        if (corner == null) return false;
        modelPart.postRender(PIXEL);
        GL11.glScalef(PIXEL, PIXEL, PIXEL);
        GL11.glTranslatef(corner[0], corner[1], corner[2]);
        return true;
    }

    /**
     * Whether Ears should leave the second layer out: the player's draw submits it, and Wawel Auth decides which parts
     * show. Parts the renderer left out are hidden on purpose.
     */
    public static boolean drewLayers() {
        if (current != null) return true;
        // First person: an open draw for the arm submits the sleeve after Ears has drawn.
        return LayerDraws.firstPersonOpen();
    }

    /** Whether the drawn sleeves were slim, or null when nothing was recorded. */
    public static Boolean slim() {
        Record record = current;
        if (record == null) return null;
        Capture sleeve = record.parts.get(SkinPart.RIGHT_SLEEVE);
        if (sleeve == null) sleeve = record.parts.get(SkinPart.LEFT_SLEEVE);
        return sleeve == null ? null : sleeve.segment.width() == 3;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeSpecials(RenderPlayerEvent.Specials.Post event) {
        Record record = RECORDS.get(event.entityPlayer);
        current = record != null && record.frame == frame && !record.parts.isEmpty() ? record : null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void afterSpecials(RenderPlayerEvent.Specials.Post event) {
        current = null;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        frame++;
        current = null;
    }

    private static SkinPart part(String bodyPart) {
        switch (bodyPart) {
            case "HEAD":
                return SkinPart.HAT;
            case "TORSO":
                return SkinPart.JACKET;
            case "LEFT_ARM":
                return SkinPart.LEFT_SLEEVE;
            case "RIGHT_ARM":
                return SkinPart.RIGHT_SLEEVE;
            case "LEFT_LEG":
                return SkinPart.LEFT_PANTS;
            case "RIGHT_LEG":
                return SkinPart.RIGHT_PANTS;
            default:
                return null;
        }
    }

    /** The vanilla box's lower front corner for Ears' body part, relative to its pivot. */
    private static float[] corner(String bodyPart, boolean slim) {
        switch (bodyPart) {
            case "HEAD":
                return new float[] { -4, 0, -4 };
            case "TORSO":
                return new float[] { -4, 12, -2 };
            case "LEFT_ARM":
                return new float[] { -1, 10, -2 };
            case "RIGHT_ARM":
                return new float[] { slim ? -2 : -3, 10, -2 };
            case "LEFT_LEG":
            case "RIGHT_LEG":
                return new float[] { -2, 12, -2 };
            default:
                return null;
        }
    }

    private static final class Record {

        final long frame;
        final EnumMap<SkinPart, Capture> parts = new EnumMap<>(SkinPart.class);

        Record(long frame) {
            this.frame = frame;
        }
    }

    private static final class Capture {

        final LayerSegment segment;
        final float[] matrix;

        Capture(LayerSegment segment, float[] matrix) {
            this.segment = segment;
            this.matrix = matrix;
        }
    }
}
