package industries.angelic.wawelauth.client.gui;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import net.minecraft.client.gui.Gui;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.HueBar;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.angellib.util.GuiText;

/**
 * The cape creator's color picker: ModularUI's color sliders and hex field, in Wawel Auth's style. It opens on HSV,
 * and keeps the hue and saturation while the color is black or grey, where the color alone would lose them.
 */
@SideOnly(Side.CLIENT)
final class CapeColorPicker extends Dialog<Integer> {

    private static final int WIDTH = 176;
    private static final int HEIGHT = 124;
    private static final int PADDING = 7;
    private static final int ROW_HEIGHT = 12;
    private static final int ROW_GAP = 4;
    private static final int FIELD_HEIGHT = 14;
    private static final int LABEL_WIDTH = 14;
    private static final int NUMBER_WIDTH = 24;
    private static final int TRACK_HEIGHT = 6;
    private static final int TAB_WIDTH = 34;
    private static final int BUTTON_WIDTH = 64;
    private static final int BUTTON_HEIGHT = 16;
    private static final int SLIDERS_HEIGHT = ROW_HEIGHT * 3 + ROW_GAP * 2;

    /** Sodium's knob, outlined, so it shows on the light end of a track too. */
    private static final IDrawable KNOB = (context, x, y, width, height, widgetTheme) -> {
        Gui.drawRect(x, y, x + width, y + height, 0xFF000000);
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFFFFFFF);
        GL11.glColor4f(1, 1, 1, 1);
    };

    private final HueBar hueBar = new HueBar(GuiAxis.X);
    private int rgb;
    private float hue;
    private float saturation;
    private float value;

    CapeColorPicker(int rgb, IntConsumer picked) {
        super("wawelauth_cape_color", color -> { if (color != null) picked.accept(color); });
        setRgb(rgb);
        // Opaque: it opens over the cape creator, whose text would show through Wawel Auth's dialog background.
        WawelAuthStyle.dialog(this)
            .background(WawelAuthStyle.verticalGradient(0xFF0A0A0A, 0xFF000000));
        size(WIDTH, HEIGHT);

        PagedWidget.Controller pages = new PagedWidget.Controller();
        Column content = new Column();
        content.sizeRel(1.0f, 1.0f)
            .padding(PADDING)
            .child(header(pages))
            .child(gap(6))
            .child(hexRow())
            .child(gap(6))
            .child(
                new PagedWidget<>().widthRel(1.0f)
                    .height(SLIDERS_HEIGHT)
                    .controller(pages)
                    .addPage(hsvPage())
                    .addPage(rgbPage()))
            .child(
                new Widget<>().widthRel(1.0f)
                    .expanded())
            .child(buttons());
        child(content);
    }

    // -- The color, as RGB and as HSV --

    private void setRgb(int color) {
        rgb = color & 0xFFFFFF;
        float[] hsv = java.awt.Color.RGBtoHSB(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, null);
        value = hsv[2];
        if (value > 0) {
            saturation = hsv[1];
            if (saturation > 0) hue = hsv[0] * 360.0f;
        }
    }

    private void setHsv(float hue, float saturation, float value) {
        this.hue = hue;
        this.saturation = saturation;
        this.value = value;
        rgb = hsvToRgb(hue, saturation, value);
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        return java.awt.Color.HSBtoRGB(hue / 360.0f, saturation, value) & 0xFFFFFF;
    }

    private int channel(int shift) {
        return rgb >> shift & 0xFF;
    }

    private void setChannel(int shift, double channel) {
        int clamped = Math.max(0, Math.min(255, (int) Math.round(channel)));
        setRgb(rgb & ~(0xFF << shift) | clamped << shift);
    }

    // -- Layout --

    /** The title, then the HSV and RGB tabs at the right. */
    private static Row header(PagedWidget.Controller pages) {
        Row row = new Row();
        row.widthRel(1.0f)
            .height(FIELD_HEIGHT)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(
                new TextWidget<>(GuiText.key("wawelauth.gui.cape_creator.color")).expanded()
                    .height(FIELD_HEIGHT)
                    .alignment(Alignment.CenterLeft)
                    .color(WawelAuthStyle.THEME_LIGHTER))
            .child(tab(0, "HSV", pages))
            .child(new Widget<>().size(2, FIELD_HEIGHT))
            .child(tab(1, "RGB", pages));
        return row;
    }

    private static PageButton tab(int index, String name, PagedWidget.Controller pages) {
        PageButton tab = new PageButton(index, pages);
        tab.size(TAB_WIDTH, FIELD_HEIGHT)
            .background(true, WawelAuthStyle.flat(WawelAuthStyle.BUTTON_IDLE, () -> true))
            .background(false, WawelAuthStyle.rect(WawelAuthStyle.BUTTON_IDLE))
            .overlay(
                IKey.str(name)
                    .color(
                        () -> tab.isActive() ? WawelAuthStyle.THEME_LIGHTER
                            : tab.isBelowMouse() ? WawelAuthStyle.TEXT_PRIMARY : WawelAuthStyle.TEXT_SECONDARY));
        return tab;
    }

    /** The color as hex, editable, and a swatch of it. */
    private Row hexRow() {
        TextFieldWidget field = new TextFieldWidget();
        WawelAuthStyle.textField(field);
        field.expanded()
            .height(FIELD_HEIGHT)
            .setValidator(CapeColorPicker::withHash)
            .value(new StringValue.Dynamic(() -> String.format(Locale.ROOT, "#%06X", rgb), text -> {
                Integer parsed = parseHex(text);
                if (parsed != null) setRgb(parsed);
            }));
        Row row = new Row();
        row.widthRel(1.0f)
            .height(FIELD_HEIGHT)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(label("Hex").width(LABEL_WIDTH + 10))
            .child(field)
            .child(new Widget<>().size(6, FIELD_HEIGHT))
            .child(
                new Widget<>().size(NUMBER_WIDTH + 6, FIELD_HEIGHT)
                    .background((context, x, y, width, height, widgetTheme) -> {
                        Gui.drawRect(x, y, x + width, y + height, 0xFF000000 | rgb);
                        GL11.glColor4f(1, 1, 1, 1);
                    }));
        return row;
    }

    private Column hsvPage() {
        return page(
            sliderRow(
                "H",
                360,
                track(
                    (context, x, y, width, height, widgetTheme) -> hueBar
                        .draw(context, x, y, width, height, widgetTheme)),
                () -> hue,
                h -> setHsv((float) h, saturation, value),
                () -> Math.round(hue)),
            sliderRow(
                "S",
                100,
                gradient(() -> hsvToRgb(hue, 0, value), () -> hsvToRgb(hue, 1, value)),
                () -> saturation * 100,
                s -> setHsv(hue, (float) s / 100.0f, value),
                () -> Math.round(saturation * 100)),
            sliderRow(
                "V",
                100,
                gradient(() -> hsvToRgb(hue, saturation, 0), () -> hsvToRgb(hue, saturation, 1)),
                () -> value * 100,
                v -> setHsv(hue, saturation, (float) v / 100.0f),
                () -> Math.round(value * 100)));
    }

    private Column rgbPage() {
        return page(channelRow("R", 16), channelRow("G", 8), channelRow("B", 0));
    }

    private Row channelRow(String name, int shift) {
        return sliderRow(
            name,
            255,
            gradient(() -> rgb & ~(0xFF << shift), () -> rgb | 0xFF << shift),
            () -> channel(shift),
            channel -> setChannel(shift, channel),
            () -> channel(shift));
    }

    private static Column page(Row first, Row second, Row third) {
        Column page = new Column();
        page.sizeRel(1.0f, 1.0f)
            .child(first)
            .child(gap(ROW_GAP))
            .child(second)
            .child(gap(ROW_GAP))
            .child(third);
        return page;
    }

    /** A name, a slider over a track of the colors it reaches, and the number it's at. */
    private static Row sliderRow(String name, double max, IDrawable track, DoubleSupplier get, DoubleConsumer set,
        IntSupplier shown) {
        SliderWidget slider = new SliderWidget();
        slider.bounds(0, max)
            .sliderTexture(KNOB)
            .sliderSize(4, ROW_HEIGHT)
            .background(track)
            .hoverBackground(track)
            .value(new DoubleValue.Dynamic(get, set))
            .expanded()
            .height(ROW_HEIGHT);
        Row row = new Row();
        row.widthRel(1.0f)
            .height(ROW_HEIGHT)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(label(name).width(LABEL_WIDTH))
            .child(slider)
            .child(
                new TextWidget<>(IKey.dynamic(() -> String.valueOf(shown.getAsInt()))).width(NUMBER_WIDTH)
                    .height(ROW_HEIGHT)
                    .alignment(Alignment.CenterRight)
                    .color(WawelAuthStyle.TEXT_SECONDARY));
        return row;
    }

    /** A thin bar across the middle of the slider, as Sodium's track, but showing the colors. */
    private static IDrawable track(IDrawable bar) {
        return (context, x, y, width, height, widgetTheme) -> {
            bar.draw(context, x, y + (height - TRACK_HEIGHT) / 2, width, TRACK_HEIGHT, widgetTheme);
            GL11.glColor4f(1, 1, 1, 1);
        };
    }

    private static IDrawable gradient(IntSupplier left, IntSupplier right) {
        return track((context, x, y, width, height, widgetTheme) -> {
            int from = 0xFF000000 | left.getAsInt();
            int to = 0xFF000000 | right.getAsInt();
            GuiDraw.drawRect(x, y, width, height, from, to, from, to);
        });
    }

    private Row buttons() {
        ButtonWidget<?> cancel = new ButtonWidget<>();
        WawelAuthStyle.textButton(cancel.size(BUTTON_WIDTH, BUTTON_HEIGHT), BUTTON_WIDTH - 8, "gui.cancel")
            .onMousePressed(button -> {
                animateClose();
                return true;
            });
        ButtonWidget<?> done = new ButtonWidget<>();
        WawelAuthStyle.textButton(done.size(BUTTON_WIDTH, BUTTON_HEIGHT), BUTTON_WIDTH - 8, "gui.done")
            .onMousePressed(button -> {
                closeWith(rgb);
                return true;
            });
        Row row = new Row();
        row.widthRel(1.0f)
            .height(BUTTON_HEIGHT)
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
            .child(cancel)
            .child(done);
        return row;
    }

    private static TextWidget<?> label(String text) {
        return new TextWidget<>(IKey.str(text)).height(ROW_HEIGHT)
            .alignment(Alignment.CenterLeft)
            .color(WawelAuthStyle.TEXT_SECONDARY);
    }

    private static Widget<?> gap(int height) {
        return new Widget<>().size(1, height);
    }

    // -- Hex --

    private static String withHash(String raw) {
        String text = raw.trim();
        if (text.startsWith("0x") || text.startsWith("0X")) text = text.substring(2);
        return text.startsWith("#") ? text : "#" + text;
    }

    /** "#RRGGBB" or "#RGB"; null for anything else. */
    static Integer parseHex(String text) {
        String hex = withHash(text).substring(1);
        if (hex.length() == 3) {
            hex = new String(
                new char[] { hex.charAt(0), hex.charAt(0), hex.charAt(1), hex.charAt(1), hex.charAt(2),
                    hex.charAt(2) });
        }
        if (hex.length() != 6) return null;
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
