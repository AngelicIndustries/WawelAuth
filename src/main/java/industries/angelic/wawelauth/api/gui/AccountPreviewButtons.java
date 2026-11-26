package industries.angelic.wawelauth.api.gui;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.client.entity.AbstractClientPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.api.account.AccountInfo;

/**
 * Entries other mods add to the account manager's Menu, below Save skin and Save cape: each its icon then its name.
 * Each acts on the selected account, with the skin the preview shows. Register on the client during initialization;
 * client thread only.
 */
@SideOnly(Side.CLIENT)
public final class AccountPreviewButtons {

    private static final List<PreviewButton> BUTTONS = new CopyOnWriteArrayList<>();

    private AccountPreviewButtons() {}

    /** One Menu entry: its icon, its name and what picking it does. */
    public interface PreviewButton {

        /**
         * Draws the icon in its square left of the name (12 pixels in the Menu); {@code hovered} while the pointer is
         * on the entry. Texturing may be on or off on entry; leave the color white.
         */
        void drawIcon(int x, int y, int width, int height, boolean hovered);

        /** The entry's name, already translated. */
        String tooltip();

        /**
         * Picked while {@code account} is selected and its preview has loaded (until then the entry is greyed out).
         * {@code preview} is the player the preview draws: the account's skin and arm model as shown. Opening a screen
         * of one's own closes the account manager; {@code reopen} opens it again with this account selected, over the
         * screen it was opened from.
         */
        void pressed(AccountInfo account, AbstractClientPlayer preview, Runnable reopen);
    }

    public static void register(PreviewButton button) {
        if (button == null) throw new IllegalArgumentException("null preview button");
        BUTTONS.add(button);
    }

    /** The registered buttons, in the order they were registered. */
    public static List<PreviewButton> buttons() {
        return Collections.unmodifiableList(BUTTONS);
    }
}
