<div align="center">

<img src="app/src/main/ic_launcher-playstore.png" alt="Pear" width="128" height="128">

# Pear

**A local music player for Android, styled after Apple Music.**

Synced lyrics, library browsing and a clean interface for the music already on your device.

[![Nightly Build](https://github.com/RoTatsuLabs/Pear/actions/workflows/nightly_build.yml/badge.svg?branch=nightly)](https://github.com/RoTatsuLabs/Pear/actions/workflows/nightly_build.yml)
[![License: GPL v3](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
[![Android 6.0+](https://img.shields.io/badge/Android-6.0%2B-3DDC84.svg)](#requirements)

[Features](#features) · [Installing](#installing) · [Building](#building-from-source) · [Contributing](#contributing) · [Licensing](#credits-and-licensing)

</div>

---

## Features

### Lyrics

- Reads LRC and TTML files placed next to a song, and lyrics stored in the song's own tags (MP3, M4A, FLAC and Ogg).
- Follows word timing from TTML files, including duet sides and translations, and shows songwriter credits below the last line.
- Adds a transliteration above the lyrics for Chinese, Japanese, Korean, Cyrillic, Greek and other scripts. A TTML file can supply its own.
- Lays out Arabic and Hebrew lyrics right to left.
- Fades and blurs lines by their distance from the sung one, and gives held notes a soft glow. Blur and glow need Android 12 or newer.

### Playback and library

- Browse by songs, albums, artists and playlists.
- Static full-screen album art on the playback and album pages, with a transition between the playback page and the lyrics.
- Interface translations for English, Japanese, Simplified Chinese and Traditional Chinese.

## Requirements

- Android 6.0 (API 23) or newer
- Builds for `armeabi-v7a`, `arm64-v8a`, `x86` and `x86_64`

## Installing

Nightly builds are produced on every push to the `nightly` branch. Open the latest run of the [Nightly Build](https://github.com/RoTatsuLabs/Pear/actions/workflows/nightly_build.yml) workflow and download the APK for your device's architecture from the run's artifacts. Tagged releases are published on the [Releases](https://github.com/RoTatsuLabs/Pear/releases) page.

The application ID is `rotatsu.yos.music.player`, so Pear installs next to earlier builds that used a different ID.

## Building from source

You need JDK 17 and the Android SDK (compile SDK 34).

```sh
git clone https://github.com/RoTatsuLabs/Pear.git
cd Pear
./gradlew assembleDebug
```

The APKs are written to `app/build/outputs/apk/debug/`, one per architecture.

The CI also runs:

```sh
./gradlew test       # unit tests
./gradlew lintDebug  # Android lint
```

## Contributing

- Check how the existing code does something before changing it, and keep changes small and focused.
- Write commit messages in the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) format, for example `fix(lyrics): handle an empty TTML file`.
- Add a line to the `[Unreleased]` section of [changelogs.md](changelogs.md) for any change a user would notice. The CI checks the file's format.
- Open pull requests against `nightly`.

## Credits and licensing

Pear is a fork of [FlamingoSank](https://github.com/Yos-X/FlamingoSank) and builds on [Flamingo](https://github.com/shouryadixitisverycool/Flamingo), both licensed under GPL v3. The lyric view takes its glow and animation approach from [BitChord](https://github.com/kushagrasinghx/BitChord).

Pear is released under the [GNU General Public License v3.0](LICENSE). Code taken from other projects keeps its original copyright and license notices. Fonts, icons and other assets have their own licenses and are checked separately.
