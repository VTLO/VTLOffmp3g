# VTLOffmp3g

Android app that runs every command of [`tools/ffmpeg.sh`](../../tools/ffmpeg.sh) on the phone,
plus the everyday editing jobs of [ffmpeg-skill](https://github.com/kajisho5/ffmpeg-skill) (84 recipes
in all), powered by an FFmpegKit *full-gpl* build (libx264, libvpx, libopus, libmp3lame, libass…).

| Cheatsheet section    | In the app                                                                 |
|-----------------------|----------------------------------------------------------------------------|
| Tab     | Section               | In the app                                                                 |
|---------|-----------------------|----------------------------------------------------------------------------|
| Convert | Transcode             | Anything → MP4, MP4 → WebM, Remux                                          |
|         | Compression & quality | CRF, slow preset, two-pass (both passes run in sequence)                   |
|         | Delivery              | YouTube, Reels/TikTok/Shorts, ProRes master, proxy, repair, photo → clip (Ken Burns), colour clip, 2×2 grid, side by side, tags |
| Audio   | Audio                 | Extract MP3 / M4A / stream copy, replace audio track                       |
|         | Audio clean-up & mix  | Loudness (LUFS), voice clean-up, music ducking, 5.1 → stereo, wider stereo, mute, compressor + limiter, remove silences, audiogram, waveform video / image |
| Edit    | Cut & join            | Fast cut, frame-accurate cut, concat, remove a section, transitions (xfade), B-roll cutaway |
|         | Speed & time          | Speed with pitch kept, smooth slow motion, freeze last frame, reverse, loop, black lead-in, fades |
|         | Scale, crop, overlay  | Exact scale, scale by width, crop, watermark, burned-in subtitles          |
|         | GIF                   | Palette generation + palette use (two steps, one tap)                      |
| Effects | Picture & colour      | Vertical 9:16 (crop / bars / blurred background), rotate, mirror, straighten, 360° → flat, deinterlace, denoise, stabilise, sharpen, colour correction, 3D LUT, HDR → SDR, pixelate / blur a region, remove a logo |
|         | Text & overlays       | Title, lower third, meme text, picture-in-picture, green screen, switchable subtitles |
| Inspect | Frames & streaming    | Poster frame, frame dump (saved to a folder), push to RTMP                 |
|         | Analysis              | Media info, black bars, scene changes, loudness (EBU R128), silences, SSIM / PSNR — results in the log |
|         | Custom                | Any ffmpeg command you type                                                |

Each screen lets you pick the input files, edit the parameters (CRF, bitrate, times, sizes…),
and edit the final command itself. `<input>`, `<output>`, `<audio>`, `<logo>`… are replaced by
your files when the job runs; text recipes write their subtitle file (`<title>`…) from your
parameters. Results can be saved anywhere (Storage Access Framework), shared,
or opened. Long encodes keep running in the background with a progress notification.

## Build

Requirements: JDK 17+, Android SDK (platform 35, NDK 27.2.12479018, CMake — Gradle installs
missing SDK packages itself) and [patchelf](https://github.com/NixOS/patchelf) ≥ 0.17 on the
`PATH` (`pip install patchelf`).

Why patchelf: the FFmpegKit build's `libavdevice.so` uses OpenSL ES and SDL's HIDAPI backend
without declaring the libraries that provide them, so Android refuses to load it
(`cannot locate symbol "PLATFORM_hid_write"`) and the app would crash on launch. The build adds
the missing dependencies: the system `libOpenSLES.so` and a small stub library
(`app/src/main/cpp`) for the HIDAPI functions, which ffmpeg never calls.

```sh
cd apps/VTLOffmp3g
echo "sdk.dir=$ANDROID_HOME" > local.properties   # if ANDROID_HOME isn't picked up
./gradlew assembleRelease
```

APKs land in `app/build/outputs/apk/release/` — one per ABI. Most phones need
`app-arm64-v8a-release.apk`; older 32-bit phones need `app-armeabi-v7a-release.apk`.

Checks (also run in CI):

```sh
./gradlew testDebugUnitTest                  # every screen starts (Robolectric)
./gradlew connectedDebugAndroidTest          # on a device/emulator: runs every recipe for real
python3 tools/check_native_libs.py "$ANDROID_HOME/ndk/27.2.12479018" 24 app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
python3 tools/check_alignment.py app/build/outputs/apk/*/*.apk
```

The release build is signed with the local debug key so it installs directly. Use your own
`signingConfig` before publishing to a store. The **VTLOffmp3g APK** GitHub Actions workflow
builds the same APKs and uploads them as an artifact.

FFmpeg is GPL-licensed in this build (x264), so the app as a whole falls under the GPL.
