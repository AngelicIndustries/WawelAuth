package industries.angelic.wawelauth.client.gui;

import java.io.File;
import java.util.List;
import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.layout.IViewport;
import com.cleanroommc.modularui.api.layout.IViewportStack;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.HoveredWidgetList;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.cleanroommc.modularui.widgets.layout.Row;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.angellib.util.GuiText;
import industries.angelic.wawelauth.wawelclient.data.ClientProvider;
import industries.angelic.wawelauth.wawelclient.lan.LanProviders;

/** Picks whose players may join a singleplayer world once it's opened to LAN. */
@SideOnly(Side.CLIENT)
final class LanProvidersDialog {

    private static final int WIDTH = 200;
    private static final int PADDING = 6;
    private static final int ROW_HEIGHT = 16;
    private static final int MAX_VISIBLE_ROWS = 8;
    private static final int BOX_SIZE = 9;
    private static final int LABEL_MAX_WIDTH_PX = 160;

    private LanProvidersDialog() {}

    /** The providers of a world played with an account of {@code hostProvider}. */
    static Dialog<Boolean> build(File worldDirectory, ClientProvider hostProvider) {
        Dialog<Boolean> dialog = new Dialog<>("wawelauth_lan_providers");
        List<LanProviders.Row> rows = LanProviders.rows(worldDirectory, hostProvider);
        int visibleRows = Math.max(1, Math.min(rows.size(), MAX_VISIBLE_ROWS));

        ListWidget<IWidget, ?> list = new ProviderList();
        WawelAuthStyle.styleList(list);
        list.widthRel(1.0f)
            .height(visibleRows * ROW_HEIGHT)
            .margin(0, 4);
        for (LanProviders.Row row : rows) {
            list.child(providerRow(worldDirectory, row));
        }

        ButtonWidget<?> doneButton = new ButtonWidget<>();
        doneButton.size(70, 18)
            .onMousePressed(mouseButton -> {
                dialog.closeIfOpen();
                return true;
            });
        WawelAuthStyle.textButton(doneButton, 62, "gui.done");

        Column root = new Column();
        root.widthRel(1.0f)
            .heightRel(1.0f)
            .padding(PADDING)
            .background(IDrawable.EMPTY)
            .disableHoverBackground()
            .child(
                new TextWidget<>(IKey.str(GuiText.tr("wawelauth.gui.lan_providers.title"))).widthRel(1.0f)
                    .height(14)
                    .color(WawelAuthStyle.THEME_LIGHTER)
                    .addTooltipLine(GuiText.tr("wawelauth.gui.lan_providers.title_tooltip")))
            .child(list)
            .child(offlinePlayersRow(worldDirectory))
            .child(
                new Row().widthRel(1.0f)
                    .margin(0, 4, 0, 0)
                    .height(18)
                    .mainAxisAlignment(Alignment.MainAxis.CENTER)
                    .child(doneButton));

        WawelAuthStyle.dialog(dialog);
        dialog.size(WIDTH, PADDING * 2 + 14 + 8 + visibleRows * ROW_HEIGHT + ROW_HEIGHT + 4 + 18)
            .child(root);
        return dialog;
    }

    private static ButtonWidget<?> providerRow(File worldDirectory, LanProviders.Row row) {
        boolean[] accepted = { row.accepted };
        String label = GuiText
            .ellipsizeToPixelWidth(ProviderDisplayName.displayName(row.provider.getName()), LABEL_MAX_WIDTH_PX);

        ButtonWidget<?> button = new ButtonWidget<>();
        WawelAuthStyle.rowButton(button, () -> accepted[0]);
        button.widthRel(1.0f)
            .height(ROW_HEIGHT)
            .overlay(checkboxRow(label, () -> accepted[0], row.hostAccount))
            .onMousePressed(mouseButton -> {
                if (row.hostAccount) return true;
                accepted[0] = !accepted[0];
                LanProviders.setAccepted(worldDirectory, row.provider, accepted[0]);
                return true;
            });
        if (row.hostAccount) {
            button.addTooltipLine(GuiText.tr("wawelauth.gui.lan_providers.host_account"));
        }
        return button;
    }

    /** Whether players no ticked provider verifies still join, as offline players as in vanilla. */
    private static ButtonWidget<?> offlinePlayersRow(File worldDirectory) {
        boolean[] letIn = { LanProviders.letsOthersIn(worldDirectory) };
        String label = GuiText
            .ellipsizeToPixelWidth(GuiText.tr("wawelauth.gui.lan_providers.offline_players"), LABEL_MAX_WIDTH_PX);

        ButtonWidget<?> button = new ButtonWidget<>();
        WawelAuthStyle.rowButton(button, () -> letIn[0]);
        button.widthRel(1.0f)
            .height(ROW_HEIGHT)
            .overlay(checkboxRow(label, () -> letIn[0], false))
            .addTooltipLine(GuiText.tr("wawelauth.gui.lan_providers.offline_players_tooltip"))
            .onMousePressed(mouseButton -> {
                letIn[0] = !letIn[0];
                LanProviders.setLetsOthersIn(worldDirectory, letIn[0]);
                return true;
            });
        return button;
    }

    /** A tick box and the provider's name, left-aligned in the row. */
    private static IDrawable checkboxRow(String label, BooleanSupplier ticked, boolean locked) {
        return (context, x, y, width, height, widgetTheme) -> {
            int boxX = x + 4;
            int boxY = y + (height - BOX_SIZE) / 2;
            int border = locked ? WawelAuthStyle.TEXT_SECONDARY : WawelAuthStyle.TEXT_PRIMARY;
            Gui.drawRect(boxX, boxY, boxX + BOX_SIZE, boxY + BOX_SIZE, border);
            Gui.drawRect(boxX + 1, boxY + 1, boxX + BOX_SIZE - 1, boxY + BOX_SIZE - 1, 0xFF1A1A1A);
            if (ticked.getAsBoolean()) {
                Gui.drawRect(boxX + 2, boxY + 2, boxX + BOX_SIZE - 2, boxY + BOX_SIZE - 2, border);
            }

            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            int textColor = ticked.getAsBoolean() ? WawelAuthStyle.THEME_LIGHTER : WawelAuthStyle.TEXT_SECONDARY;
            font.drawString(label, boxX + BOX_SIZE + 5, y + (height - font.FONT_HEIGHT) / 2 + 1, textColor);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        };
    }

    private static class ProviderList extends ListWidget<IWidget, ProviderList> {

        @Override
        public void getWidgetsAt(IViewportStack stack, HoveredWidgetList widgets, int mouseX, int mouseY) {
            if (widgets.peek() == this) {
                IViewport.getChildrenAt(this, stack, widgets, mouseX, mouseY);
            }
        }
    }
}
