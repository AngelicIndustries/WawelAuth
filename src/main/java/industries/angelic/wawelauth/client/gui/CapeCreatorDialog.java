package industries.angelic.wawelauth.client.gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ColorPickerDialog;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.cleanroommc.modularui.widgets.layout.Row;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.angellib.util.FileUtil;
import industries.angelic.angellib.util.GuiText;
import industries.angelic.wawelauth.WawelAuth;
import industries.angelic.wawelauth.client.capecreator.CapeCreator;
import industries.angelic.wawelauth.client.capecreator.CapeExport;
import industries.angelic.wawelauth.client.capecreator.CapePreviewTexture;
import industries.angelic.wawelauth.client.capecreator.CapeSource;
import industries.angelic.wawelauth.wawelclient.CapeUploadInfo;
import industries.angelic.wawelauth.wawelclient.WawelClient;
import industries.angelic.wawelauth.wawelclient.data.ClientAccount;
import industries.angelic.wawelauth.wawelcore.data.TextureType;

/**
 * Makes an animated cape from a GIF, like minecraftcapes.net's creator: the picture on the cape's back and, if wanted,
 * on the elytra, the rest a color or a background cape, at 64x32 up to 2048x1024. The players beside it wear the
 * result. It saves the cape to a file, sets it on an offline account, or uploads it to a Wawel Auth server that takes
 * it.
 */
@SideOnly(Side.CLIENT)
final class CapeCreatorDialog {

    /** What the account manager lends the creator: its preview players, and refreshing after a new cape. */
    interface Host {

        ClientAccount account();

        PlayerPreviewEntity previewEntity(ResourceLocation cape);

        Widget<?> previewPanel(PlayerPreviewEntity front, PlayerPreviewEntity back, Runnable tick, int height,
            int entityWidth, int entityHeight);

        void capeChanged(long accountId, String result);
    }

    private static final int WIDTH = 416;
    private static final int HEIGHT = 212;
    private static final int PADDING = 9;
    private static final int CONTROLS_WIDTH = 210;
    private static final int PREVIEW_GAP = 8;
    private static final int BODY_HEIGHT = 132;
    /** Larger than the upload dialog's players, filling the preview's width. */
    private static final int PREVIEW_ENTITY_WIDTH = 84;
    private static final int PREVIEW_ENTITY_HEIGHT = 100;
    private static final int SLIDER_COLOR = 0xFFFFFFFF;
    private static final int ROW_HEIGHT = 16;
    private static final int ROW_GAP = 7;
    private static final int LABEL_WIDTH = 62;
    private static final int SIZE_LABEL_WIDTH = 50;
    private static final int SECONDARY_TEXT = 0xFFA8A8A8;
    private static final int BOX_SIZE = 9;
    private static final String[] IMAGE_EXTENSIONS = { "gif", "png", "jpg", "jpeg" };

    private final Host host;
    private final CapeCreator creator = new CapeCreator();
    private final CapePreviewTexture previewTexture = new CapePreviewTexture();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "WawelAuth-CapeCreator");
        thread.setDaemon(true);
        return thread;
    });
    private final Dialog<Boolean> dialog = new Dialog<>("wawelauth_cape_creator");

    private PlayerPreviewEntity front;
    private PlayerPreviewEntity back;
    private CapeUploadInfo uploadInfo;
    private String status = "";
    private boolean busy;
    private boolean previewRunning;
    private boolean previewDirty;
    private boolean closed;
    private File lastDirectory;
    /** For saving; uploads use APNG where the server takes it, GIF otherwise. */
    private CapeExport.Format saveFormat = CapeExport.Format.APNG;

    private CapeCreatorDialog(Host host) {
        this.host = host;
    }

    static Dialog<Boolean> build(Host host) {
        return new CapeCreatorDialog(host).build();
    }

    private Dialog<Boolean> build() {
        dialog.setCloseOnOutOfBoundsClick(false);
        dialog.onCloseAction(() -> {
            closed = true;
            worker.shutdownNow();
            previewTexture.delete();
        });

        front = host.previewEntity(null);
        back = host.previewEntity(null);
        requestPreview();
        probeUpload();

        IPanelHandler colorHandler = IPanelHandler
            .simple(dialog, (parent, player) -> hsvColorPicker(creator.getColor(), this::colorPicked), true);

        Column controls = new Column();
        controls.width(CONTROLS_WIDTH)
            .heightRel(1.0f)
            .child(
                fileRow(
                    "wawelauth.gui.cape_creator.image",
                    "wawelauth.gui.cape_creator.choose",
                    this::chooseImage,
                    () -> creator.hasImage() ? GuiText
                        .tr("wawelauth.gui.cape_creator.image_info", creator.getImageName(), creator.getFrameCount())
                        : GuiText.tr("wawelauth.gui.cape_creator.no_image"),
                    false))
            .child(gap(ROW_GAP))
            .child(
                fileRow(
                    "wawelauth.gui.cape_creator.background",
                    "wawelauth.gui.cape_creator.choose",
                    this::chooseBackground,
                    () -> creator.getBackgroundName() != null ? creator.getBackgroundName()
                        : GuiText.tr("wawelauth.gui.cape_creator.no_background"),
                    true))
            .child(gap(ROW_GAP))
            .child(colorRow(colorHandler))
            .child(gap(ROW_GAP))
            .child(scaleRow())
            .child(gap(ROW_GAP))
            .child(elytraCheckbox())
            .child(gap(ROW_GAP))
            .child(formatRow());

        Row body = new Row();
        body.widthRel(1.0f)
            .height(BODY_HEIGHT)
            .child(controls)
            .child(new Widget<>().size(PREVIEW_GAP, 1))
            .child(
                host.previewPanel(
                    front,
                    back,
                    this::tickPreview,
                    BODY_HEIGHT,
                    PREVIEW_ENTITY_WIDTH,
                    PREVIEW_ENTITY_HEIGHT)
                    .width(WIDTH - PADDING * 2 - CONTROLS_WIDTH - PREVIEW_GAP));

        int textWidth = WIDTH - PADDING * 2 - 4;
        ButtonWidget<?> close = new ButtonWidget<>();
        WawelAuthStyle.textButton(close.size(60, 18), 52, "wawelauth.gui.common.close")
            .onMousePressed(button -> {
                dialog.closeIfOpen();
                return true;
            });
        ButtonWidget<?> save = new ButtonWidget<>();
        WawelAuthStyle.textButton(save.size(84, 18), 76, "wawelauth.gui.cape_creator.save", this::canExport)
            .onMousePressed(button -> {
                if (canExport()) save();
                return true;
            });
        ButtonWidget<?> upload = new ButtonWidget<>();
        upload.size(84, 18)
            .setEnabledIf(widget -> uploadInfo != null && uploadInfo.isAllowed());
        upload.background(WawelAuthStyle.underlined(WawelAuthStyle.BUTTON_IDLE))
            .hoverBackground(
                WawelAuthStyle.underlined(() -> canExport() ? WawelAuthStyle.BUTTON_HOVER : WawelAuthStyle.BUTTON_IDLE))
            .overlay(
                IKey.dynamic(
                    () -> GuiText.ellipsizeToPixelWidth(
                        GuiText.tr(
                            uploadInfo != null && uploadInfo.isOffline() ? "wawelauth.gui.cape_creator.set"
                                : "wawelauth.gui.cape_creator.upload"),
                        76))
                    .color(
                        () -> canExport()
                            ? upload.isBelowMouse() ? WawelAuthStyle.TEXT_PRIMARY : WawelAuthStyle.TEXT_BUTTON_IDLE
                            : WawelAuthStyle.TEXT_SECONDARY))
            .onMousePressed(button -> {
                if (canExport()) upload();
                return true;
            });

        Column root = new Column();
        root.widthRel(1.0f)
            .heightRel(1.0f)
            .padding(PADDING)
            .background(IDrawable.EMPTY)
            .disableHoverBackground()
            .child(
                new TextWidget<>(GuiText.key("wawelauth.gui.cape_creator.title")).widthRel(1.0f)
                    .height(14)
                    .color(WawelAuthStyle.THEME_LIGHTER))
            .child(gap(4))
            .child(body)
            .child(gap(6))
            .child(
                new TextWidget<>(IKey.dynamic(() -> GuiText.ellipsizeToPixelWidth(statusLine(), textWidth)))
                    .tooltipDynamic(tooltip -> {
                        String line = statusLine();
                        if (!line.isEmpty()) tooltip.addLine(IKey.str(line));
                    })
                    .tooltipAutoUpdate(true)
                    .widthRel(1.0f)
                    .height(10)
                    .scale(0.8f)
                    .color(SECONDARY_TEXT))
            .child(
                new Widget<>().widthRel(1.0f)
                    .expanded())
            .child(
                new Row().widthRel(1.0f)
                    .height(18)
                    .mainAxisAlignment(Alignment.MainAxis.END)
                    .child(save)
                    .child(new Widget<>().size(6, 18))
                    .child(upload)
                    .child(new Widget<>().size(6, 18))
                    .child(close));

        WawelAuthStyle.dialog(dialog);
        dialog.size(WIDTH, HEIGHT)
            .child(root);
        return dialog;
    }

    // -- Rows --

    private Row fileRow(String labelKey, String buttonKey, Runnable choose,
        java.util.function.Supplier<String> description, boolean clearable) {
        ButtonWidget<?> pick = new ButtonWidget<>();
        WawelAuthStyle.textButton(pick.size(52, ROW_HEIGHT), 46, buttonKey, () -> !busy)
            .onMousePressed(button -> {
                if (!busy) choose.run();
                return true;
            });
        int descriptionWidth = CONTROLS_WIDTH - LABEL_WIDTH - 52 - 4 - (clearable ? ROW_HEIGHT + 2 : 0);
        Row row = new Row();
        row.widthRel(1.0f)
            .height(ROW_HEIGHT)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(label(labelKey))
            .child(pick)
            .child(new Widget<>().size(4, ROW_HEIGHT))
            .child(
                new TextWidget<>(
                    IKey.dynamic(
                        () -> GuiText.ellipsizeToPixelWidth(description.get(), (int) (descriptionWidth / 0.8f))))
                            .tooltipDynamic(tooltip -> tooltip.addLine(IKey.str(description.get())))
                            .tooltipAutoUpdate(true)
                            .width(descriptionWidth)
                            .height(ROW_HEIGHT)
                            .scale(0.8f)
                            .color(SECONDARY_TEXT));
        if (clearable) {
            ButtonWidget<?> clear = new ButtonWidget<>();
            WawelAuthStyle.button(clear.size(ROW_HEIGHT, ROW_HEIGHT))
                .overlay(
                    IKey.str("x")
                        .color(WawelAuthStyle.TEXT_PRIMARY))
                .addTooltipLine(GuiText.tr("wawelauth.gui.cape_creator.clear_background"))
                .setEnabledIf(widget -> creator.getBackgroundName() != null)
                .onMousePressed(button -> {
                    creator.clearBackground();
                    requestPreview();
                    return true;
                });
            row.child(new Widget<>().size(2, ROW_HEIGHT))
                .child(clear);
        }
        return row;
    }

    /** The swatch is the button: it opens the color picker. */
    private Row colorRow(IPanelHandler colorHandler) {
        ButtonWidget<?> swatch = new ButtonWidget<>();
        swatch.size(30, 14)
            .background(swatch(false))
            .hoverBackground(swatch(true))
            .onMousePressed(button -> {
                colorHandler.deleteCachedPanel();
                colorHandler.openPanel();
                return true;
            });
        Row row = new Row();
        row.widthRel(1.0f)
            .height(ROW_HEIGHT)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(label("wawelauth.gui.cape_creator.color"))
            .child(swatch)
            .child(new Widget<>().size(6, ROW_HEIGHT))
            .child(
                new TextWidget<>(IKey.dynamic(() -> String.format(Locale.ROOT, "#%06X", creator.getColor()))).width(60)
                    .height(ROW_HEIGHT)
                    .color(SECONDARY_TEXT));
        return row;
    }

    /**
     * ModularUI's color picker, on its HSV page: the hue and saturation sliders are what picking a cape color is
     * mostly about.
     */
    private static ColorPickerDialog hsvColorPicker(int rgb, java.util.function.IntConsumer picked) {
        ColorPickerDialog picker = new ColorPickerDialog(
            "wawelauth_cape_color",
            color -> { if (color != null) picked.accept(color); },
            0xFF000000 | rgb,
            false);
        PagedWidget<?> pages = findPages(picker);
        if (pages != null) {
            pages.initialPage(1);
        }
        return picker;
    }

    private static PagedWidget<?> findPages(IWidget widget) {
        if (widget instanceof PagedWidget<?>paged) return paged;
        if (widget.getChildren() == null) return null;
        for (IWidget child : widget.getChildren()) {
            PagedWidget<?> found = findPages(child);
            if (found != null) return found;
        }
        return null;
    }

    private Row scaleRow() {
        Row row = new Row();
        row.widthRel(1.0f)
            .height(ROW_HEIGHT)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(label("wawelauth.gui.cape_creator.scale"))
            .child(scaleSlider().width(CONTROLS_WIDTH - LABEL_WIDTH - SIZE_LABEL_WIDTH - 6))
            .child(new Widget<>().size(6, ROW_HEIGHT))
            .child(
                new TextWidget<>(IKey.dynamic(() -> CapeCreator.sizeLabel(creator.getScale()))).width(SIZE_LABEL_WIDTH)
                    .height(ROW_HEIGHT)
                    .scale(0.8f)
                    .color(SECONDARY_TEXT));
        return row;
    }

    /** Drawn as Sodium draws its sliders: a thin track and a narrow knob of the full height, both white. */
    private SliderWidget scaleSlider() {
        IDrawable track = (context, x, y, width, height, widgetTheme) -> {
            Gui.drawRect(x, y + height / 2, x + width, y + height / 2 + 1, SLIDER_COLOR);
            GL11.glColor4f(1, 1, 1, 1);
        };
        IDrawable knob = (context, x, y, width, height, widgetTheme) -> {
            Gui.drawRect(x, y, x + width, y + height, SLIDER_COLOR);
            GL11.glColor4f(1, 1, 1, 1);
        };
        SliderWidget slider = new SliderWidget();
        slider.bounds(CapeCreator.MIN_SCALE, CapeCreator.MAX_SCALE)
            .stopper(1, 2, 3, 4, 5, 6)
            .stopperTexture(IDrawable.EMPTY)
            .sliderTexture(knob)
            .sliderSize(4, 12)
            .background(track)
            .hoverBackground(track)
            .value(new DoubleValue.Dynamic(creator::getScale, value -> {
                int scale = (int) Math.round(value);
                if (scale != creator.getScale()) {
                    creator.setScale(scale);
                    requestPreview();
                }
            }))
            .height(12);
        return slider;
    }

    /** The format animated capes are saved in: APNG or GIF. */
    private Row formatRow() {
        Row row = new Row();
        row.widthRel(1.0f)
            .height(ROW_HEIGHT)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(label("wawelauth.gui.cape_creator.format"))
            .child(formatButton(CapeExport.Format.APNG, "APNG", "wawelauth.gui.cape_creator.format_apng"))
            .child(new Widget<>().size(4, ROW_HEIGHT))
            .child(formatButton(CapeExport.Format.GIF, "GIF", "wawelauth.gui.cape_creator.format_gif"));
        return row;
    }

    private ButtonWidget<?> formatButton(CapeExport.Format format, String name, String tooltipKey) {
        ButtonWidget<?> button = new ButtonWidget<>();
        WawelAuthStyle.button(button.size(40, ROW_HEIGHT), () -> saveFormat == format)
            .overlay(
                IKey.str(name)
                    .color(() -> saveFormat == format ? WawelAuthStyle.THEME_LIGHTER : WawelAuthStyle.TEXT_SECONDARY))
            .addTooltipLine(GuiText.tr(tooltipKey))
            .onMousePressed(mouseButton -> {
                saveFormat = format;
                return true;
            });
        return button;
    }

    private ButtonWidget<?> elytraCheckbox() {
        ButtonWidget<?> box = new ButtonWidget<>();
        box.widthRel(1.0f)
            .height(ROW_HEIGHT)
            .background(IDrawable.EMPTY)
            .hoverBackground(IDrawable.EMPTY)
            .overlay(checkbox(GuiText.tr("wawelauth.gui.cape_creator.elytra"), creator::isOnElytra))
            .onMousePressed(button -> {
                creator.setOnElytra(!creator.isOnElytra());
                requestPreview();
                return true;
            });
        return box;
    }

    private static TextWidget<?> label(String key) {
        return new TextWidget<>(GuiText.key(key)).width(LABEL_WIDTH)
            .height(ROW_HEIGHT)
            .color(SECONDARY_TEXT);
    }

    private static Widget<?> gap(int height) {
        return new Widget<>().size(1, height);
    }

    private IDrawable swatch(boolean hovered) {
        return (context, x, y, width, height, widgetTheme) -> {
            Gui.drawRect(x, y, x + width, y + height, hovered ? 0xFFFFFFFF : 0xFF000000);
            Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF000000 | creator.getColor());
            GL11.glColor4f(1, 1, 1, 1);
        };
    }

    private static IDrawable checkbox(String label, java.util.function.BooleanSupplier ticked) {
        return (context, x, y, width, height, widgetTheme) -> {
            int boxY = y + (height - BOX_SIZE) / 2;
            int border = WawelAuthStyle.TEXT_PRIMARY;
            Gui.drawRect(x, boxY, x + BOX_SIZE, boxY + BOX_SIZE, border);
            Gui.drawRect(x + 1, boxY + 1, x + BOX_SIZE - 1, boxY + BOX_SIZE - 1, 0xFF1A1A1A);
            if (ticked.getAsBoolean()) {
                Gui.drawRect(x + 2, boxY + 2, x + BOX_SIZE - 2, boxY + BOX_SIZE - 2, border);
            }
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            int textColor = ticked.getAsBoolean() ? WawelAuthStyle.THEME_LIGHTER : WawelAuthStyle.TEXT_SECONDARY;
            font.drawString(label, x + BOX_SIZE + 5, y + (height - font.FONT_HEIGHT) / 2 + 1, textColor);
            GL11.glColor4f(1, 1, 1, 1);
        };
    }

    // -- Preview --

    private void tickPreview() {
        previewTexture.tick();
        ResourceLocation location = previewTexture.getLocation();
        if (location != null) {
            front.setForcedCape(location);
            back.setForcedCape(location);
        }
    }

    /** Makes the preview again; while one is being made, the next waits for it, so dragging stays smooth. */
    private void requestPreview() {
        if (previewRunning) {
            previewDirty = true;
            return;
        }
        previewRunning = true;
        CapeCreator.Snapshot snapshot = creator.snapshot();
        runInBackground(() -> {
            CapeCreator.PreviewFrames frames = snapshot.preview();
            onClientThread(() -> {
                previewRunning = false;
                if (closed) return;
                previewTexture.show(frames);
                tickPreview();
                if (previewDirty) {
                    previewDirty = false;
                    requestPreview();
                }
            });
        }, () -> previewRunning = false);
    }

    private void colorPicked(int rgb) {
        if ((rgb & 0xFFFFFF) == creator.getColor()) return;
        creator.setColor(rgb);
        requestPreview();
    }

    // -- Files --

    private void chooseImage() {
        pick("wawelauth.gui.cape_creator.pick_image", IMAGE_EXTENSIONS, file -> {
            CapeSource source = CapeCreator.loadImage(file);
            onClientThread(() -> {
                creator.setImage(source, file.getName());
                status = source.animated()
                    ? GuiText.tr("wawelauth.gui.cape_creator.loaded_animated", source.frameCount())
                    : GuiText.tr("wawelauth.gui.cape_creator.loaded_still");
                requestPreview();
            });
        });
    }

    private void chooseBackground() {
        pick("wawelauth.gui.cape_creator.pick_background", new String[] { "png" }, file -> {
            java.awt.image.BufferedImage background = CapeCreator.loadBackground(file);
            onClientThread(() -> {
                creator.setBackground(background, file.getName());
                requestPreview();
            });
        });
    }

    private interface FileTask {

        void run(File file) throws IOException;
    }

    private void pick(String titleKey, String[] extensions, FileTask then) {
        String title = GuiText.tr(titleKey);
        File directory = lastDirectory != null ? lastDirectory : FileUtil.getDefaultFileSelectionDirectory();
        Thread thread = new Thread(() -> {
            FileUtil.FilePickerResult result = FileUtil.pickFile(title, directory, extensions);
            if (result.getStatus() != FileUtil.FilePickerResult.Status.SELECTED || result.getFile() == null) {
                reportPicker(result);
                return;
            }
            File file = result.getFile();
            lastDirectory = file.getParentFile();
            try {
                then.run(file);
            } catch (IOException | RuntimeException e) {
                WawelAuth.debug("Cape creator couldn't read " + file + ": " + e);
                onClientThread(
                    () -> status = GuiText.tr("wawelauth.gui.cape_creator.load_failed", file.getName(), message(e)));
            }
        }, "WawelAuth-CapeCreatorPicker");
        thread.setDaemon(true);
        thread.start();
    }

    private void reportPicker(FileUtil.FilePickerResult result) {
        if (result.getStatus() == FileUtil.FilePickerResult.Status.UNAVAILABLE
            || result.getStatus() == FileUtil.FilePickerResult.Status.ERROR) {
            String message = result.getMessage();
            onClientThread(() -> status = message != null ? message : "");
        }
    }

    private void save() {
        CapeCreator.Snapshot snapshot = creator.snapshot();
        String base = baseName(creator.getImageName());
        CapeExport.Format format = saveFormat;
        String extension = creator.isAnimated() && format == CapeExport.Format.GIF ? "gif" : "png";
        String title = GuiText.tr("wawelauth.gui.cape_creator.save_title");
        File directory = lastDirectory != null ? lastDirectory : FileUtil.getDefaultFileSelectionDirectory();
        busy = true;
        Thread thread = new Thread(() -> {
            FileUtil.FilePickerResult result = FileUtil
                .pickSaveFile(title, directory, base + "-cape." + extension, extension);
            if (result.getStatus() != FileUtil.FilePickerResult.Status.SELECTED || result.getFile() == null) {
                onClientThread(() -> busy = false);
                reportPicker(result);
                return;
            }
            File file = result.getFile();
            lastDirectory = file.getParentFile();
            try {
                CapeExport export = snapshot.export(this::reportProgress, format);
                FileUtil.writeFileBytes(file, export.getData());
                onClientThread(() -> {
                    busy = false;
                    status = GuiText.tr("wawelauth.gui.cape_creator.saved", file.getAbsolutePath(), size(export));
                });
            } catch (IOException | RuntimeException e) {
                WawelAuth.LOG.warn("Cape creator couldn't save {}: {}", file, e.toString());
                onClientThread(() -> {
                    busy = false;
                    status = GuiText.tr("wawelauth.gui.cape_creator.save_failed", message(e));
                });
            }
        }, "WawelAuth-CapeCreatorSave");
        thread.setDaemon(true);
        thread.start();
    }

    // -- Upload --

    private void probeUpload() {
        ClientAccount account = host.account();
        WawelClient client = WawelClient.instance();
        if (account == null || client == null) return;
        client.getAccountManager()
            .probeCapeUpload(account.getId())
            .thenAccept(info -> onClientThread(() -> uploadInfo = info));
    }

    private boolean canExport() {
        return creator.hasImage() && !busy;
    }

    /** What keeps the cape from this server, or null when it fits. Checked again after the file is made. */
    private String limitProblem(CapeUploadInfo info, int width, int height, int frames, boolean animated, int bytes) {
        if (info.isOffline()) return null;
        if (width > info.getMaxCapeWidth() || height > info.getMaxCapeHeight()) {
            return GuiText.tr("wawelauth.gui.cape_creator.too_big", info.getMaxCapeWidth(), info.getMaxCapeHeight());
        }
        if (animated && !info.allowsAnimatedCapes()) {
            return GuiText.tr("wawelauth.gui.cape_creator.no_animated");
        }
        if (animated && frames > info.getMaxFrames()) {
            return GuiText.tr("wawelauth.gui.cape_creator.too_many_frames", info.getMaxFrames());
        }
        int maxBytes = animated ? info.getMaxAnimatedBytes() : info.getMaxFileSizeBytes();
        if (bytes > maxBytes) {
            return GuiText.tr("wawelauth.gui.cape_creator.file_too_big", megabytes(bytes), megabytes(maxBytes));
        }
        return null;
    }

    /** The last thing that happened, or else why this account gets no Upload button. */
    private String statusLine() {
        if (status != null && !status.isEmpty()) return status;
        CapeUploadInfo info = uploadInfo;
        return info != null && !info.isAllowed() && info.getReason() != null ? GuiText.tr(info.getReason()) : "";
    }

    private void upload() {
        CapeUploadInfo info = uploadInfo;
        ClientAccount account = host.account();
        WawelClient client = WawelClient.instance();
        if (info == null || !info.isAllowed() || account == null || client == null) return;
        String problem = limitProblem(
            info,
            CapeCreator.width(creator.getScale()),
            CapeCreator.height(creator.getScale()),
            creator.getFrameCount(),
            creator.isAnimated(),
            0);
        if (problem != null) {
            status = problem;
            return;
        }
        CapeCreator.Snapshot snapshot = creator.snapshot();
        long accountId = account.getId();
        UUID profileId = account.getProfileUuid();
        busy = true;
        runInBackground(() -> {
            CapeExport export = snapshot
                .export(this::reportProgress, info.takesApng() ? CapeExport.Format.APNG : CapeExport.Format.GIF);
            String afterExport = limitProblem(
                info,
                export.getWidth(),
                export.getHeight(),
                export.getFrameCount(),
                export.isAnimated(),
                export.getData().length);
            if (afterExport != null) {
                onClientThread(() -> {
                    busy = false;
                    status = afterExport;
                });
                return;
            }
            File file = capeFile(client, info, profileId, export.extension());
            Files.write(file.toPath(), export.getData());
            onClientThread(() -> status = GuiText.tr("wawelauth.gui.cape_creator.uploading"));
            client.getAccountManager()
                .uploadTexture(accountId, TextureType.CAPE, file, false)
                .whenComplete((result, error) -> {
                    if (!info.isOffline()) {
                        file.delete();
                    }
                    onClientThread(() -> {
                        busy = false;
                        if (error != null) {
                            Throwable cause = error.getCause() != null ? error.getCause() : error;
                            status = GuiText.tr("wawelauth.gui.cape_creator.upload_failed", message(cause));
                            return;
                        }
                        status = GuiText.tr(
                            info.isOffline() ? "wawelauth.gui.cape_creator.set_done"
                                : "wawelauth.gui.cape_creator.upload_done");
                        host.capeChanged(accountId, status);
                    });
                });
        }, () -> busy = false);
    }

    /**
     * Offline accounts keep pointing at their cape file, so it goes where it stays: Wawel Auth's folder, one file per
     * account. Uploads only need it until they're sent.
     */
    private static File capeFile(WawelClient client, CapeUploadInfo info, UUID profileId, String extension)
        throws IOException {
        if (!info.isOffline()) {
            return File.createTempFile("wawelauth-cape-", "." + extension);
        }
        File directory = new File(client.getDataDir(), "offline-capes");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Couldn't create " + directory);
        }
        String name = profileId != null ? profileId.toString() : "cape";
        // A cape of the other kind would be left behind, and the account points at only one.
        new File(directory, name + (extension.equals("gif") ? ".png" : ".gif")).delete();
        return new File(directory, name + "." + extension);
    }

    // -- Threads --

    private interface Work {

        void run() throws Exception;
    }

    private void runInBackground(Work work, Runnable onFailure) {
        try {
            worker.submit(() -> {
                try {
                    work.run();
                } catch (Exception | LinkageError e) {
                    WawelAuth.LOG.warn("Cape creator failed: {}", e.toString());
                    onClientThread(() -> {
                        onFailure.run();
                        status = GuiText.tr("wawelauth.gui.cape_creator.failed", message(e));
                    });
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            onFailure.run();
        }
    }

    private void reportProgress(int done, int total) {
        if (total > 1) {
            onClientThread(() -> status = GuiText.tr("wawelauth.gui.cape_creator.writing", done, total));
        }
    }

    private static void onClientThread(Runnable action) {
        Minecraft.getMinecraft()
            .func_152344_a(action); // Minecraft.addScheduledTask
    }

    private static String message(Throwable e) {
        return e.getMessage() != null ? e.getMessage()
            : e.getClass()
                .getSimpleName();
    }

    private static String baseName(String name) {
        if (name == null || name.isEmpty()) return "cape";
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String size(CapeExport export) {
        return export.getWidth() + "x" + export.getHeight() + ", " + megabytes(export.getData().length);
    }

    private static String megabytes(int bytes) {
        return String.format(Locale.ROOT, "%.2f MB", bytes / 1048576.0);
    }
}
