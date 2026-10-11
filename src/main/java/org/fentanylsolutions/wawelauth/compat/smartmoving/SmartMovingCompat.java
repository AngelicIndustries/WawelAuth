package org.fentanylsolutions.wawelauth.compat.smartmoving;

import cpw.mods.fml.common.Loader;

/** Smart Moving replaces the player model, and its limbs do not match voxel layers. */
public final class SmartMovingCompat {

    private static Boolean loaded;

    private SmartMovingCompat() {}

    public static boolean isLoaded() {
        if (loaded == null) loaded = Loader.isModLoaded("SmartMoving");
        return loaded;
    }
}
