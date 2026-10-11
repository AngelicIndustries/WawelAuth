package org.fentanylsolutions.wawelauth.api.skin;

/** Where a player model is being drawn. */
public enum DrawKind {

    /** In the world, including third person and other players. */
    WORLD,
    /** In a GUI, such as the inventory or an account preview. Ignores the 3D distance limit. */
    GUI,
    /** The first-person arm. Ignores the distance limit and armor, which is not drawn there. */
    FIRST_PERSON
}
