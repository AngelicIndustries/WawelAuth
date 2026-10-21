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
 * Ears keeps its whole package away from class transformers, so no mixin reaches its render delegate. Before Ears loads
 * its renderer, the broad exclusion is replaced by narrower ones that cover everything else in its jar: only the
 * delegate becomes transformable, and Ears' loader and transformers stay as protected as before.
 * <p>
 * Plateau's Ears is left alone: NilLoader remaps it as the JVM defines it, after any mixin would have run.
 */
public final class EarsTransformerAccess {

    /** Standalone Ears: excludes {@code com.unascribed.ears}. */
    private static final Build STANDALONE = new Build(
        "com.unascribed.ears",
        "com.unascribed.ears.LayerEars$1",
        "standalone Ears");
    /** FoamFix's bundled Ears: FoamFix excludes all of {@code pl.asie.foamfix}. */
    private static final Build FOAMFIX = new Build(
        "pl.asie.foamfix",
        "pl.asie.foamfix.repack.com.unascribed.ears.LayerEars$1",
        "FoamFix's bundled Ears");

    private static boolean opened;

    private EarsTransformerAccess() {}

    /** Whether Ears' render delegate can be mixed into. Decided once, before mods load. */
    public static boolean opened() {
        return opened;
    }

    /** Opens whichever Ears build is installed. Call from the late mixin loader, before Ears loads its renderer. */
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
        if (cached(loader).containsKey(build.delegate)) {
            WawelAuth.LOG
                .warn("{} loaded its renderer before Wawel Auth could open it; Ears stays on its own", build.name);
            return false;
        }
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
        Set<String> narrowed = narrowed(build.excluded, classes, outer(build.delegate));
        exclusions.remove(build.excluded);
        exclusions.addAll(narrowed);
        WawelAuth.LOG.info(
            "Opened {}'s skin renderer to Wawel Auth: {} narrower transformer exclusions replace {}",
            build.name,
            narrowed.size(),
            build.excluded);
        return true;
    }

    /**
     * Prefixes covering every class under {@code excluded} except {@code open} and its inner classes: whole packages
     * where they hold none of them, single classes in the package that does.
     */
    static Set<String> narrowed(String excluded, List<String> classes, String open) {
        String openPackage = open.substring(0, open.lastIndexOf('.'));
        Set<String> prefixes = new LinkedHashSet<>();
        for (String name : classes) {
            if (name.equals(open) || name.startsWith(open + "$")) continue;
            String pkg = name.substring(0, name.lastIndexOf('.'));
            if (pkg.equals(openPackage) || openPackage.startsWith(pkg + ".")) {
                // In the delegate's own package, or a parent of it: only the class itself, unless its name would also
                // cover the delegate.
                if (!open.startsWith(name)) prefixes.add(name);
                continue;
            }
            // The highest package below the excluded root on the way to this class that doesn't hold the delegate.
            String rest = name.substring(excluded.length() + 1);
            String prefix = excluded;
            for (String segment : rest.split("\\.")) {
                prefix = prefix + "." + segment;
                if (!openPackage.equals(prefix) && !openPackage.startsWith(prefix + ".")) break;
            }
            prefixes.add(prefix.equals(name) ? name : prefix + ".");
        }
        return prefixes;
    }

    private static String outer(String className) {
        int inner = className.indexOf('$');
        return inner < 0 ? className : className.substring(0, inner);
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

        Build(String excluded, String delegate, String name) {
            this.excluded = excluded;
            this.delegate = delegate;
            this.name = name;
        }
    }
}
