package industries.angelic.wawelauth.client.gui;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.factory.ClientGUI;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.wawelclient.WawelClient;

@SideOnly(Side.CLIENT)
public class WawelAuthKeybind {

    private final KeyBinding keyBinding = new KeyBinding(
        "wawelauth.key.accountmanager",
        Keyboard.KEY_NONE,
        "WawelAuth");

    public KeyBinding getKeyBinding() {
        return keyBinding;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (keyBinding.isPressed() && WawelClient.instance() != null) {
            ClientGUI.open(new AccountManagerScreen());
        }
    }
}
