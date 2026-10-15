package org.fentanylsolutions.wawelauth.client.skin.model;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.MinecraftForgeClient;

import org.fentanylsolutions.wawelauth.api.skin.DrawKind;
import org.fentanylsolutions.wawelauth.api.skin.LayerDraw;
import org.fentanylsolutions.wawelauth.api.skin.LayerSegment;
import org.fentanylsolutions.wawelauth.api.skin.SkinLayers;
import org.fentanylsolutions.wawelauth.api.skin.SkinPart;
import org.fentanylsolutions.wawelauth.client.fakeworld.PreviewEntityRenderContext;
import org.fentanylsolutions.wawelauth.wawelcore.data.SkinModel;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The 64x64 player model WawelAuth installs in the vanilla player renderer, after 1.8's ModelPlayer: left limbs with
 * their own UVs, separate classic and slim arms, and overlay layers drawn through {@link SkinLayers}.
 * <p>
 * The arm fields are swapped to the drawn player's model before each draw. Read {@code bipedRightArm} and
 * {@code bipedLeftArm} at draw time; do not cache them. The cape keeps the 64x32 cape layout.
 * <p>
 * It is installed by {@code RenderPlayer}'s constructor, subclasses included. A renderer that swaps in its own model
 * keeps the constructed one for vanilla-style draws such as the first-person arm, so the sleeve still works.
 */
@SideOnly(Side.CLIENT)
public class ModelPlayer extends ModelBiped {

    /** Overlay segments of the vanilla 64x64 layout, each in its part's space. */
    public static final LayerSegment HAT = LayerSegment.box(SkinPart.HAT, -4, -8, -4, 8, 8, 8, 32, 0);
    public static final LayerSegment JACKET = LayerSegment.box(SkinPart.JACKET, -4, 0, -2, 8, 12, 4, 16, 32);
    public static final LayerSegment RIGHT_SLEEVE = LayerSegment
        .box(SkinPart.RIGHT_SLEEVE, -3, -2, -2, 4, 12, 4, 40, 32);
    public static final LayerSegment RIGHT_SLEEVE_SLIM = LayerSegment
        .box(SkinPart.RIGHT_SLEEVE, -2, -2, -2, 3, 12, 4, 40, 32);
    public static final LayerSegment LEFT_SLEEVE = LayerSegment.box(SkinPart.LEFT_SLEEVE, -1, -2, -2, 4, 12, 4, 48, 48);
    public static final LayerSegment LEFT_SLEEVE_SLIM = LayerSegment
        .box(SkinPart.LEFT_SLEEVE, -1, -2, -2, 3, 12, 4, 48, 48);
    public static final LayerSegment RIGHT_PANTS = LayerSegment.box(SkinPart.RIGHT_PANTS, -2, 0, -2, 4, 12, 4, 0, 32);
    public static final LayerSegment LEFT_PANTS = LayerSegment.box(SkinPart.LEFT_PANTS, -2, 0, -2, 4, 12, 4, 0, 48);

    private final ModelRenderer classicRightArm;
    private final ModelRenderer classicLeftArm;
    private final ModelRenderer slimRightArm;
    private final ModelRenderer slimLeftArm;
    private SkinModel skinModel = SkinModel.CLASSIC;

    public ModelPlayer(float scale) {
        // Vanilla builds the cape (64x32 cape layout) and the 64x32 parts; the parts are rebuilt below.
        super(scale);
        this.boxList.clear();
        this.textureWidth = 64;
        this.textureHeight = 64;

        this.bipedHead = part(0, 0, -4, -8, -4, 8, 8, 8, scale, 0, 0, 0);
        this.bipedHeadwear = new Headwear(this);
        this.bipedHeadwear.addBox(-4, -8, -4, 8, 8, 8, scale + 0.5F);
        this.bipedEars = part(24, 0, -3, -6, -1, 6, 6, 1, scale, 0, 0, 0);
        this.bipedBody = part(16, 16, -4, 0, -2, 8, 12, 4, scale, 0, 0, 0);
        this.classicRightArm = part(40, 16, -3, -2, -2, 4, 12, 4, scale, -5, 2.0F, 0);
        this.slimRightArm = part(40, 16, -2, -2, -2, 3, 12, 4, scale, -5, 2.5F, 0);
        this.classicLeftArm = part(32, 48, -1, -2, -2, 4, 12, 4, scale, 5, 2.0F, 0);
        this.slimLeftArm = part(32, 48, -1, -2, -2, 3, 12, 4, scale, 5, 2.5F, 0);
        this.bipedRightLeg = part(0, 16, -2, 0, -2, 4, 12, 4, scale, -1.9F, 12, 0);
        this.bipedLeftLeg = part(16, 48, -2, 0, -2, 4, 12, 4, scale, 1.9F, 12, 0);
        this.bipedRightArm = this.classicRightArm;
        this.bipedLeftArm = this.classicLeftArm;
    }

    private ModelRenderer part(int u, int v, float x, float y, float z, int width, int height, int depth, float inflate,
        float pivotX, float pivotY, float pivotZ) {
        ModelRenderer part = new ModelRenderer(this, u, v);
        part.addBox(x, y, z, width, height, depth, inflate);
        part.setRotationPoint(pivotX, pivotY, pivotZ);
        return part;
    }

    public SkinModel getSkinModel() {
        return skinModel;
    }

    /** Swaps in the arms for this skin model. Call before each player's draw. */
    public void setSkinModel(SkinModel model) {
        this.skinModel = model == SkinModel.SLIM ? SkinModel.SLIM : SkinModel.CLASSIC;
        boolean slim = this.skinModel == SkinModel.SLIM;
        this.bipedRightArm = slim ? this.slimRightArm : this.classicRightArm;
        this.bipedLeftArm = slim ? this.slimLeftArm : this.classicLeftArm;
    }

    public LayerSegment rightSleeve() {
        return skinModel == SkinModel.SLIM ? RIGHT_SLEEVE_SLIM : RIGHT_SLEEVE;
    }

    public LayerSegment leftSleeve() {
        return skinModel == SkinModel.SLIM ? LEFT_SLEEVE_SLIM : LEFT_SLEEVE;
    }

    /**
     * Draws the vanilla parts, then the overlay layers. The draw is open around the vanilla parts too, so code that
     * replaces this model's rendering there can suppress this draw's layers or claim some of its parts. The headwear
     * part draws only once the draw is suppressed or the hat claimed; until then the hat layer stands in for it while
     * {@code bipedHeadwear} is shown.
     */
    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
        float headPitch, float scale) {
        try (LayerDraw draw = SkinLayers.begin(entity, drawKind(), scale)) {
            super.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            if (!draw.suppressed()) drawLayers(draw, scale, true);
        }
    }

    /** The right sleeve of the first-person arm, after the vanilla arm was drawn. */
    public void drawFirstPersonSleeve(LayerDraw draw, float scale) {
        submit(draw, this.bipedRightArm, rightSleeve(), scale);
    }

    /**
     * Submits the overlay layers on the parts' current poses, for renderers that draw this model part by part instead
     * of calling {@link #render}. The hat is left out when the vanilla headwear part already drew it.
     */
    public void drawLayers(LayerDraw draw, float scale, boolean hat) {
        if (this.isChild) {
            // Same groups and transforms as ModelBiped.render, headwear included in the body group.
            GL11.glPushMatrix();
            GL11.glScalef(0.5F, 0.5F, 0.5F);
            GL11.glTranslatef(0.0F, 24.0F * scale, 0.0F);
            drawBodyLayers(draw, scale, hat);
            GL11.glPopMatrix();
        } else {
            drawBodyLayers(draw, scale, hat);
        }
    }

    private void drawBodyLayers(LayerDraw draw, float scale, boolean hat) {
        if (hat) submit(draw, this.bipedHeadwear, HAT, scale);
        submit(draw, this.bipedBody, JACKET, scale);
        submit(draw, this.bipedRightArm, rightSleeve(), scale);
        submit(draw, this.bipedLeftArm, leftSleeve(), scale);
        submit(draw, this.bipedRightLeg, RIGHT_PANTS, scale);
        submit(draw, this.bipedLeftLeg, LEFT_PANTS, scale);
    }

    /** A hidden part has no transform, so its layer is skipped too, as is a part someone else claimed. */
    private static void submit(LayerDraw draw, ModelRenderer part, LayerSegment segment, float scale) {
        if (part.isHidden || !part.showModel || draw.claimed(segment.part())) return;
        GL11.glPushMatrix();
        GL11.glTranslatef(part.offsetX, part.offsetY, part.offsetZ);
        part.postRender(scale);
        draw.submit(segment);
        GL11.glPopMatrix();
    }

    /**
     * The vanilla headwear, drawn by {@code ModelBiped.render} only while the open layer draw is suppressed or its hat
     * claimed, so a model replacement that takes over the hat still gets it. Drawn directly when no draw is open.
     */
    private static final class Headwear extends ModelRenderer {

        Headwear(ModelBiped model) {
            super(model, 32, 0);
        }

        @Override
        public void render(float scale) {
            LayerDraw draw = SkinLayers.current();
            if (draw == null || draw.suppressed() || draw.claimed(SkinPart.HAT)) super.render(scale);
        }
    }

    /** World entity passes set a render pass; GUI drawing (inventory, previews) happens outside them. */
    private static DrawKind drawKind() {
        if (PreviewEntityRenderContext.isRenderingInGui || MinecraftForgeClient.getRenderPass() == -1) {
            return DrawKind.GUI;
        }
        return DrawKind.WORLD;
    }
}
