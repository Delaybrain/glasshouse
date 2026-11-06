<div align="center">

# Glasshouse

### A fast, glassy home screen for Android TV and Google TV.

Built and tuned on a 1.9 GB Google TV, where the stock home screen had become painfully slow.

**[Click through it in your browser →](https://delaybrain.github.io/glasshouse/)**

<img src="docs/shots/01-home.jpg" alt="The Glasshouse home screen" width="100%">

</div>

## What you get

- **Your apps on a glass dock.** The big picture and name at the top follow the focus. The row scrolls past six apps and wraps round. Hold OK on an app to move, replace or remove it.
- **HDMI inputs that know what's plugged in.** A dot for a device that's on (over HDMI-CEC), and a live preview behind the page while an input is focused.
- **Glass panels.** Menus, panels and Control Center float on a blurred copy of the page, with a tint that adapts so every panel is equally bright, and a rim that catches the light. All drawn once, never per frame.
- **Seven tile looks.** Original, dark, light, glass, black & white, spotlight, or light by day and dark at night. Banners on white or black get their background turned round, brand colours optionally too.
- **Status bar you choose.** Clock, network (Wi-Fi bars or a cable), date, CPU speed, RAM, download speed, off timer.
- **Remote buttons that work.** Home, the settings gear, the apps button and the TV button are caught before Google TV takes them (an accessibility service, *HomeGuard*). Hold Home for the apps you used last.
- **Control Center** on the gear button: Bluetooth, network, picture, sound, an off timer, when the screensaver and sleep start.
- **Kids lock.** A PIN for the apps you pick. It also asks when an app is opened by voice or a link.
- **Photos from your phone.** Scan a QR code on the TV, pick photos on your phone (or drop them in on a computer) for the screensaver or the wallpaper. A tiny web server runs on the TV only while that card is open.
- **Screensaver** with a big clock over your apps' colours, or your photos.
- **Back up and restore** your setup: from the menu, over Wi-Fi from a phone, or with `backup.sh` / `restore.sh`.
- **18 languages**, following the TV's.

<p>
<img src="docs/shots/04-tiles.jpg" width="49%" alt="Tile looks">
<img src="docs/shots/13-glass.jpg" width="49%" alt="Glass tiles">
<img src="docs/shots/07-control-center.jpg" width="49%" alt="Control Center">
<img src="docs/shots/09-photos-card.jpg" width="49%" alt="Photos from your phone">
</p>

## Fast on a slow TV

Measured on a PEAQ 65GQU (MediaTek, 1.9 GB RAM, Android 14), 150 presses along the app row:

| | |
|---|---|
| Janky frames | 0.85 % |
| 99th percentile frame | 38 ms |
| Memory (PSS) | about 70 MB |
| CPU while idle | about 1 % (the status bar's two-second update) |

How: the wallpaper and glass are drawn into bitmaps once, there are no elevation shadows or live blurs, banners and their washes are cached, and status text has fixed sizes so updates never lay out the page.

## Install

You need a computer with `adb` ([platform tools](https://developer.android.com/tools/releases/platform-tools)) on the same network as the TV.

1. **Turn on developer mode on the TV.** Settings → System → About → press *Android TV OS build* 7 times. Then turn on **USB debugging** (and **network debugging** if your TV has it) in Developer options.
2. **Install:**
   ```sh
   sh install.sh <TV_IP>
   ```
   It installs Glasshouse, makes it the home screen, turns on HomeGuard and the screensaver, and grants the extras below. To pick the apps: `sh install.sh <TV_IP> "com.netflix.ninja,com.google.android.youtube.tv"`.
3. **Turn debugging off** in Developer options when you're done. While it's on, other devices on your network can ask to control the TV.

To build it yourself: `sh build.sh` (or `build.ps1` on Windows) with Java 17 and the Android SDK (platform and build-tools 34). `setup-tools.ps1` fetches both on Windows.

### What install.sh grants, and why

Each one is optional: without it, only that feature stays off.

| Grant | For |
|---|---|
| `appops GET_USAGE_STATS` | "last opened", Home going back to the app you left, Hold Home |
| `pm grant WRITE_SECURE_SETTINGS` | the sleep row in Control Center; HomeGuard putting itself back after updates; keeping the waits you pick |
| `appops WRITE_SETTINGS` | the screensaver row in Control Center |
| `pm grant BLUETOOTH_CONNECT` | Control Center's Bluetooth row |
| HomeGuard (accessibility) | the remote's Home, gear, apps and TV buttons |
| `READ_MEDIA_IMAGES` | photos from a USB stick (asked on the TV when you pick that) |

### Undo

```sh
sh undo.sh <TV_IP>
```
turns back on whatever was turned off, removes Glasshouse and brings your TV's own home screen back. `restore-stock.sh` and `check.sh` work from a list of changes, for those who also cleaned up system apps.

## Cleaning up the TV

Glasshouse started as part of making a slow TV fast again. [docs/debloat.md](docs/debloat.md) explains how to turn off what you don't need, safely and reversibly, with `candidates.sh` listing every app with a verdict.

> [!WARNING]
> **At your own risk.** Turning off system apps can leave a TV stuck at the logo or restarting. Turn apps off, never uninstall them, keep the undo list, and leave Google TV's own home screen on (`install.sh` does). On OLED TVs keep the screensaver on.

## Good to know

- **Tested on** Android 14 (Google TV, MediaTek). Older TVs may work; the photo features need Android 10 or newer.
- **What apps can't do:** "Continue watching" rows from Netflix and others, CPU use in percent, and temperatures are closed to apps on Android TV. Glasshouse shows what it can (CPU speed, RAM).
- **Privacy:** nothing leaves your home network. The photo web server runs only while its card is open and takes only pictures or a backup.

## Credits

Glasshouse began as a fork of **[tv-detox](https://github.com/Ansub/tv-detox)** by **Ansub Khan** (MIT): its install, undo and check scripts, the debloat guide and the first home screen are the base this grew from. The cleanup that started it all used **[android-tv-debloat-toolkit](https://github.com/seun-novodev/android-tv-debloat-toolkit)** by **seun-novodev** (MIT). Thank you both.

App names and logos in the screenshots belong to their owners. Glasshouse is not affiliated with Google, Apple or any app shown.

## License

MIT, see [LICENSE](LICENSE). The original tv-detox copyright stays in it.
