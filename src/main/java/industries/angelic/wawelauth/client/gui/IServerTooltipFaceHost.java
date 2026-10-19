package industries.angelic.wawelauth.client.gui;

import java.util.UUID;

public interface IServerTooltipFaceHost {

    void wawelauth$setServerTooltipFace(String displayName, UUID profileUuid, String providerName);

    void wawelauth$clearServerTooltipFace();
}
