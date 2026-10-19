package industries.angelic.wawelauth.client.skin;

import java.awt.image.BufferedImage;

import org.junit.Assert;
import org.junit.Test;

public class RevisionTrackerTest {

    @Test
    public void sameSourceKeepsTheValueAndRevision() {
        RevisionTracker<String, String, Long> tracker = new RevisionTracker<>(8);
        long first = tracker.get("a", "skin-1", revision -> revision);
        long again = tracker.get("a", "skin-1", revision -> revision);
        Assert.assertEquals(first, again);
    }

    @Test
    public void changedSourceGetsANewRevision() {
        RevisionTracker<String, String, Long> tracker = new RevisionTracker<>(8);
        long first = tracker.get("a", "skin-1", revision -> revision);
        long second = tracker.get("a", "skin-2", revision -> revision);
        Assert.assertTrue(second > first);
    }

    @Test
    public void revisionsAreUniqueAcrossKeys() {
        RevisionTracker<String, String, Long> tracker = new RevisionTracker<>(8);
        long a = tracker.get("a", "same", revision -> revision);
        long b = tracker.get("b", "same", revision -> revision);
        Assert.assertNotEquals(a, b);
    }

    @Test
    public void leastRecentlyUsedKeysAreDropped() {
        RevisionTracker<Integer, String, Long> tracker = new RevisionTracker<>(2);
        long first = tracker.get(1, "s", revision -> revision);
        tracker.get(2, "s", revision -> revision);
        tracker.get(1, "s", revision -> revision);
        tracker.get(3, "s", revision -> revision);
        Assert.assertEquals(2, tracker.size());
        Assert.assertEquals(first, (long) tracker.get(1, "s", revision -> revision));
        Assert.assertNotEquals(first, (long) tracker.get(2, "s", revision -> revision));
    }

    @Test
    public void imageCopiesAreIndependent() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(3, 4, 0x80FF0000);
        BufferedImage copy = SkinImages.copy(image);
        Assert.assertNotSame(image, copy);
        Assert.assertEquals(0x80FF0000, copy.getRGB(3, 4));
        copy.setRGB(3, 4, 0);
        Assert.assertEquals(0x80FF0000, image.getRGB(3, 4));
        Assert.assertNull(SkinImages.copy(null));
    }
}
