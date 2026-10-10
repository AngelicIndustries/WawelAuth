package org.fentanylsolutions.wawelauth.wawelcore.skin;

import org.fentanylsolutions.wawelauth.api.skin.SkinPart;
import org.junit.Assert;
import org.junit.Test;

public class SkinPartFlagsTest {

    @Test
    public void encodePartsNeverTouchesCapeBits() {
        int flags = SkinPartFlags.encodeParts(part -> true);
        Assert.assertEquals(SkinPartFlags.PART_MASK, flags);
        Assert.assertEquals(0, flags & (SkinPartFlags.WEARER_CAPE_MASK | SkinPartFlags.CAPE_MASK));
    }

    @Test
    public void mergeKeepsCapeBits() {
        int flags = SkinPartFlags.WEARER_CAPE_MASK | SkinPartFlags.CAPE_MASK | 0x04;
        int merged = SkinPartFlags.mergeParts(flags, 0x80);
        Assert.assertEquals(SkinPartFlags.WEARER_CAPE_MASK | SkinPartFlags.CAPE_MASK | 0x80, merged);
    }

    @Test
    public void mergeIgnoresCapeBitsFromTheClient() {
        Assert.assertEquals(0, SkinPartFlags.mergeParts(0, SkinPartFlags.WEARER_CAPE_MASK | SkinPartFlags.CAPE_MASK));
    }

    @Test
    public void equipmentCannotEraseTheWearersChoice() {
        int flags = SkinPartFlags.composeCape(SkinPartFlags.withWearerCape(0, true), false);
        Assert.assertTrue((flags & SkinPartFlags.CAPE_MASK) != 0);

        // An elytra tick reporting "no elytra" must leave the wearer's hidden cape hidden.
        flags = SkinPartFlags.composeCape(flags, false);
        Assert.assertTrue((flags & SkinPartFlags.CAPE_MASK) != 0);

        flags = SkinPartFlags.composeCape(SkinPartFlags.withWearerCape(flags, false), false);
        Assert.assertEquals(0, flags & SkinPartFlags.CAPE_MASK);
    }

    @Test
    public void equipmentHidesAShownCape() {
        int flags = SkinPartFlags.composeCape(0, true);
        Assert.assertEquals(SkinPartFlags.CAPE_MASK, flags);
        Assert.assertEquals(0, SkinPartFlags.composeCape(flags, false));
    }

    @Test
    public void bitSevenSurvivesTheByteRoundTrip() {
        int flags = SkinPartFlags.withPart(0, SkinPart.JACKET, true);
        int roundTripped = ((byte) flags) & 0xFF;
        Assert.assertTrue(SkinPartLayout.V2.isHidden(roundTripped, SkinPart.JACKET));
        Assert.assertEquals(0, SkinPartFlags.withPart(roundTripped, SkinPart.JACKET, false));
    }
}
