package industries.angelic.wawelauth.mixins.early.minecraft;

import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.wawelauth.client.gui.WorldRowIcons;
import industries.angelic.wawelauth.wawelclient.IGuiSelectWorldExt;

/** Draws each world's account face and castle icon with the row, so the list clips them as it scrolls. */
@Mixin(targets = "net.minecraft.client.gui.GuiSelectWorld$List")
public abstract class MixinGuiSelectWorldList {

    /** The list's world screen: the synthetic {@code this$0} at runtime, {@code field_148207_k} in some setups. */
    @Shadow(aliases = { "field_148207_k" }, remap = false)
    @Final
    GuiSelectWorld this$0;

    @Inject(method = "drawSlot", at = @At("RETURN"))
    private void wawelauth$drawWorldAccount(int slotIndex, int x, int y, int slotHeight, Tessellator tessellator,
        int mouseX, int mouseY, CallbackInfo ci) {
        WorldRowIcons.drawRow(
            ((IGuiSelectWorldExt) this$0).wawelauth$getWorld(slotIndex),
            x,
            y,
            mouseX,
            mouseY,
            ((GuiSlot) (Object) this).func_148141_e(mouseY)); // GuiSlot.isMouseYWithinSlotBounds
    }
}
