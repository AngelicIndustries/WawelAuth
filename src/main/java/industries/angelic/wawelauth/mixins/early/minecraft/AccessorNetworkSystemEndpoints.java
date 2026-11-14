package industries.angelic.wawelauth.mixins.early.minecraft;

import java.util.List;

import net.minecraft.network.NetworkSystem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NetworkSystem.class)
public interface AccessorNetworkSystemEndpoints {

    /** The server's listening channels, as ChannelFutures: the LAN port, relays' ports, and the host's own. */
    @Accessor("endpoints")
    List<?> wawelauth$getEndpoints();
}
