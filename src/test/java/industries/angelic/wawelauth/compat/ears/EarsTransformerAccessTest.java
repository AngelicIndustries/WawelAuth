package industries.angelic.wawelauth.compat.ears;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class EarsTransformerAccessTest {

    @Test
    public void standaloneEarsOpensOnlyItsRendererAndProfileLookup() {
        List<String> classes = Arrays.asList(
            "com.unascribed.ears.Ears",
            "com.unascribed.ears.Ears$$Lambda$1",
            "com.unascribed.ears.EarsFeaturesLookup",
            "com.unascribed.ears.LayerEars",
            "com.unascribed.ears.LayerEars$1",
            "com.unascribed.ears.ModelTransBox",
            "com.unascribed.ears.asm.RenderPlayerTransformer",
            "com.unascribed.ears.common.EarsRenderer",
            "com.unascribed.ears.common.agent.OldForgeCoremod",
            "com.unascribed.ears.legacy.LegacyHelper",
            "com.unascribed.ears.legacy.LegacyHelper$1",
            "com.unascribed.ears.legacy.SessionService");
        List<String> open = Arrays.asList("com.unascribed.ears.LayerEars", "com.unascribed.ears.legacy.LegacyHelper");
        Set<String> prefixes = EarsTransformerAccess.narrowed("com.unascribed.ears", classes, open);
        for (String name : classes) assertTrue(name, opened(open, name) != excluded(prefixes, name));
        // Whole packages where possible.
        assertTrue(prefixes.contains("com.unascribed.ears.common."));
        assertTrue(prefixes.contains("com.unascribed.ears.asm."));
        assertFalse(prefixes.contains("com.unascribed.ears.legacy."));
    }

    @Test
    public void foamFixOpensOnlyItsBundledRendererAndProfileLookup() {
        List<String> classes = Arrays.asList(
            "pl.asie.foamfix.FoamFix",
            "pl.asie.foamfix.coremod.FoamFixCore",
            "pl.asie.foamfix.bugfixmod.coremod.BugfixModClassTransformer",
            "pl.asie.foamfix.repack.com.unascribed.ears.Ears",
            "pl.asie.foamfix.repack.com.unascribed.ears.LayerEars",
            "pl.asie.foamfix.repack.com.unascribed.ears.LayerEars$1",
            "pl.asie.foamfix.repack.com.unascribed.ears.common.EarsRenderer",
            "pl.asie.foamfix.repack.com.unascribed.ears.legacy.LegacyHelper",
            "pl.asie.foamfix.repack.com.unascribed.ears.legacy.ProfileService",
            "pl.asie.foamfix.repack.org.example.Library");
        List<String> open = Arrays.asList(
            "pl.asie.foamfix.repack.com.unascribed.ears.LayerEars",
            "pl.asie.foamfix.repack.com.unascribed.ears.legacy.LegacyHelper");
        Set<String> prefixes = EarsTransformerAccess.narrowed("pl.asie.foamfix", classes, open);
        for (String name : classes) assertTrue(name, opened(open, name) != excluded(prefixes, name));
        assertFalse(prefixes.contains("pl.asie.foamfix.repack."));
        assertTrue(prefixes.contains("pl.asie.foamfix.coremod."));
    }

    private static boolean opened(List<String> open, String name) {
        for (String kept : open) if (name.equals(kept) || name.startsWith(kept + "$")) return true;
        return false;
    }

    /** As LaunchClassLoader matches its transformer exclusions. */
    private static boolean excluded(Set<String> prefixes, String name) {
        for (String prefix : prefixes) if (name.startsWith(prefix)) return true;
        return false;
    }
}
