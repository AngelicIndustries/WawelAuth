package industries.angelic.wawelauth.compat.ears;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import industries.angelic.wawelauth.WawelAuth;

/**
 * Ears keeps its whole package away from class transformers, so no mixin reaches its render delegate or its profile
 * lookup. Before Ears loads either, the broad exclusion is replaced by narrower ones that cover everything else in its
 * jar: only those two classes become transformable, and Ears' loader and transformers stay as protected as before.
 * <p>
 * Plateau's Ears is left alone: NilLoader remaps it as the JVM defines it, after any mixin would have run.
 */
public final class EarsTransformerAccess {

    /** Standalone Ears: excludes {@code com.unascribed.ears}. */
    private static final Build STANDALONE = new Build(
        "com.unascribed.ears",
        "com.unascribed.ears.LayerEars$1",
        "standalone Ears",
        "com.unascribed.ears.LayerEars",
        "com.unascribed.ears.legacy.LegacyHelper");
    /** FoamFix's bundled Ears: FoamFix excludes all of {@code pl.asie.foamfix}. */
    private static final Build FOAMFIX = new Build(
        "pl.asie.foamfix",
        "pl.asie.foamfix.repack.com.unascribed.ears.LayerEars$1",
        "FoamFix's bundled Ears",
        "pl.asie.foamfix.repack.com.unascribed.ears.LayerEars",
        "pl.asie.foamfix.repack.com.unascribed.ears.legacy.LegacyHelper");

    private static boolean opened;

    private EarsTransformerAccess() {}

    /** Whether Ears' render delegate can be mixed into. Decided once, before mods load. */
    public static boolean opened() {
        return opened;
    }

    /** Opens whichever Ears build is installed. Call from the late mixin loader, before Ears loads those classes. */
    public static synchronized void open() {
        LaunchClassLoader loader = Launch.classLoader;
        if (loader == null) return;
        for (Build build : new Build[] { STANDALONE, FOAMFIX }) {
            if (!classExists(loader, build.delegate)) continue;
            try {
                opened |= open(loader, build);
            } catch (ReflectiveOperationException | IOException | RuntimeException e) {
                WawelAuth.LOG.error("Could not open " + build.name + " to Wawel Auth's skin layer anchoring", e);
            }
        }
    }

    private static boolean open(LaunchClassLoader loader, Build build)
        throws ReflectiveOperationException, IOException {
        Set<String> exclusions = exclusions(loader);
        Map<String, Class<?>> cached = cached(loader);
        if (cached.containsKey(build.delegate)) {
            WawelAuth.LOG
                .warn("{} loaded its renderer before Wawel Auth could open it; Ears stays on its own", build.name);
            return false;
        }
        for (String open : build.open) if (cached.containsKey(open))
            WawelAuth.LOG.warn("{} loaded {} before Wawel Auth could open it", build.name, open);
        if (!exclusions.contains(build.excluded)) {
            // Nothing keeps it from transformers.
            return true;
        }
        URL source = loader.findResource(build.delegate.replace('.', '/') + ".class");
        if (source == null) return false;
        URLConnection connection = source.openConnection();
        if (!(connection instanceof JarURLConnection)) return false;
        // A shared, cached jar handle would close under the class loader.
        connection.setUseCaches(false);
        List<String> classes = new ArrayList<>();
        try (JarFile jar = ((JarURLConnection) connection).getJarFile()) {
            for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
                String name = entries.nextElement()
                    .getName();
                if (!name.endsWith(".class")) continue;
                String className = name.substring(0, name.length() - ".class".length())
                    .replace('/', '.');
                if (className.startsWith(build.excluded + ".")) classes.add(className);
            }
        }
        Set<String> narrowed = narrowed(build.excluded, classes, build.open);
        exclusions.remove(build.excluded);
        exclusions.addAll(narrowed);
        WawelAuth.LOG.info(
            "Opened {}'s skin renderer and profile lookup to Wawel Auth: {} narrower exclusions replace {}",
            build.name,
            narrowed.size(),
            build.excluded);
        return true;
    }

    /**
     * Prefixes covering every class under {@code excluded} except the {@code open} ones and their inner classes: whole
     * packages where they hold none of them, single classes in the packages that do and the packages above those.
     */
    static Set<String> narrowed(String excluded, List<String> classes, List<String> open) {
        Set<String> prefixes = new LinkedHashSet<>();
        for (String name : classes) {
            if (opened(name, open)) continue;
            String pkg = name.substring(0, name.lastIndexOf('.'));
            if (onPath(pkg, open)) {
                // Only the class itself, unless its name would also cover an open one.
                boolean covers = false;
                for (String kept : open) covers |= kept.startsWith(name);
                if (!covers) prefixes.add(name);
                continue;
            }
            // The highest package below the excluded root on the way to this class that holds no open class.
            String prefix = excluded;
            for (String segment : name.substring(excluded.length() + 1)
                .split("\\.")) {
                prefix = prefix + "." + segment;
                if (!onPath(prefix, open)) break;
            }
            prefixes.add(prefix + ".");
        }
        return prefixes;
    }

    private static boolean opened(String name, List<String> open) {
        for (String kept : open) if (name.equals(kept) || name.startsWith(kept + "$")) return true;
        return false;
    }

    /** Whether the package holds an open class, directly or further down. */
    private static boolean onPath(String pkg, List<String> open) {
        for (String kept : open) {
            String keptPackage = kept.substring(0, kept.lastIndexOf('.'));
            if (keptPackage.equals(pkg) || keptPackage.startsWith(pkg + ".")) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> exclusions(LaunchClassLoader loader) throws ReflectiveOperationException {
        Field field = LaunchClassLoader.class.getDeclaredField("transformerExceptions");
        field.setAccessible(true);
        return (Set<String>) field.get(loader);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Class<?>> cached(LaunchClassLoader loader) throws ReflectiveOperationException {
        Field field = LaunchClassLoader.class.getDeclaredField("cachedClasses");
        field.setAccessible(true);
        return (Map<String, Class<?>>) field.get(loader);
    }

    private static boolean classExists(LaunchClassLoader loader, String name) {
        try {
            return loader.getClassBytes(name) != null;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    private static final class Build {

        final String excluded;
        final String delegate;
        final String name;
        /** The classes to open, with their inner classes. */
        final List<String> open;

        Build(String excluded, String delegate, String name, String... open) {
            this.excluded = excluded;
            this.delegate = delegate;
            this.name = name;
            this.open = java.util.Arrays.asList(open);
        }
    }
}
