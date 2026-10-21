package industries.angelic.wawelauth.client.skin.translucent;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Whether water, ice or stained glass lies between the camera and a player: sampled at the feet, middle and head from
 * the real camera, once per frame per player. A heuristic: it treats a whole block as covering and misses panes that
 * only partly hide the player.
 */
@SideOnly(Side.CLIENT)
final class TerrainBetween {

    private static final double MAX_DISTANCE = 128.0D;
    private static final double[] HEIGHTS = { 0.15D, 0.55D, 0.9D };
    private static final Map<Entity, Check> CHECKS = new WeakHashMap<>();

    private TerrainBetween() {}

    static boolean between(Entity entity, long frame, float partialTicks) {
        Check check = CHECKS.get(entity);
        if (check == null) {
            check = new Check();
            CHECKS.put(entity, check);
        }
        if (check.frame != frame) {
            check.frame = frame;
            check.between = look(entity, partialTicks);
        }
        return check.between;
    }

    private static boolean look(Entity entity, float partialTicks) {
        EntityLivingBase viewer = Minecraft.getMinecraft().renderViewEntity;
        World world = entity.worldObj;
        if (viewer == null || world == null) return true;
        Vec3 camera = ActiveRenderInfo.projectViewFromEntity(viewer, partialTicks);
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - entity.yOffset;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        for (double height : HEIGHTS) {
            double ty = y + entity.height * height;
            double dx = x - camera.xCoord, dy = ty - camera.yCoord, dz = z - camera.zCoord;
            if (dx * dx + dy * dy + dz * dz > MAX_DISTANCE * MAX_DISTANCE) return true;
            if (crosses(
                (bx, by, bz) -> translucent(world, bx, by, bz),
                camera.xCoord,
                camera.yCoord,
                camera.zCoord,
                x,
                ty,
                z)) return true;
        }
        return false;
    }

    /** Blocks drawn in the translucent terrain pass: water, ice, stained glass. */
    private static boolean translucent(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block.getMaterial() != Material.air && block.getRenderBlockPass() == 1;
    }

    interface Blocks {

        boolean translucent(int x, int y, int z);
    }

    /** Walks every block the segment passes through, from the start, and stops at the first translucent one. */
    static boolean crosses(Blocks blocks, double x0, double y0, double z0, double x1, double y1, double z1) {
        int x = floor(x0), y = floor(y0), z = floor(z0);
        int endX = floor(x1), endY = floor(y1), endZ = floor(z1);
        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1, stepZ = dz > 0 ? 1 : -1;
        double deltaX = dx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dx);
        double deltaY = dy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dy);
        double deltaZ = dz == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dz);
        double nextX = dx == 0 ? Double.POSITIVE_INFINITY : (dx > 0 ? x + 1 - x0 : x0 - x) * deltaX;
        double nextY = dy == 0 ? Double.POSITIVE_INFINITY : (dy > 0 ? y + 1 - y0 : y0 - y) * deltaY;
        double nextZ = dz == 0 ? Double.POSITIVE_INFINITY : (dz > 0 ? z + 1 - z0 : z0 - z) * deltaZ;
        for (int steps = 0; steps < 1024; steps++) {
            if (blocks.translucent(x, y, z)) return true;
            if (x == endX && y == endY && z == endZ) return false;
            if (nextX <= nextY && nextX <= nextZ) {
                if (nextX > 1) return false;
                x += stepX;
                nextX += deltaX;
            } else if (nextY <= nextZ) {
                if (nextY > 1) return false;
                y += stepY;
                nextY += deltaY;
            } else {
                if (nextZ > 1) return false;
                z += stepZ;
                nextZ += deltaZ;
            }
        }
        return false;
    }

    private static int floor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static final class Check {

        long frame = -1;
        boolean between;
    }
}
