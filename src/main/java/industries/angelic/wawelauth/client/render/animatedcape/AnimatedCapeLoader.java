package industries.angelic.wawelauth.client.render.animatedcape;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.angellib.util.ApngUtil;
import industries.angelic.wawelauth.WawelAuth;

/**
 * Animates downloaded capes that are APNGs. Providers such as Drasl serve them as plain PNGs without saying they're
 * animated, so the game would show only the first frame; once the download is in, its texture is replaced by the
 * animation.
 */
@SideOnly(Side.CLIENT)
public final class AnimatedCapeLoader {

    private AnimatedCapeLoader() {}

    /** After a cape's bytes arrive, on the downloading thread. */
    public static void animateIfApng(ResourceLocation location, byte[] bytes) {
        if (location == null || !ApngUtil.isAnimated(bytes)) return;
        AnimatedCapeTexture.DecodedGif decoded = AnimatedCapeTexture.decodeApng(bytes);
        if (decoded == null) return;
        Minecraft.getMinecraft()
            .func_152344_a(() -> { // Minecraft.addScheduledTask
                AnimatedCapeTexture texture = AnimatedCapeTexture.createFromDecoded(decoded, location);
                if (texture != null) AnimatedCapeTracker.registerShared(location, texture);
            });
    }

    /**
     * After a cape comes from the disk cache, which the client thread reads: checks and decodes on a worker. The check
     * reads only the file's start, where an APNG's animation chunk is, so still capes cost little.
     */
    public static void animateIfApngAsync(ResourceLocation location, File file) {
        if (location == null || file == null) return;
        WORKER.submit(() -> {
            try {
                if (!ApngUtil.isAnimated(head(file))) return;
                animateIfApng(location, Files.readAllBytes(file.toPath()));
            } catch (Exception e) {
                WawelAuth.debug("Couldn't check cached cape " + file + " for animation: " + e.getMessage());
            }
        });
    }

    private static final int HEAD_BYTES = 64 * 1024;
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "WawelAuth-AnimatedCapeLoader");
        thread.setDaemon(true);
        return thread;
    });

    private static byte[] head(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[HEAD_BYTES];
            int read = 0;
            while (read < buffer.length) {
                int n = in.read(buffer, read, buffer.length - read);
                if (n < 0) break;
                read += n;
            }
            return Arrays.copyOf(buffer, read);
        }
    }
}
