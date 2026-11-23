<p align="center">
  <img src="assets/banner.png" alt="VTLOffmp3g · FFmpeg sans ligne de commande, partout où c'est possible" width="100%">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/LICENCE-GPL--3.0-2BE36A?style=for-the-badge&labelColor=555555" alt="Licence GPL-3.0">
  <img src="https://img.shields.io/badge/MOTEUR-FFMPEG-18C5D8?style=for-the-badge&labelColor=555555" alt="Moteur FFmpeg">
  <br>
  <img src="https://img.shields.io/badge/ANDROID%20%C2%B7%20WINDOWS%20%C2%B7%20MACOS%20%C2%B7%20LINUX-A855F7?style=for-the-badge" alt="Android · Windows · macOS · Linux">
</p>

# ∇TLO · VTLOffmp3g

> Faites faire à FFmpeg tout ce dont vous avez besoin, sans taper une seule commande.
> Gardez le terminal pour le jour où vous en aurez vraiment envie.

VTLOffmp3g est **la façon la plus simple d'utiliser FFmpeg** : les 24 commandes essentielles de la
[cheatsheet FFmpeg](tools/ffmpeg.sh) deviennent des **recettes prêtes à l'emploi**, sur téléphone Android comme
sur PC. Vous choisissez un fichier, vous réglez deux paramètres, vous lancez : **tout se passe sur votre
appareil, hors ligne, sans envoyer un seul octet sur Internet**. Et pour ceux qui veulent garder la main, la
commande FFmpeg reste visible et modifiable à chaque étape.

<p align="center">
  <img src="assets/apercu.png" alt="Aperçu : l'application PC dans le navigateur et l'application Android" width="100%">
</p>

## 📥 Télécharger

| Plateforme | Fichier | Taille |
|---|---|---|
| **Android** 7+ (téléphones récents, ARM 64 bits) | [`VTLOffmp3g-1.4-arm64-v8a.apk`](downloads/VTLOffmp3g-1.4-arm64-v8a.apk) | 46 Mo |
| **Android** 7+ (anciens téléphones 32 bits) | [`VTLOffmp3g-1.4-armeabi-v7a.apk`](downloads/VTLOffmp3g-1.4-armeabi-v7a.apk) | 46 Mo |
| **PC** Windows · macOS · Linux (navigateur) | [`VTLOffmp3g-web-1.4.zip`](downloads/VTLOffmp3g-web-1.4.zip) | 10 Mo |

- **Android** : ouvrez l'APK sur le téléphone et autorisez l'installation depuis cette source.
- **PC** : décompressez le zip, puis double-cliquez `Lancer-VTLOffmp3g.bat` (Windows) ou lancez `./lancer.sh`
  (macOS / Linux). Le navigateur s'ouvre sur l'application.

Instructions détaillées et empreintes SHA-256 : [`downloads/`](downloads/).

## ✨ Ce que ça fait

- **24 recettes** issues de la cheatsheet, regroupées en quatre onglets : *Convertir*, *Audio*, *Montage*, *Images*.
- **Recherche** par nom, codec ou format (`mp3`, `gif`, `webm`…).
- **Paramètres simples** (qualité CRF, débit, début et durée, taille, position du filigrane…) et
  **commande FFmpeg éditable** pour les utilisateurs avancés.
- **Suivi en direct** : jauge de progression, **télémétrie** fps et débit façon analyseur de spectre, vitesse,
  taille produite, journal complet.
- **Résultat** à enregistrer, partager ou ouvrir (Android), à prévisualiser et télécharger (PC).
- **Guide animé en français** au premier lancement.
- **Android** : travail en arrière-plan avec notification, rapport de plantage clair au lancement suivant,
  compatible Android 7 à 17 et pages mémoire 16 Ko.
- **PC** : FFmpeg compilé en WebAssembly tourne dans le navigateur ; rien à installer sous Windows.

## 🧭 Comment ça marche

1. **Choisissez une recette**, par onglet ou par recherche.
2. **Ajoutez vos fichiers** avec *Pick* (vidéo, audio, logo, sous-titres selon la recette).
3. **Réglez les paramètres**, ou modifiez directement la commande FFmpeg.
4. **Lancez** : la jauge, la télémétrie et le journal se mettent à jour en direct.
5. **Récupérez le résultat** : enregistrer, partager, ouvrir ou télécharger.

## 🎬 Les 24 recettes

| Catégorie | Recettes |
|---|---|
| **Conversion** | Tout format → MP4 (H.264 + AAC) · MP4 → WebM (VP9 + Opus) · MP4 → WebM (VP8, presets libvpx) · Remux sans réencodage |
| **Compression** | Compression CRF · Preset lent (fichier plus petit) · Encodage deux passes (débit cible) |
| **Audio** | Extraire en MP3 · Extraire en M4A (AAC) · Extraire sans réencodage · Remplacer la piste audio |
| **GIF** | Vidéo → GIF optimisé (palette en deux étapes) |
| **Découpe et assemblage** | Découpe rapide · Découpe à l'image près · Assemblage sans perte |
| **Taille, recadrage, incrustation** | Taille exacte · Largeur fixe en gardant les proportions · Recadrage · Filigrane · Sous-titres incrustés |
| **Images et diffusion** | Image fixe (miniature) · Une image par seconde · Diffusion RTMP *(Android)* |
| **Libre** | Commande FFmpeg personnalisée |

## 🗂️ Organisation

| Dossier | Contenu |
|---|---|
| [`apps/VTLOffmp3g`](apps/VTLOffmp3g) | Application Android (Java, FFmpegKit full-gpl 6.1, Android 7+) |
| [`apps/VTLOffmp3g-web`](apps/VTLOffmp3g-web) | Application PC dans le navigateur (ffmpeg.wasm, FFmpeg 5.1) ; `dist/` prêt à l'emploi |
| [`downloads/`](downloads) | APK et zip PC prêts à installer |
| [`tools/ffmpeg.sh`](tools/ffmpeg.sh) | La cheatsheet FFmpeg d'origine des recettes |
| [`assets/`](assets) | Bannière et aperçu de ce README |
| [`.github/workflows`](.github/workflows) | Construction et vérification des APK, publication web sur GitHub Pages |

Les recettes sont définies une seule fois, dans
[`Recipe.java`](apps/VTLOffmp3g/app/src/main/java/com/vtlo/offmp3g/Recipe.java) ; l'application web les exporte
de là au moment de sa construction.

## 🛠️ Construire

**Android** (JDK 17+, Android SDK, `patchelf`) :

```sh
cd apps/VTLOffmp3g
./gradlew assembleRelease        # APK dans app/build/outputs/apk/release/
./gradlew testDebugUnitTest      # tests, avec rendus d'écran dans app/build/screenshots
```

**PC** (Python 3, JDK, npm) :

```sh
cd apps/VTLOffmp3g-web
python3 build.py                 # écrit dist/
python3 dist/serve.py            # ouvre l'application sur http://localhost:8765/
```

Détails : [`apps/VTLOffmp3g/README.md`](apps/VTLOffmp3g/README.md) et
[`apps/VTLOffmp3g-web/README.md`](apps/VTLOffmp3g-web/README.md).

## 📄 Licence

VTLOffmp3g est distribué sous **GNU GPL v3** ([`LICENSE`](LICENSE)) : FFmpeg, tel qu'il est embarqué
(FFmpegKit full-gpl sur Android, ffmpeg.wasm sur PC, tous deux avec x264), est lui-même sous GPL.

- [`tools/ffmpeg.sh`](tools/ffmpeg.sh) provient d'[awesome-cheatsheets](https://github.com/LeCoupa/awesome-cheatsheets),
  sous licence MIT ([`tools/LICENSE-ffmpeg.sh.txt`](tools/LICENSE-ffmpeg.sh.txt)).
- Police DejaVu Sans (version PC) : licence Bitstream Vera.

<p align="center"><sub>ɔ copyleft · GPL-3.0 · VTLO</sub></p>
