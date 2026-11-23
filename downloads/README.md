# Téléchargements VTLOffmp3g 1.5

84 recettes FFmpeg prêtes à l'emploi (la cheatsheet [`tools/ffmpeg.sh`](../tools/ffmpeg.sh) et le montage du quotidien), sur téléphone et sur PC.
Au premier lancement, un court guide animé en français explique comment s'en servir.

| Fichier | Pour | Taille |
|---|---|---|
| [`VTLOffmp3g-1.5-arm64-v8a.apk`](VTLOffmp3g-1.5-arm64-v8a.apk) | Téléphones Android récents (Pixel, Samsung…, Android 7 et plus) | 48 Mo |
| [`VTLOffmp3g-1.5-armeabi-v7a.apk`](VTLOffmp3g-1.5-armeabi-v7a.apk) | Anciens téléphones Android 32 bits | 48 Mo |
| [`VTLOffmp3g-web-1.5.zip`](VTLOffmp3g-web-1.5.zip) | PC (Windows, macOS, Linux), dans le navigateur | 11 Mo |

## Android

1. Téléchargez `VTLOffmp3g-1.5-arm64-v8a.apk` sur le téléphone (bouton **Download raw file** de GitHub).
2. Ouvrez-le et autorisez l'installation d'applications depuis cette source si Android le demande.
3. Si une ancienne version est installée, la 1.5 s'installe par-dessus.

## PC

1. Téléchargez `VTLOffmp3g-web-1.5.zip` et décompressez-le.
2. Dans le dossier `VTLOffmp3g-web` :
   - **Windows** : double-cliquez `Lancer-VTLOffmp3g.bat`.
   - **macOS / Linux** : lancez `./lancer.sh` (nécessite Python 3).
3. Le navigateur s'ouvre sur `http://localhost:8765/`. Laissez la fenêtre du lanceur ouverte pendant l'utilisation.

Le traitement se fait entièrement dans le navigateur (FFmpeg compilé en WebAssembly) : aucun fichier n'est envoyé sur
Internet. Les fichiers sont chargés en mémoire, donc restez sous ~1 à 2 Go par fichier. Le streaming RTMP n'est
disponible que dans l'application Android.

## Empreintes SHA-256

```
bf042ad657507a084b5786e1ac3113c42b0e6a0b5de07ebe06cbd8cc46c262e6  VTLOffmp3g-1.5-arm64-v8a.apk
b50adcafe51bffbb33b1bc91c5bd239fba1253055c695f3c58a01067ff80f22b  VTLOffmp3g-1.5-armeabi-v7a.apk
0ec2f6e473c57b3bdff8e452416f4e64650dfb7dfe03f0ca8ffa0e84df4abc90  VTLOffmp3g-web-1.5.zip
```

Les APK sont signés avec une clé de développement : ils s'installent directement mais ne sont pas destinés au
Play Store.
