package org.fentanylsolutions.wawelauth.mixins.early.minecraft;

import java.awt.image.BufferedImage;

import net.minecraft.client.renderer.ThreadDownloadImageData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ThreadDownloadImageData.class)
public interface AccessorThreadDownloadImageData {

    @Accessor("bufferedImage")
    BufferedImage wawelauth$getBufferedImage();
}
