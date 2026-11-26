package industries.angelic.wawelauth.client.capecreator;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/** Writes a looping animated GIF, frame by frame, so only one full-size frame is in memory at a time. */
public final class GifWriter {

    private static final String FORMAT = "javax_imageio_gif_image_1.0";

    private GifWriter() {}

    interface Frames {

        int count();

        BufferedImage frame(int index);

        int delayMs(int index);
    }

    public interface Progress {

        void frameWritten(int done, int total);
    }

    static byte[] write(Frames frames, Progress progress) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("gif");
        if (!writers.hasNext()) {
            throw new IOException("This Java has no GIF writer.");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(out);
            writer.prepareWriteSequence(null);
            int total = frames.count();
            for (int i = 0; i < total; i++) {
                BufferedImage frame = frames.frame(i);
                BufferedImage indexed = PaletteQuantizer.quantize(frame);
                ImageWriteParam param = writer.getDefaultWriteParam();
                IIOMetadata metadata = writer
                    .getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(indexed), param);
                describeFrame(
                    metadata,
                    (IndexColorModel) indexed.getColorModel(),
                    frames.delayMs(i),
                    PaletteQuantizer.hasTransparency(frame),
                    i == 0);
                writer.writeToSequence(new IIOImage(indexed, null, metadata), param);
                if (progress != null) progress.frameWritten(i + 1, total);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    /**
     * Each frame brings its own palette and replaces the one before; the first one also makes the GIF loop forever.
     * Without its own palette, the writer would use a default one for every frame.
     */
    private static void describeFrame(IIOMetadata metadata, IndexColorModel palette, int delayMs, boolean transparent,
        boolean first) throws IOException {
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(FORMAT);
        IIOMetadataNode table = child(root, "LocalColorTable");
        while (table.getLength() > 0) {
            table.removeChild(table.getFirstChild());
        }
        table.setAttribute("sizeOfLocalColorTable", "256");
        table.setAttribute("sortFlag", "FALSE");
        for (int i = 0; i < 256; i++) {
            IIOMetadataNode entry = new IIOMetadataNode("ColorTableEntry");
            entry.setAttribute("index", String.valueOf(i));
            entry.setAttribute("red", String.valueOf(i < palette.getMapSize() ? palette.getRed(i) : 0));
            entry.setAttribute("green", String.valueOf(i < palette.getMapSize() ? palette.getGreen(i) : 0));
            entry.setAttribute("blue", String.valueOf(i < palette.getMapSize() ? palette.getBlue(i) : 0));
            table.appendChild(entry);
        }
        IIOMetadataNode control = child(root, "GraphicControlExtension");
        control.setAttribute("disposalMethod", "restoreToBackgroundColor");
        control.setAttribute("userInputFlag", "FALSE");
        control.setAttribute("transparentColorFlag", transparent ? "TRUE" : "FALSE");
        control.setAttribute("transparentColorIndex", String.valueOf(PaletteQuantizer.TRANSPARENT_INDEX));
        // GIF delays are in hundredths of a second; under 2, browsers and most players wait 100 ms instead.
        control.setAttribute("delayTime", String.valueOf(Math.max(2, Math.round(delayMs / 10f))));
        if (first) {
            IIOMetadataNode extensions = child(root, "ApplicationExtensions");
            IIOMetadataNode loop = new IIOMetadataNode("ApplicationExtension");
            loop.setAttribute("applicationID", "NETSCAPE");
            loop.setAttribute("authenticationCode", "2.0");
            loop.setUserObject(new byte[] { 1, 0, 0 });
            extensions.appendChild(loop);
        }
        metadata.setFromTree(FORMAT, root);
    }

    private static IIOMetadataNode child(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i)
                .getNodeName()
                .equalsIgnoreCase(name)) {
                return (IIOMetadataNode) root.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(name);
        root.appendChild(node);
        return node;
    }
}
