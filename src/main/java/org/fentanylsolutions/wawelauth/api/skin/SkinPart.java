package org.fentanylsolutions.wawelauth.api.skin;

/**
 * The skin parts a player can show or hide, in the order of the 1.8+ part ids.
 */
public enum SkinPart {

    CAPE,
    JACKET,
    LEFT_SLEEVE,
    RIGHT_SLEEVE,
    LEFT_PANTS,
    RIGHT_PANTS,
    HAT;

    public static final SkinPart[] VALUES = values();
}
