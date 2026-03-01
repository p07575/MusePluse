# MusePluse

![Release](https://img.shields.io/badge/release-v2.0.7-blue)
![Minecraft](https://img.shields.io/badge/Minecraft-Up%20to%201.21.11-green)

> **Fork Notice:** This is a maintained fork of the original MusePluse plugin. The original project is no longer maintained. This fork restores YouTube downloading via yt-dlp and adds support for modern Minecraft versions.

MusePluse is an extremely powerful Minecraft music player. Capable of taking YouTube links and automatically converting
them into resource packs & sending them to the players on your server. And when you think that was it, it also
comes with a full fleshed out GUI player for all your players.

MusePluse also comes with an extremely powerful developer API, every feature of the plugin is exposed publicly
meaning any methods used for developing the plugin are directly available to you!

## What's Changed in This Fork

- **YouTube downloading restored** — Replaced the broken `java-youtube-downloader` library with [yt-dlp](https://github.com/yt-dlp/yt-dlp), which is actively maintained
- **Modern Minecraft support** — Resource pack format updated to work with 1.19.4 through 1.21.11
- **Bug fixes** — Fixed resource pack namespace issues, crash on unavailable songs, and various compile errors

## Requirements

- Java 16+
- Spigot/Paper 1.19.4+
- **Recommended:** Install [deno](https://deno.land) or Node.js on your server for best yt-dlp compatibility with YouTube

## Installing & Setting up MusePluse

1. Drop the JAR into your server's `plugins/` folder
2. Start the server — FFmpeg and yt-dlp will be auto-downloaded on first run
3. Configure your songs in `plugins/MusePluse/songs.yml`
4. Restart or run `/reloadmusic` to generate the resource pack

***WARNING ABOUT CLIENT-SIDE STORAGE SPACE FOR ADMINISTRATORS OR SERVERS THAT CONSISTENTLY ADD SONGS***

Due to current bugs within recent minecraft code we are unable to use hashes for updating the resource pack. This means in order to force an
update when you regenerate your resource pack we just send a file with a different UUID, meaning minecraft on your client may leave behind a bunch of unused server-side
resource packs located in `%appdata%\.minecraft\server-resource-packs`. This is a bug within minecraft itself see here: https://bugs.mojang.com/browse/MC-164316
