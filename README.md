# Q-OS by Q-CUBIC

Q-OS turns an Android phone into a desktop-style computer. Android, Windows and Linux apps all open from one place.

## Version 0.1 (prototype) includes

- **Desktop home screen**: wallpaper, desktop icons, taskbar with clock and battery
- **Start menu**: every installed app, with search
- **Pin apps**: long-press any app to pin it to the desktop and taskbar, or to see app info or uninstall it
- **Smart Files**: tap a file and Q-OS opens it in the right engine
  - `.apk` installs as an Android app
  - `.exe`, `.msi`, `.bat` open in the Windows engine
  - `.deb`, `.sh`, `.py`, `.rpm` open in the Linux engine
- **Engines screen**: shows whether the Windows engine (Winlator) and Linux engine (Termux) are installed, with download links and the one-time Ubuntu setup command
- **Settings**: Lite mode for low-RAM phones, 24-hour clock, set Q-OS as the home screen

Version 0.1 hands files to existing engine apps. Later versions will build the engines into Q-OS itself.

## Download

Each push to `main` builds the APK automatically. Open the **Releases** page and download `Q-OS.apk` from **Q-OS latest build**.

## Install on your phone

1. Download `Q-OS.apk` on your phone and open it. Allow "Install unknown apps" if Android asks.
2. Open Q-OS. To use it as your home screen, go to Settings and tap "Make Q-OS my home screen".
3. In Files, allow file access.
4. In Engines, download the Windows and Linux engines once.

## Requirements

Android 8 or newer on an ARM64 phone. The Windows engine needs 4 GB of RAM or more. It works best on Snapdragon phones.

## Build it yourself

Open the folder in Android Studio and press Run, or run `gradle assembleDebug`.
