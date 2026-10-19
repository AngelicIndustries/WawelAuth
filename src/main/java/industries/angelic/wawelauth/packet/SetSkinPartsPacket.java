package industries.angelic.wawelauth.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import industries.angelic.wawelauth.api.SkinLayersHelper;
import industries.angelic.wawelauth.common.ServerTaskScheduler;
import industries.angelic.wawelauth.wawelcore.skin.SkinPartFlags;
import io.netty.buffer.ByteBuf;

/**
 * Client -> Server, protocol v2. The hidden non-cape parts in v2 bit positions; the cape travels in the vanilla
 * client settings packet. Only sent after the server announced v2.
 */
public class SetSkinPartsPacket implements IMessage {

    private int parts;

    public SetSkinPartsPacket() {}

    public SetSkinPartsPacket(int parts) {
        this.parts = parts;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.parts = buf.readUnsignedByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.parts & SkinPartFlags.PART_MASK);
    }

    public static class Handler implements IMessageHandler<SetSkinPartsPacket, IMessage> {

        @Override
        public IMessage onMessage(SetSkinPartsPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) return null;

            int parts = message.parts & SkinPartFlags.PART_MASK;
            ServerTaskScheduler.schedule(() -> { if (!player.isDead) SkinLayersHelper.mergeParts(player, parts); });
            return null;
        }
    }
}
