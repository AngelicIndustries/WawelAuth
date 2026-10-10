package org.fentanylsolutions.wawelauth.api;

import net.minecraft.entity.player.EntityPlayer;

import org.fentanylsolutions.wawelauth.api.skin.SkinPart;
import org.fentanylsolutions.wawelauth.wawelcore.skin.IPlayerCapeState;
import org.fentanylsolutions.wawelauth.wawelcore.skin.SkinPartFlags;
import org.fentanylsolutions.wawelauth.wawelcore.skin.SkinPartLayout;

public class SkinLayersHelper {

    public enum EnumPlayerModelParts {

        CAPE(0, "wawelauth.gui.skincustomization.cape", false),
        JACKET(1, "wawelauth.gui.skincustomization.jacket", true),
        LEFT_SLEEVE(2, "wawelauth.gui.skincustomization.left_sleeve", true),
        RIGHT_SLEEVE(3, "wawelauth.gui.skincustomization.right_sleeve", true),
        LEFT_PANTS(4, "wawelauth.gui.skincustomization.left_pants", true),
        RIGHT_PANTS(5, "wawelauth.gui.skincustomization.right_pants", true),
        HAT(6, "wawelauth.gui.skincustomization.hat", false);

        private final int partId;
        private final String partName;
        private final boolean partIsModern;

        EnumPlayerModelParts(int partId, String partName, boolean partIsModern) {
            this.partId = partId;
            this.partName = partName;
            this.partIsModern = partIsModern;
        }

        public int id() {
            return this.partId;
        }

        public String partName() {
            return this.partName;
        }

        public boolean isModern() {
            return this.partIsModern;
        }

        public SkinPart skinPart() {
            return SkinPart.VALUES[this.partId];
        }

        public static EnumPlayerModelParts fromId(int id) {
            for (EnumPlayerModelParts part : values()) {
                if (part.partId == id) return part;
            }
            return null;
        }
    }

    /** Decides how a player's flags are read. The client installs a server-aware reader. */
    public interface PartReader {

        boolean isHidden(EntityPlayer player, SkinPart part);
    }

    private static volatile PartReader reader = (player, part) -> SkinPartLayout.V2.isHidden(readFlags(player), part);

    public static void setPartReader(PartReader partReader) {
        reader = partReader;
    }

    public static boolean isSkinLayerHidden(EntityPlayer player, EnumPlayerModelParts part) {
        return isPartHidden(player, part.skinPart());
    }

    public static boolean isPartHidden(EntityPlayer player, SkinPart part) {
        return reader.isHidden(player, part);
    }

    /** The cape as vanilla rendering sees it: hidden by the wearer, the server or equipment. */
    public static boolean isCapeHidden(EntityPlayer player) {
        return isPartHidden(player, SkinPart.CAPE) || isCapeHiddenByEquipment(player);
    }

    /**
     * Writes a part in protocol v2 on the player's own DataWatcher. Only for server-side players and local
     * preview entities; clients never write the flags of networked players.
     */
    public static void setSkinLayerHidden(EntityPlayer player, EnumPlayerModelParts part, boolean hidden) {
        int flags = SkinPartFlags.withPart(readFlags(player), part.skinPart(), hidden);
        writeFlags(player, SkinPartFlags.composeCape(flags, isCapeHiddenByEquipment(player)));
    }

    /** Server side: the wearer's cape choice from the vanilla client settings packet. */
    public static void setWearerCapeHidden(EntityPlayer player, boolean hidden) {
        int flags = SkinPartFlags.withWearerCape(readFlags(player), hidden);
        writeFlags(player, SkinPartFlags.composeCape(flags, isCapeHiddenByEquipment(player)));
    }

    /** Server side: the non-cape parts sent by a v2 client. */
    public static void mergeParts(EntityPlayer player, int parts) {
        writeFlags(player, SkinPartFlags.mergeParts(readFlags(player), parts));
    }

    /** Server side: refreshes the vanilla cape bit after the equipment state changed. */
    public static void recomposeCape(EntityPlayer player) {
        writeFlags(player, SkinPartFlags.composeCape(readFlags(player), isCapeHiddenByEquipment(player)));
    }

    /** Reads slot 16 as unsigned. A non-byte value (a foreign or broken server) reads as "all shown". */
    public static int readFlags(EntityPlayer player) {
        try {
            return player.getDataWatcher()
                .getWatchableObjectByte(SkinPartFlags.DATA_WATCHER_ID) & 0xFF;
        } catch (ClassCastException e) {
            return 0;
        }
    }

    private static void writeFlags(EntityPlayer player, int flags) {
        player.getDataWatcher()
            .updateObject(SkinPartFlags.DATA_WATCHER_ID, (byte) flags);
    }

    private static boolean isCapeHiddenByEquipment(EntityPlayer player) {
        return player instanceof IPlayerCapeState state && state.wawelauth$isCapeHiddenByEquipment();
    }
}
