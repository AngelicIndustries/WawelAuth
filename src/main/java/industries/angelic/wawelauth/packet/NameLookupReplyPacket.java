package industries.angelic.wawelauth.packet;

import java.util.UUID;

import net.minecraft.client.Minecraft;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import industries.angelic.wawelauth.wawelclient.WawelClient;
import io.netty.buffer.ByteBuf;

/**
 * Server -> Client. The answer to a {@link NameLookupPacket}: the player behind the name, if the server knows one,
 * and the session server of the provider that player last joined with, if known.
 */
public class NameLookupReplyPacket implements IMessage {

    private String name;
    private GameProfile profile;
    private String sessionUrl;

    public NameLookupReplyPacket() {}

    public NameLookupReplyPacket(String name, GameProfile profile, String sessionUrl) {
        this.name = name;
        this.profile = profile;
        this.sessionUrl = sessionUrl;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.name = ByteBufUtils.readUTF8String(buf);
        if (buf.readBoolean()) {
            UUID id = new UUID(buf.readLong(), buf.readLong());
            this.profile = new GameProfile(id, ByteBufUtils.readUTF8String(buf));
        }
        if (buf.readBoolean()) {
            this.sessionUrl = ByteBufUtils.readUTF8String(buf);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.name == null ? "" : this.name);
        boolean found = this.profile != null && this.profile.getId() != null;
        buf.writeBoolean(found);
        if (found) {
            buf.writeLong(
                this.profile.getId()
                    .getMostSignificantBits());
            buf.writeLong(
                this.profile.getId()
                    .getLeastSignificantBits());
            ByteBufUtils.writeUTF8String(buf, this.profile.getName() == null ? this.name : this.profile.getName());
        }
        buf.writeBoolean(this.sessionUrl != null);
        if (this.sessionUrl != null) {
            ByteBufUtils.writeUTF8String(buf, this.sessionUrl);
        }
    }

    public static final class Handler implements IMessageHandler<NameLookupReplyPacket, IMessage> {

        @Override
        public IMessage onMessage(NameLookupReplyPacket message, MessageContext ctx) {
            if (!ctx.side.isClient() || message.name == null
                || message.name.length() > NameLookupPacket.MAX_NAME_LENGTH) {
                return null;
            }
            Minecraft.getMinecraft()
                .func_152344_a(() -> { // addScheduledTask
                    WawelClient client = WawelClient.instance();
                    if (client != null) {
                        client.getNameProfileResolver()
                            .onServerAnswer(message.name, message.profile, message.sessionUrl);
                    }
                });
            return null;
        }
    }
}
