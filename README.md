# Wawel Auth

![logo](images/logo_combined.png)

Authentication mod for *Minecraft* 1.7.10.

**Server**: Allow players from *Microsoft*, *Ely.by*, *LittleSkin*, and more, including the option to host your own integrated auth server.

**Client**: Login into and manage accounts for various auth providers and servers. Every player's skin and cape is loaded from their own provider, HD skins included.

This mod also adds some other features, like an admin web panel, and animated capes (only for *Wawel Auth* accounts). For a complete list of features, and usage instructions, consult [the wiki](https://github.com/AngelicIndustries/WawelAuth/wiki).

*Wawel Auth* can be installed on the server, client, or both, but the last combination is recommended for an optimal experience. By default, the client saves configuration to a shared folder, no need to login again to all your accounts when switching *Minecraft* instances. This can be disabled.

> [!NOTE]
> *Wawel Auth* no longer handles modern skin rendering, such as slim arms, outer skin layers on the body and limbs, or 3D skin layers. For a better experience, use it alongside [SimpleSkinBackport](https://github.com/GTNewHorizons/SimpleSkinBackport), [Ears](https://modrinth.com/mod/ears), or [Salamander](https://github.com/AngelicIndustries/Salamander).

![screen1](images/screenshots/screen1.png)

[![hub](images/badges/github.png)](https://github.com/AngelicIndustries/WawelAuth/releases)
[![curse](images/badges/curse.png)](https://www.curseforge.com/minecraft/mc-mods/wawel-auth)
[![modrinth](images/badges/modrinth.png)](https://modrinth.com/mod/wawel-auth)
[![67](images/badges/67.png)](https://67minecraft.com/mod/wawel-auth)
[![maven](images/badges/maven.png)](https://maven.angelic.industries/#/releases/industries/angelic/wawelauth/WawelAuth)
![forge](images/badges/forge.png)
[![cord](images/badges/cord.png)](https://discord.gg/xAWCqGrguG)
[![mcmodcn](images/badges/mcmodcn.png)](https://www.mcmod.cn/class/27750.html#comment-2159044)

## Dependencies

* [UniMixins](https://modrinth.com/mod/unimixins) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/unimixins) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/unimixins/versions) [![git](images/icons/git.png)](https://github.com/LegacyModdingMC/UniMixins/releases)
* [AngelLib](https://www.curseforge.com/minecraft/mc-mods/angellib) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/angellib) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/angellib) [![67](images/icons/67.png)](https://67minecraft.com/mod/angellib) [![git](images/icons/git.png)](https://github.com/AngelicIndustries/AngelLib)
* [ModularUI2](https://github.com/GTNewHorizons/ModularUI2) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/modularui)  [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/modularui) [![git](images/icons/git.png)](https://github.com/GTNewHorizons/ModularUI2) (Client only)

> [!NOTE]
> Jar variants:
> * Regular and `dev`: bundle `sqlite-jdbc` and Bouncy Castle, so they work offline and in downstream development environments.
> * `slim` and `slim-dev`: download those at startup, with only your platform's SQLite natives (Android included); libraries already present are reused.
> * `android`: a slim jar with Android's SQLite natives built in. Not published to CurseForge.
> * `curseforge`: cannot import launcher sessions (CurseForge disallows this functionality).

## Building

```bash
./gradlew build
```

## Credits

* [Catalogue-Vintage](https://github.com/RuiXuqi/Catalogue-Vintage) for folder icon and system-open inspiration
* [GT:NH buildscript](https://github.com/GTNewHorizons/ExampleMod1.7.10)
* [Background image](https://www.pinterest.com/pin/367536019569661725/)

## License

`LGPLv3`

## Buy me some creatine

* [ko-fi.com](https://ko-fi.com/jackisasubtlejoke)
* Monero: `893tQ56jWt7czBsqAGPq8J5BDnYVCg2tvKpvwTcMY1LS79iDabopdxoUzNLEZtRTH4ewAcKLJ4DM4V41fvrJGHgeKArxwmJ`

<br>

![license](images/license_small.png)
