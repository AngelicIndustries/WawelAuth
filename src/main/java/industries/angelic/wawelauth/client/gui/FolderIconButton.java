package industries.angelic.wawelauth.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FolderIconButton extends GuiButton {

    private static final ResourceLocation ICON_TEXTURE = new ResourceLocation("wawelauth", "textures/gui/icons.png");
    private static final int ICON_U = 0;
    private static final int ICON_V = 0;
    private static final int ICON_SIZE = 10;

    public FolderIconButton(int id, int x, int y) {
        super(id, x, y, 20, 20, "");
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
        super.drawButton(minecraft, mouseX, mouseY);
        if (!this.visible) {
            return;
        }

        minecraft.getTextureManager()
            .bindTexture(ICON_TEXTURE);
        float brightness = this.enabled ? 1.0F : 0.5F;
        GL11.glColor4f(brightness, brightness, brightness, 1.0F);

        int iconX = this.xPosition + (this.width - ICON_SIZE) / 2;
        int iconY = this.yPosition + (this.height - ICON_SIZE) / 2;
        Gui.func_146110_a(iconX, iconY, ICON_U, ICON_V, ICON_SIZE, ICON_SIZE, 64.0f, 64.0f); // drawModalRectWithCustomSizedTexture

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return this.visible && mouseX >= this.xPosition
            && mouseX < this.xPosition + this.width
            && mouseY >= this.yPosition
            && mouseY < this.yPosition + this.height;
    }
}
