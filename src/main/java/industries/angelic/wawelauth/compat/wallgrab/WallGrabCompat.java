package industries.angelic.wawelauth.compat.wallgrab;

import java.lang.reflect.Method;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.WawelAuth;

/**
 * Wall Grab puts its own posed bipeds into the player renderer once loaded: vanilla 64x32 models, which drop the
 * 64x64 skin and its layers. The player renderer keeps {@code ModelPlayer} instead, and Wall Grab's pose is applied to
 * it. Wall Grab's armor models stay, posed the same way.
 */
@SideOnly(Side.CLIENT)
public final class WallGrabCompat {

    private static final String MODEL = "com.bogdantokarev.wallgrab.client.model.WallGrabModelBiped";
    private static final String POSE = "com.bogdantokarev.wallgrab.client.PoseHook";
    private static final boolean LOADED = Loader.isModLoaded("wallgrab");
    private static boolean resolved;
    private static Method apply;

    private WallGrabCompat() {}

    /** Whether {@code model} is Wall Grab's stand-in for the player model. */
    public static boolean replaced(Object model) {
        return LOADED && model != null
            && model.getClass()
                .getName()
                .equals(MODEL);
    }

    /** Wall Grab's wall slide and wall jump on {@code model}, after its vanilla pose is set. */
    public static void pose(ModelBiped model, Entity entity) {
        if (!LOADED || !resolve()) return;
        try {
            apply.invoke(null, model, entity);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            apply = null;
            WawelAuth.LOG.warn("Wall Grab's pose could not be applied to the player model", e);
        }
    }

    private static boolean resolve() {
        if (!resolved) {
            resolved = true;
            try {
                apply = Class.forName(POSE)
                    .getMethod("apply", ModelBiped.class, Entity.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                WawelAuth.LOG.warn("Wall Grab's pose is not available to the player model", e);
            }
        }
        return apply != null;
    }
}
