package industries.angelic.wawelauth.client.gui;

import java.util.List;
import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Column;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The account manager's menu, which drops down from its button like a File menu: a column of entries, each its icon
 * then its name. Picking one closes the menu and runs it; clicking anywhere else or pressing Escape just closes it.
 */
@SideOnly(Side.CLIENT)
final class AccountManagerMenu {

    private static final int ROW_HEIGHT = 16;
    private static final int ICON_SIZE = 12;
    private static final int PADDING = 2;
    private static final int TEXT_LEFT = 4 + ICON_SIZE + 5;
    private static final int TEXT_RIGHT_PADDING = 10;
    private static final int SEPARATOR_HEIGHT = 5;
    private static final int DISABLED_ICON_SHADE = 0x99000000;
    /** Light on the dark menu, where the buttons' darker hover wouldn't show. */
    private static final int HOVER = 0x26FFFFFF;

    private AccountManagerMenu() {}

    static final class Entry {

        final IDrawable icon;
        final IDrawable hoverIcon;
        final String label;
        final BooleanSupplier enabled;
        final Runnable action;

        Entry(IDrawable icon, String label, BooleanSupplier enabled, Runnable action) {
            this(icon, icon, label, enabled, action);
        }

        Entry(IDrawable icon, IDrawable hoverIcon, String label, BooleanSupplier enabled, Runnable action) {
            this.icon = icon;
            this.hoverIcon = hoverIcon;
            this.label = label;
            this.enabled = enabled;
            this.action = action;
        }
    }

    /** A line between groups of entries. */
    static final Entry SEPARATOR = new Entry(null, null, () -> false, null);

    /**
     * The menu with its top left corner at {@code x}, {@code y}; when it wouldn't fit below there, it opens upward from
     * {@code aboveY} instead.
     */
    static ModularPanel build(List<Entry> entries, int x, int y, int aboveY) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int width = 0;
        int height = PADDING * 2;
        for (Entry entry : entries) {
            if (entry == SEPARATOR) {
                height += SEPARATOR_HEIGHT;
            } else {
                width = Math.max(width, TEXT_LEFT + font.getStringWidth(entry.label) + TEXT_RIGHT_PADDING);
                height += ROW_HEIGHT;
            }
        }

        MenuPanel panel = new MenuPanel();
        Column column = new Column();
        column.widthRel(1.0f)
            .heightRel(1.0f)
            .padding(PADDING);
        for (Entry entry : entries) {
            column.child(entry == SEPARATOR ? separator() : row(panel, entry));
        }

        int screenHeight = new net.minecraft.client.gui.ScaledResolution(
            Minecraft.getMinecraft(),
            Minecraft.getMinecraft().displayWidth,
            Minecraft.getMinecraft().displayHeight).getScaledHeight();
        int top = y + height <= screenHeight ? y : Math.max(0, aboveY - height);
        WawelAuthStyle.dialog(panel);
        panel.size(width, height)
            .left(x)
            .top(top)
            .child(column);
        return panel;
    }

    private static ButtonWidget<?> row(ModularPanel panel, Entry entry) {
        ButtonWidget<?> row = new ButtonWidget<>();
        row.widthRel(1.0f)
            .height(ROW_HEIGHT)
            .background(IDrawable.EMPTY)
            .hoverBackground(WawelAuthStyle.dynamicRect(() -> entry.enabled.getAsBoolean() ? HOVER : 0x00000000))
            .overlay((context, x, y, width, height, widgetTheme) -> {
                boolean enabled = entry.enabled.getAsBoolean();
                int iconX = x + 4;
                int iconY = y + (height - ICON_SIZE) / 2;
                IDrawable icon = enabled && row.isBelowMouse() ? entry.hoverIcon : entry.icon;
                icon.draw(context, iconX, iconY, ICON_SIZE, ICON_SIZE, widgetTheme);
                if (!enabled) {
                    Gui.drawRect(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, DISABLED_ICON_SHADE);
                }
                FontRenderer font = Minecraft.getMinecraft().fontRenderer;
                font.drawString(
                    entry.label,
                    x + TEXT_LEFT,
                    y + (height - font.FONT_HEIGHT) / 2 + 1,
                    enabled ? WawelAuthStyle.TEXT_PRIMARY : WawelAuthStyle.TEXT_SECONDARY);
                GL11.glColor4f(1, 1, 1, 1);
            })
            .onMousePressed(mouseButton -> {
                if (!entry.enabled.getAsBoolean()) return true;
                panel.closeIfOpen();
                entry.action.run();
                return true;
            });
        return row;
    }

    private static Widget<?> separator() {
        return new Widget<>().widthRel(1.0f)
            .height(SEPARATOR_HEIGHT)
            .background((context, x, y, width, height, widgetTheme) -> {
                Gui.drawRect(x + 2, y + height / 2, x + width - 2, y + height / 2 + 1, 0x40FFFFFF);
                GL11.glColor4f(1, 1, 1, 1);
            });
    }

    /** Stays where it opened, leaves the screen behind it as it is, and closes on a click elsewhere. */
    private static final class MenuPanel extends ModularPanel {

        MenuPanel() {
            super("wawelauth_account_manager_menu");
        }

        @Override
        public boolean closeOnOutOfBoundsClick() {
            return true;
        }

        @Override
        public boolean disablePanelsBelow() {
            return false;
        }

        @Override
        public boolean isDraggable() {
            return false;
        }

        @Override
        public boolean shouldAnimate() {
            return false;
        }
    }
}
