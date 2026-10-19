package industries.angelic.wawelauth.compat.smartmoving;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.api.skin.PlayerSkins;
import industries.angelic.wawelauth.api.skin.SkinPart;
import industries.angelic.wawelauth.api.skin.ViewerSkinSettings;
import industries.angelic.wawelauth.client.render.SkinModelHelper;
import industries.angelic.wawelauth.client.skin.layers.LayerDraws;
import industries.angelic.wawelauth.wawelcore.data.SkinModel;

/**
 * Smart Render, Smart Moving's renderer, builds its player model as a vanilla 64x32 model and copies each part's boxes
 * and child parts into its own animated renderers. Before that copy, the main model is rebuilt on the 64x64 layout. The
 * hat, jacket, sleeves and pants become child parts, which Smart Render carries along, and the arms carry both the
 * classic and the slim shape. {@link #prepare} picks the arm shape and part visibility before each player's draw.
 * Layers stay flat: Smart Render's limbs don't fit the voxel meshes.
 */
@SideOnly(Side.CLIENT)
public final class SmartRenderModels {

    private static final Map<ModelBiped, Parts> MODELS = new WeakHashMap<>();

    private SmartRenderModels() {}

    /** Call before Smart Render copies the parts, on the main player model only; armor models keep their layout. */
    public static void modernize(ModelBiped model) {
        model.textureWidth = 64;
        model.textureHeight = 64;
        Parts parts = new Parts();

        rebuild(model.bipedHead, 0, 0, -4, -8, -4, 8, 8, 8);
        model.bipedHeadwear.cubeList.clear();
        parts.hat = child(model, model.bipedHeadwear, 32, 0, -4, -8, -4, 8, 8, 8, 0.5F, 0);

        rebuild(model.bipedBody, 16, 16, -4, 0, -2, 8, 12, 4);
        parts.jacket = child(model, model.bipedBody, 16, 32, -4, 0, -2, 8, 12, 4, 0.25F, 0);

        // Slim arms hang half a pixel lower, as in 1.8.
        model.bipedRightArm.cubeList.clear();
        parts.rightArm = child(model, model.bipedRightArm, 40, 16, -3, -2, -2, 4, 12, 4, 0, 0);
        parts.rightArmSlim = child(model, model.bipedRightArm, 40, 16, -2, -2, -2, 3, 12, 4, 0, 0.5F);
        parts.rightSleeve = child(model, model.bipedRightArm, 40, 32, -3, -2, -2, 4, 12, 4, 0.25F, 0);
        parts.rightSleeveSlim = child(model, model.bipedRightArm, 40, 32, -2, -2, -2, 3, 12, 4, 0.25F, 0.5F);
        model.bipedLeftArm.cubeList.clear();
        parts.leftArm = child(model, model.bipedLeftArm, 32, 48, -1, -2, -2, 4, 12, 4, 0, 0);
        parts.leftArmSlim = child(model, model.bipedLeftArm, 32, 48, -1, -2, -2, 3, 12, 4, 0, 0.5F);
        parts.leftSleeve = child(model, model.bipedLeftArm, 48, 48, -1, -2, -2, 4, 12, 4, 0.25F, 0);
        parts.leftSleeveSlim = child(model, model.bipedLeftArm, 48, 48, -1, -2, -2, 3, 12, 4, 0.25F, 0.5F);

        rebuild(model.bipedRightLeg, 0, 16, -2, 0, -2, 4, 12, 4);
        parts.rightPants = child(model, model.bipedRightLeg, 0, 32, -2, 0, -2, 4, 12, 4, 0.25F, 0);
        rebuild(model.bipedLeftLeg, 16, 48, -2, 0, -2, 4, 12, 4);
        parts.leftPants = child(model, model.bipedLeftLeg, 0, 48, -2, 0, -2, 4, 12, 4, 0.25F, 0);

        synchronized (MODELS) {
            MODELS.put(model, parts);
        }
    }

    /** Shows this player's arm shape and visible layers. A no-op for any other model. */
    public static void prepare(ModelBiped model, EntityPlayer player) {
        Parts parts;
        synchronized (MODELS) {
            parts = MODELS.get(model);
        }
        if (parts == null) return;
        boolean slim = player instanceof AbstractClientPlayer client
            && SkinModelHelper.getSkinModel(client) == SkinModel.SLIM;
        parts.rightArm.showModel = !slim;
        parts.rightArmSlim.showModel = slim;
        parts.leftArm.showModel = !slim;
        parts.leftArmSlim.showModel = slim;

        parts.hat.showModel = shown(player, SkinPart.HAT);
        parts.jacket.showModel = shown(player, SkinPart.JACKET);
        boolean rightSleeve = shown(player, SkinPart.RIGHT_SLEEVE);
        parts.rightSleeve.showModel = rightSleeve && !slim;
        parts.rightSleeveSlim.showModel = rightSleeve && slim;
        boolean leftSleeve = shown(player, SkinPart.LEFT_SLEEVE);
        parts.leftSleeve.showModel = leftSleeve && !slim;
        parts.leftSleeveSlim.showModel = leftSleeve && slim;
        parts.rightPants.showModel = shown(player, SkinPart.RIGHT_PANTS);
        parts.leftPants.showModel = shown(player, SkinPart.LEFT_PANTS);
    }

    private static boolean shown(EntityPlayer player, SkinPart part) {
        if (!PlayerSkins.isShown(player, part)) return false;
        return !ViewerSkinSettings.hideOverlayUnderArmor() || !LayerDraws.coveredByArmor(player, part);
    }

    private static void rebuild(ModelRenderer part, int u, int v, float x, float y, float z, int width, int height,
        int depth) {
        part.setTextureSize(64, 64);
        part.cubeList.clear();
        part.setTextureOffset(u, v);
        part.mirror = false;
        part.addBox(x, y, z, width, height, depth, 0);
    }

    private static ModelRenderer child(ModelBiped model, ModelRenderer parent, int u, int v, float x, float y, float z,
        int width, int height, int depth, float inflate, float dropY) {
        ModelRenderer child = new ModelRenderer(model, u, v);
        child.setTextureSize(64, 64);
        child.addBox(x, y, z, width, height, depth, inflate);
        child.setRotationPoint(0, dropY, 0);
        parent.addChild(child);
        return child;
    }

    private static final class Parts {

        ModelRenderer hat, jacket;
        ModelRenderer rightArm, rightArmSlim, rightSleeve, rightSleeveSlim;
        ModelRenderer leftArm, leftArmSlim, leftSleeve, leftSleeveSlim;
        ModelRenderer rightPants, leftPants;
    }
}
