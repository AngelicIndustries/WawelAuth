package industries.angelic.wawelauth.wawelclient;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Settings for taking over the account the game was started with. Not part of builds without launcher import.
 */
@Config(modid = "wawelauth", category = "launcherimport", configSubDirectory = "wawelauth")
public class LauncherImportConfig {

    @Config.Comment({ "Add the account the launcher started the game with to Wawel Auth without asking.",
        "For modpacks: players then join servers that accept that account without opening Wawel Auth." })
    @Config.DefaultBoolean(false)
    public static boolean importWithoutAsking = false;
}
