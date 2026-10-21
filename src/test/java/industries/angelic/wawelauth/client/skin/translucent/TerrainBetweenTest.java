package industries.angelic.wawelauth.client.skin.translucent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class TerrainBetweenTest {

    @Test
    public void findsGlassOnTheWayButNotBeside() {
        Set<String> glass = new HashSet<>();
        glass.add("5,64,0");
        TerrainBetween.Blocks blocks = (x, y, z) -> glass.contains(x + "," + y + "," + z);
        // Straight through the pane.
        assertTrue(TerrainBetween.crosses(blocks, 0.5, 64.5, 0.5, 10.5, 64.5, 0.5));
        // A block over, it stays clear.
        assertFalse(TerrainBetween.crosses(blocks, 0.5, 64.5, 1.5, 10.5, 64.5, 1.5));
        // Diagonally across it, and from the far side.
        assertTrue(TerrainBetween.crosses(blocks, 0.2, 63.9, -4.7, 9.8, 64.2, 4.6));
        assertTrue(TerrainBetween.crosses(blocks, 10.5, 64.5, 0.5, 0.5, 64.5, 0.5));
    }

    @Test
    public void stopsAtTheTarget() {
        Set<String> glass = new HashSet<>();
        glass.add("12,64,0");
        TerrainBetween.Blocks blocks = (x, y, z) -> glass.contains(x + "," + y + "," + z);
        // Glass behind the player is not between.
        assertFalse(TerrainBetween.crosses(blocks, 0.5, 64.5, 0.5, 10.5, 64.5, 0.5));
        // Negative coordinates walk the right blocks too.
        glass.add("-3,64,-1");
        assertTrue(TerrainBetween.crosses(blocks, 0.5, 64.5, -0.5, -6.5, 64.5, -1.5));
    }
}
