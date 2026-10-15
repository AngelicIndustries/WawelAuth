package org.fentanylsolutions.wawelauth.compat.armoredarms;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderPlayer;

import org.fentanylsolutions.wawelauth.WawelAuth;
import org.fentanylsolutions.wawelauth.api.skin.DrawKind;
import org.fentanylsolutions.wawelauth.api.skin.LayerDraw;
import org.fentanylsolutions.wawelauth.api.skin.LayerSegment;
import org.fentanylsolutions.wawelauth.api.skin.SkinLayers;
import org.fentanylsolutions.wawelauth.api.skin.SkinPart;
import org.fentanylsolutions.wawelauth.client.skin.model.ModelPlayer;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * ArmoredArms draws the first-person arm itself, then its armor over it. The sleeve is submitted right after the arm,
 * on the arm's transform, and claimed so the first-person draw around it doesn't add a second one. ArmoredArms clears
 * {@code shouldRenderWear} when the armor should hide the sleeve.
 */
@SideOnly(Side.CLIENT)
public final class ArmoredArmsCompat {

    private static final float SCALE = 0.0625F;
    private static Field renderPlayer;
    private static Field shouldRenderWear;
    private static boolean failed;

    private ArmoredArmsCompat() {}

    public static void afterArm(Object manager, Object side) {
        if (failed) return;
        try {
            if (renderPlayer == null) {
                renderPlayer = manager.getClass()
                    .getField("renderPlayer");
                shouldRenderWear = manager.getClass()
                    .getField("shouldRenderWear");
            }
            RenderPlayer renderer = (RenderPlayer) renderPlayer.get(manager);
            if (renderer == null || !(renderer.modelBipedMain instanceof ModelPlayer model)) return;
            boolean right = "RIGHT".equals(((Enum<?>) side).name());
            ModelRenderer arm = right ? model.bipedRightArm : model.bipedLeftArm;
            LayerSegment sleeve = right ? model.rightSleeve() : model.leftSleeve();
            drawSleeve(arm, sleeve, shouldRenderWear.getBoolean(manager));
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            WawelAuth.LOG.error("ArmoredArms sleeve support failed; first-person sleeves are off with ArmoredArms", e);
        }
    }

    private static void drawSleeve(ModelRenderer arm, LayerSegment sleeve, boolean wear) {
        SkinPart part = sleeve.part();
        LayerDraw outer = SkinLayers.current();
        if (outer != null) outer.claim(part);
        if (!wear || arm.isHidden || !arm.showModel) return;
        LayerDraw own = outer == null
            ? SkinLayers.begin(Minecraft.getMinecraft().thePlayer, DrawKind.FIRST_PERSON, SCALE)
            : null;
        try {
            GL11.glPushMatrix();
            GL11.glTranslatef(arm.offsetX, arm.offsetY, arm.offsetZ);
            arm.postRender(SCALE);
            (own != null ? own : outer).submit(sleeve);
            GL11.glPopMatrix();
        } finally {
            if (own != null) own.close();
        }
    }
}
