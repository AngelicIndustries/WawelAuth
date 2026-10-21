package industries.angelic.wawelauth.compat.ears;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class EarsTransformerAccessTest {

    @Test
    public void standaloneEarsOpensOnlyItsRenderer() {
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
            "com.unascribed.ears.legacy.LegacyHelper");
        Set<String> prefixes = EarsTransformerAccess
            .narrowed("com.unascribed.ears", classes, "com.unascribed.ears.LayerEars");
        for (String name : classes) {
            boolean open = name.startsWith("com.unascribed.ears.LayerEars");
            assertTrue(name, open != excluded(prefixes, name));
        }
        // Whole packages where possible.
        assertTrue(prefixes.contains("com.unascribed.ears.common."));
        assertTrue(prefixes.contains("com.unascribed.ears.asm."));
    }

    @Test
    public void foamFixOpensOnlyItsBundledRenderer() {
        List<String> classes = Arrays.asList(
            "pl.asie.foamfix.FoamFix",
            "pl.asie.foamfix.coremod.FoamFixCore",
            "pl.asie.foamfix.bugfixmod.coremod.BugfixModClassTransformer",
            "pl.asie.foamfix.repack.com.unascribed.ears.Ears",
            "pl.asie.foamfix.repack.com.unascribed.ears.LayerEars",
            "pl.asie.foamfix.repack.com.unascribed.ears.LayerEars$1",
            "pl.asie.foamfix.repack.com.unascribed.ears.common.EarsRenderer",
            "pl.asie.foamfix.repack.com.unascribed.ears.legacy.LegacyHelper",
            "pl.asie.foamfix.repack.org.example.Library");
        String open = "pl.asie.foamfix.repack.com.unascribed.ears.LayerEars";
        Set<String> prefixes = EarsTransformerAccess.narrowed("pl.asie.foamfix", classes, open);
        for (String name : classes) assertTrue(name, name.startsWith(open) != excluded(prefixes, name));
        assertFalse(prefixes.contains("pl.asie.foamfix.repack."));
        assertTrue(prefixes.contains("pl.asie.foamfix.coremod."));
    }

    /** As LaunchClassLoader matches its transformer exclusions. */
    private static boolean excluded(Set<String> prefixes, String name) {
        for (String prefix : prefixes) if (name.startsWith(prefix)) return true;
        return false;
    }
}
