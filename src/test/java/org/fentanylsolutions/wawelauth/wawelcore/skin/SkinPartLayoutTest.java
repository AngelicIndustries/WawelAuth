package org.fentanylsolutions.wawelauth.wawelcore.skin;

import java.util.EnumSet;
import java.util.Set;

import org.fentanylsolutions.wawelauth.api.skin.SkinPart;
import org.junit.Assert;
import org.junit.Test;

public class SkinPartLayoutTest {

    @Test
    public void legacyLayoutsRoundTrip() {
        Set<SkinPart> hidden = EnumSet.of(SkinPart.CAPE, SkinPart.JACKET, SkinPart.HAT);
        for (SkinPartLayout layout : new SkinPartLayout[] { SkinPartLayout.V0, SkinPartLayout.V1 }) {
            int flags = layout.encode(hidden::contains);
            for (SkinPart part : SkinPart.VALUES) {
                Assert.assertEquals(layout + " " + part, hidden.contains(part), layout.isHidden(flags, part));
            }
        }
    }

    @Test
    public void legacyBitPositions() {
        Assert.assertEquals(0x02, SkinPartLayout.V0.encode(part -> part == SkinPart.CAPE));
        Assert.assertEquals(0x80, SkinPartLayout.V0.encode(part -> part == SkinPart.HAT));
        Assert.assertEquals(0x01, SkinPartLayout.V1.encode(part -> part == SkinPart.CAPE));
        Assert.assertEquals(0x02, SkinPartLayout.V1.encode(part -> part == SkinPart.JACKET));
    }

    @Test
    public void vanillaOnlyReadsTheCapeBit() {
        Assert.assertTrue(SkinPartLayout.VANILLA.isHidden(0x02, SkinPart.CAPE));
        Assert.assertFalse(SkinPartLayout.VANILLA.isHidden(0xFD, SkinPart.CAPE));
        for (SkinPart part : SkinPart.VALUES) {
            if (part != SkinPart.CAPE) Assert.assertFalse(SkinPartLayout.VANILLA.isHidden(0xFF, part));
        }
    }

    @Test
    public void v2CapeIsHiddenByEitherCapeBit() {
        Assert.assertFalse(SkinPartLayout.V2.isHidden(0, SkinPart.CAPE));
        Assert.assertTrue(SkinPartLayout.V2.isHidden(SkinPartFlags.WEARER_CAPE_MASK, SkinPart.CAPE));
        Assert.assertTrue(SkinPartLayout.V2.isHidden(SkinPartFlags.CAPE_MASK, SkinPart.CAPE));
    }

    @Test
    public void v2KeepsTheJacketOffTheVanillaCapeBit() {
        Assert.assertEquals(7, SkinPartLayout.V2.bit(SkinPart.JACKET));
        Assert.assertFalse(SkinPartLayout.V2.isHidden(SkinPartFlags.CAPE_MASK, SkinPart.JACKET));
        Assert.assertTrue(SkinPartLayout.V2.isHidden(0x80, SkinPart.JACKET));
    }

    @Test
    public void v1ClientsReadV2WithOnlyAJacketMismatch() {
        int flags = SkinPartFlags.encodeParts(part -> part == SkinPart.LEFT_SLEEVE || part == SkinPart.HAT);
        for (SkinPart part : SkinPart.VALUES) {
            if (part == SkinPart.CAPE || part == SkinPart.JACKET) continue;
            Assert.assertEquals(
                part.toString(),
                SkinPartLayout.V2.isHidden(flags, part),
                SkinPartLayout.V1.isHidden(flags, part));
        }
    }

    @Test
    public void legacyVersionDetection() {
        Assert.assertEquals(SkinPartLayout.V0, SkinPartLayout.forLegacyVersion("1.0.1"));
        Assert.assertEquals(SkinPartLayout.V0, SkinPartLayout.forLegacyVersion("1.0.4"));
        Assert.assertEquals(SkinPartLayout.V1, SkinPartLayout.forLegacyVersion("1.0.5"));
        Assert.assertEquals(SkinPartLayout.V1, SkinPartLayout.forLegacyVersion("1.0.6"));
        Assert.assertEquals(SkinPartLayout.V1, SkinPartLayout.forLegacyVersion("409cfd2-snapshot"));
        Assert.assertEquals(SkinPartLayout.V1, SkinPartLayout.forLegacyVersion("1.1.0"));
    }

    @Test
    public void legacyPacketOnlyForKnownReleases() {
        Assert.assertFalse(SkinPartLayout.acceptsLegacyPacket(null));
        Assert.assertFalse(SkinPartLayout.acceptsLegacyPacket("1.0.0"));
        Assert.assertFalse(SkinPartLayout.acceptsLegacyPacket("409cfd2-snapshot"));
        Assert.assertTrue(SkinPartLayout.acceptsLegacyPacket("1.0.1"));
        Assert.assertTrue(SkinPartLayout.acceptsLegacyPacket("1.0.6"));
    }
}
