# Changelog

What changed in each release. The APKs are in [Releases](https://github.com/Delaybrain/glasshouse/releases);
plans are in the README's [Roadmap](README.md#roadmap).

## 1.2.0 (2026-11-20)

- **An HDMI player in Glasshouse's look**, if you turn it on (Settings → HDMI inputs; off until you
  do, so an update changes nothing). OK on an HDMI pill shows the input full screen with a glass
  badge (input, resolution, refresh rate), a glass card when there's no signal (with the inputs that
  have something on, and the TV goes to standby after 10 minutes without one), and Back brings up a
  strip of all inputs. The TV still does the picture itself.
- **Names and icons for HDMI inputs:** PC, laptop, game console, TV box, Blu-ray player, receiver or
  camera, a name of your own, or the TV's name again.
- **Lighter over a live picture:** the home screen over the HDMI preview is no longer almost black at
  the bottom, and a PC's green dot is shared between the preview and the player.
- **Measured against Google TV's home screen** on the same TV: 0.4 % janky frames against 21 %, with
  less memory. See the README for how, and why the real difference is bigger.

## 1.1.1 (2026-11-11)

- **Remote buttons by their names.** Settings → Remote buttons says *Gear*, *Person button* or
  *Red button* in the TV's language, not Android's names like `NOTIFICATION` or `PROG_RED`.
- **Casting in the docs.** What keeping Chromecast built-in (`mediashell`) costs, measured on
  the tested TV, and that without it the TV doesn't show up when you cast from a phone.
- **Changelog, roadmap and issue forms** for reporting a TV it runs on, or a bug.

## 1.1.0 (2026-11-06)

- **Remote buttons set on the TV.** Settings → Remote buttons: pick a job (Home, All apps, Control
  Center, Live TV, Account, or Off) and press the button that should do it. A card beside the list
  shows how. Until something is changed the standard buttons stay, so a remote that works keeps
  working; *Reset to standard* asks first.
- **Photo position** for the wallpaper and the screensaver: top, middle, bottom, or the whole photo
  on a soft copy of itself. Screensaver photos of another shape were stretched before.
- **Delete photos**, one or all, each after one question that shows the photo.
- **Home closes every menu at once**; Back still goes one step.
- Menus' glass follows the wallpaper while you try wallpapers.
- An HDMI input's green dot goes out 10 minutes after its picture was last seen.
- On MediaTek TVs with Google's voice search off, the voice button's pairing page closes itself.

## 1.0.0 (2026-11-06)

The first release: the home screen with its glass dock and shelf, HDMI inputs with a live preview,
Control Center, tile looks, status bar, recently used apps, kids lock, photos and backups over
Wi-Fi from a phone, screensaver, all labels in 18 languages, and the install, check and undo
scripts.
