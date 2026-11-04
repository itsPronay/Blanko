<img src="images/applogo.png" alt="Blanko" width="128" height="128" align="left"/>

# Blanko

**Listen to different sounds.** 

<br>

[![License](https://img.shields.io/github/license/itsPronay/napify.svg)](https://github.com/itsPronay/napify/blob/main/LICENSE)
[![Desktop](https://img.shields.io/badge/Windows%20%7C%20macOS%20%7C%20Linux-Desktop-0078D6?logo=linux&logoColor=white)](#installation)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/multiplatform.html)

> This branch (`kmp-impl`) is the **desktop-only** Kotlin Multiplatform version of Blanko for Windows, macOS and Linux. The Android app belongs to a [separate repository](https://github.com/itsPronay/blankee)`. 

This app was inspired by [Blanket](https://github.com/rafaelmardojai/blanket), originally a GNOME application. You can think of this project as an all-in-one desktop version of Blanket. Supports macOS, Windows, Linux (few versions)

## Description

Improve focus and increase your productivity by listening to different sounds. Blanko can also be used to help you to fall asleep in a noisy environment.

## Features

- **Multiple Ambient Sounds** – Rain, waves, fireplace, wind, storm, birds, and more
- **Sleep Timer** – Set a timer, and the playing sounds will stop automatically
- **Custom Sounds** – Upload your own audio files
- **Save Presets** – Create and save your favorite combinations for quick access
- **System Tray** – Keep sounds playing while the window is hidden (where the OS has a tray)

## Installation

Download the installer for your system from the [GitHub Releases](https://github.com/itsPronay/blankee/releases) page:

| System | Installer |
|---|---|
| Windows | `.exe` or `.msi` |
| Linux | `.deb` (Debian, Ubuntu) or `.rpm` (Fedora, openSUSE) |
| macOS | `.dmg` |

### Build from Source

Blanko is a Kotlin Multiplatform project with a Compose Multiplatform UI.

| Module | What it is |
|---|---|
| `shared` | Kotlin Multiplatform code: Compose UI, view models, use cases, Room database, DataStore preferences, Koin DI and the string/drawable resources. |
| `desktopApp` | The desktop app: window, system tray, the bundled sounds and the installers (`.exe`, `.msi`, `.deb`, `.rpm`, `.dmg`). |
| `desktop-audio` | Pure-JVM audio engine: M4A/AAC, MP3, WAV and OGG decoders and a gapless looping mixer. |

#### Requirements

- JDK 17+
- Each installer must be built on its own OS: Windows (`.exe`/`.msi`, needs the [WiX Toolset 3](https://wixtoolset.org)), Linux (`.deb`/`.rpm`, needs `rpmbuild` and `fakeroot`) or macOS (`.dmg`).

#### Build Instructions

```bash
# Clone the repository
git clone https://github.com/itsPronay/blanko.git
cd blanko

# Run the app
./gradlew :desktopApp:run

# Build an installer for the OS you are on
./gradlew :desktopApp:packageExe :desktopApp:packageMsi   # Windows
./gradlew :desktopApp:packageDeb :desktopApp:packageRpm   # Linux
./gradlew :desktopApp:packageDmg                          # macOS (unsigned)
```

Installers are written to `desktopApp/build/compose/binaries/main/`. Settings, the database and imported custom sounds live in `%APPDATA%\Blanko` on Windows, `~/.local/share/blanko` on Linux and `~/Library/Application Support/Blanko` on macOS. The *Desktop Build* GitHub workflow builds the installers on each OS and uploads every one as a plain (unzipped) artifact. Closing the window keeps the sounds playing from the system tray (where the OS has one); use *Quit* in the tray menu to exit.

The app plays its bundled sounds from Ogg Vorbis files in `desktopApp/src/main/resources/sounds`, trimmed to their exact loop length. Custom sounds can be MP3, M4A/AAC, WAV or OGG.

## Translations

Blanko is translated into 20 languages. If your language is not added, please feel free to open an issue or contribute.

## Contributing

We welcome open-source contributions! You can:

- 🐛 Report bugs
- 💡 Suggest features
- 🔧 Submit pull requests
- 🌍 Help with translations
- ⭐ Star this repository

## Credits

Sounds are sourced from others. For detailed information about sounds licensing, [see this file](SOUNDS_LICENSING.md).

<p align="center">
  Inspired by Rafael Mardojai's <a href="https://github.com/rafaelmardojai/blanket">Blanket</a>
  <br>
  Made with ❤️ by Pronay
</p>
