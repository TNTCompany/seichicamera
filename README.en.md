# SeichiCamera

> Map-to-camera companion for anime pilgrimage photography.

[简体中文](README.md) · [English](README.en.md) · [日本語](README.ja.md) · [繁體中文](README.zh-TW.md)

SeichiCamera connects anime discovery, pilgrimage maps, reference imagery, and CameraX capture in one Android workflow. Find a location, bring a clear reference image into the camera, align the composition, and save a side-by-side comparison.

## Highlights

- Search anime titles and explore associated locations on a map.
- Open a location directly in the camera with its reference image.
- Move, scale, rotate, mirror, and adjust the transparency of the image overlay.
- Remove the current overlay to return to a clean camera preview, then load another image from the map or gallery.
- Generate comparison images and keep local cache and preferences.

## Stack

Kotlin · Jetpack Compose · Material 3 · CameraX · Hilt · Room · Retrofit · Coil · osmdroid / OpenStreetMap

## Getting started

Requires JDK 17, Android SDK `compileSdk` 35, and Android 7.0 / API 24 or newer.

```bash
git clone <your-fork-or-repository-url>
cd SeichiCamera
./gradlew assembleDebug
```

Create a local `local.properties` file with `sdk.dir=/path/to/Android/Sdk` before building.

## License

Released under the [MIT License](LICENSE). This project is not officially affiliated with its data providers or the rights holders of referenced works and images.

For complete documentation, see the [Simplified Chinese README](README.md).
