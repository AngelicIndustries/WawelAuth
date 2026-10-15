package org.fentanylsolutions.wawelauth.client.skin.layers;

import java.util.EnumSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import org.fentanylsolutions.wawelauth.WawelAuth;
import org.fentanylsolutions.wawelauth.api.skin.DrawKind;
import org.fentanylsolutions.wawelauth.api.skin.LayerDraw;
import org.fentanylsolutions.wawelauth.api.skin.LayerSegment;
import org.fentanylsolutions.wawelauth.api.skin.PlayerSkin;
import org.fentanylsolutions.wawelauth.api.skin.PlayerSkins;
import org.fentanylsolutions.wawelauth.api.skin.SkinPart;
import org.fentanylsolutions.wawelauth.api.skin.ViewerSkinSettings;
import org.fentanylsolutions.wawelauth.compat.smartmoving.SmartMovingCompat;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Implementation behind {@link org.fentanylsolutions.wawelauth.api.skin.SkinLayers}. Client thread only. */
@SideOnly(Side.CLIENT)
public final class LayerDraws {

    private static final DrawStack<Draw> STACK = new DrawStack<>();
    private static boolean registered;
    private static boolean leakLogged;
    private static boolean orderLogged;

    private LayerDraws() {}

    public static synchronized void register() {
        if (registered) return;
        FMLCommonHandler.instance()
            .bus()
            .register(new LayerDraws());
        registered = true;
    }

    public static LayerDraw begin(Entity entity, DrawKind kind, float scale) {
        VoxelLayers.sweepIfDue();
        Draw draw = new Draw(entity, kind, scale, voxelsAllowed(entity, kind));
        Draw outer = STACK.peek();
        if (outer != null && outer.suppressed && outer.entity == entity) {
            draw.suppressed = true;
        }
        if (!STACK.push(draw) && !leakLogged) {
            leakLogged = true;
            WawelAuth.LOG.error("Skin layer draws were opened without being closed; dropped them");
        }
        return draw;
    }

    public static LayerDraw current() {
        return STACK.peek();
    }

    private static boolean voxelsAllowed(Entity entity, DrawKind kind) {
        if (!(entity instanceof AbstractClientPlayer player) || SmartMovingCompat.isLoaded()) return false;
        if (kind != DrawKind.WORLD) return true;
        Entity viewer = Minecraft.getMinecraft().renderViewEntity;
        if (viewer == null || viewer == player) return true;
        double lod = ViewerSkinSettings.lodDistance();
        return player.getDistanceSqToEntity(viewer) < lod * lod;
    }

    /** Helmet covers the hat, chestplate the jacket and sleeves, leggings or boots the pants. */
    public static boolean coveredByArmor(EntityPlayer player, SkinPart part) {
        if (player.inventory == null) return false;
        return switch (part) {
            case HAT -> player.inventory.armorItemInSlot(3) != null;
            case JACKET, LEFT_SLEEVE, RIGHT_SLEEVE -> player.inventory.armorItemInSlot(2) != null;
            case LEFT_PANTS, RIGHT_PANTS -> player.inventory.armorItemInSlot(1) != null
                || player.inventory.armorItemInSlot(0) != null;
            case CAPE -> false;
        };
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft()
            .func_152344_a(VoxelLayers::clear);
    }

    private static final class Draw implements LayerDraw {

        private final Entity entity;
        private final DrawKind kind;
        private final float scale;
        private final boolean voxels;
        private PlayerSkin skin;
        private boolean suppressed;
        private final EnumSet<SkinPart> claimed = EnumSet.noneOf(SkinPart.class);
        private boolean closed;

        Draw(Entity entity, DrawKind kind, float scale, boolean voxels) {
            this.entity = entity;
            this.kind = kind;
            this.scale = scale;
            this.voxels = voxels;
        }

        @Override
        public void submit(LayerSegment segment) {
            if (closed) throw new IllegalStateException("Layer draw is already closed");
            if (suppressed) return;

            SkinPart part = segment.part();
            boolean covered = false;
            if (entity instanceof EntityPlayer player) {
                if (!PlayerSkins.isShown(player, part)) return;
                covered = kind != DrawKind.FIRST_PERSON && coveredByArmor(player, part);
                if (covered && ViewerSkinSettings.hideOverlayUnderArmor()) return;
            }

            // voxels is only ever true for client players.
            if (voxels && !covered && ViewerSkinSettings.volumetric(part)) {
                if (skin == null) skin = PlayerSkins.get((AbstractClientPlayer) entity);
                if (VoxelLayers.draw(skin, segment, scale) != VoxelLayers.Result.UNAVAILABLE) return;
            }
            OverlayBoxes.draw(segment, scale);
        }

        @Override
        public void suppress() {
            suppressed = true;
        }

        @Override
        public boolean suppressed() {
            return suppressed;
        }

        @Override
        public void claim(SkinPart part) {
            claimed.add(part);
        }

        @Override
        public boolean claimed(SkinPart part) {
            return claimed.contains(part);
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            if (!STACK.remove(this) && !orderLogged) {
                orderLogged = true;
                WawelAuth.LOG.error("Skin layer draws were closed out of order");
            }
        }
    }
}
