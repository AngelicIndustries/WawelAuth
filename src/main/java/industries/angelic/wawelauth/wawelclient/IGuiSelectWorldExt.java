package industries.angelic.wawelauth.wawelclient;

import net.minecraft.world.storage.SaveFormatComparator;

/** The world list screen's worlds, as its list shows them now (sorted or filtered by other mods). */
public interface IGuiSelectWorldExt {

    SaveFormatComparator wawelauth$getWorld(int index);
}
