package org.fentanylsolutions.wawelauth.packet;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Client -> Server, legacy (WA 1.0.1 to 1.0.6). The whole flag byte in the sender's layout.
 * <p>
 * Kept on its discriminator so older servers still understand it and older clients can still send it. New clients
 * only send it to older servers. New servers ignore it: the layout (v0 or v1) of the sender is unknown, so those
 * clients are treated like vanilla ones (all parts shown, cape from the client settings packet).
 */
public class UpdateSkinLayersPacket implements IMessage {

    private byte mask;

    public UpdateSkinLayersPacket() {}

    public UpdateSkinLayersPacket(byte mask) {
        this.mask = mask;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.mask = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.mask);
    }

    public static class Handler implements IMessageHandler<UpdateSkinLayersPacket, IMessage> {

        @Override
        public IMessage onMessage(UpdateSkinLayersPacket message, MessageContext ctx) {
            return null;
        }
    }
}
