package industries.angelic.wawelauth.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Wawel Auth's logo as a button, at the top right of the server and world lists; it opens the account manager. */
@SideOnly(Side.CLIENT)
public class LogoButton extends GuiButton {

    public static final int SIZE = 20;
    private static final ResourceLocation LOGO = new ResourceLocation("wawelauth", "Logo_Dragon_Outline.png");
    private static final int OUTLINE_COLOR = 0xFFFFFFFF;

    public LogoButton(int id, int x, int y) {
        super(id, x, y, SIZE, SIZE, "");
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition // GuiButton.hovered
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager()
            .bindTexture(LOGO);
        // The 64 pixel logo shrinks to the button smoothly instead of dropping pixels.
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        Gui.func_146110_a(
            /* drawModalRectWithCustomSizedTexture */this.xPosition,
            this.yPosition,
            0,
            0,
            SIZE,
            SIZE,
            SIZE,
            SIZE);

        if (this.field_146123_n) {
            int left = this.xPosition - 1;
            int top = this.yPosition - 1;
            int right = this.xPosition + SIZE + 1;
            int bottom = this.yPosition + SIZE + 1;
            Gui.drawRect(left, top, right, top + 1, OUTLINE_COLOR);
            Gui.drawRect(left, bottom - 1, right, bottom, OUTLINE_COLOR);
            Gui.drawRect(left, top + 1, left + 1, bottom - 1, OUTLINE_COLOR);
            Gui.drawRect(right - 1, top + 1, right, bottom - 1, OUTLINE_COLOR);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public boolean isHovered() {
        return this.field_146123_n;
    }
}
