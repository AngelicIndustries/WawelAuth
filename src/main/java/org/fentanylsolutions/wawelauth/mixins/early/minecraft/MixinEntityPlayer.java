package org.fentanylsolutions.wawelauth.mixins.early.minecraft;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.fentanylsolutions.wawelauth.api.SkinLayersHelper;
import org.fentanylsolutions.wawelauth.wawelcore.skin.IPlayerCapeState;
import org.fentanylsolutions.wawelauth.wawelcore.skin.SkinPartFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Slot 16 bit 1 is vanilla's "hide cape". The wearer's own choice lives in its own bit (see SkinPartFlags), so any
 * other writer of bit 1, such as Et Futurum's elytra tick, only sets the equipment half and cannot erase it.
 */
@Mixin(value = EntityPlayer.class, priority = 999)
public abstract class MixinEntityPlayer extends EntityLivingBase implements IPlayerCapeState {

    @Unique
    private boolean wawelauth$capeHiddenByEquipment;

    public MixinEntityPlayer(World p_i1594_1_) {
        super(p_i1594_1_);
    }

    /**
     * @author WawelAuth
     * @reason The cape bit is composed from the wearer's choice and the equipment state
     */
    @Overwrite
    @SideOnly(Side.CLIENT)
    protected boolean getHideCape(int p_82241_1_) {
        if (p_82241_1_ != SkinPartFlags.CAPE_BIT) {
            return (SkinLayersHelper.readFlags((EntityPlayer) (Object) this) & (1 << p_82241_1_)) != 0;
        }
        return SkinLayersHelper.isCapeHidden((EntityPlayer) (Object) this);
    }

    /**
     * @author WawelAuth
     * @reason Writes to the cape bit only set the equipment half; the server recomposes the vanilla bit
     */
    @Overwrite
    protected void setHideCape(int p_82239_1_, boolean p_82239_2_) {
        if (p_82239_1_ != SkinPartFlags.CAPE_BIT) {
            int flags = SkinLayersHelper.readFlags((EntityPlayer) (Object) this);
            int mask = 1 << p_82239_1_;
            this.dataWatcher
                .updateObject(SkinPartFlags.DATA_WATCHER_ID, (byte) (p_82239_2_ ? flags | mask : flags & ~mask));
            return;
        }
        this.wawelauth$capeHiddenByEquipment = p_82239_2_;
        if (!this.worldObj.isRemote) {
            SkinLayersHelper.recomposeCape((EntityPlayer) (Object) this);
        }
    }

    @Override
    public boolean wawelauth$isCapeHiddenByEquipment() {
        return this.wawelauth$capeHiddenByEquipment;
    }

    @Override
    public void wawelauth$setCapeHiddenByEquipment(boolean hidden) {
        this.wawelauth$capeHiddenByEquipment = hidden;
    }
}
