# Origem do código — Nova Video Player

## 1. Base

| Item | Valor |
|---|---|
| Release | Nova Video Player v6.6.2 (27/10/2026) |
| Manifesto | `docs/operacional/nova-manifest-v6.6.2.xml` — cópia sem alteração de https://github.com/nova-video-player/aos-AVP/releases/download/v6.6.2/manifest.xml |
| Montagem | 30/10/2026, no WSL: `repo init -u https://github.com/nova-video-player/aos-AVP -b nova` + `repo sync -m ~/nova-manifest-v6.6.2.xml` em `~/nova-src` |
| Tag | `nova-v6.6.2` no primeiro commit (código do Nova sem alteração). Tudo o que a StarkTech mudou: `git diff nova-v6.6.2` |
| O que foi copiado | Só os arquivos que cada projeto versiona (`git ls-files`), mais o `Makefile` da raiz (cópia de `AVP/Makefile`, feita pelo `copyfile` do manifesto). Sem `.git` nem `.repo` |
| Fora da cópia | As 21 entradas de submódulo (modo 160000) que o `aos-AVP` ainda registra (`AVP/Video`, `AVP/MediaLib`, `AVP/native/*`…): são só ponteiros, sem arquivo |
| Bit de execução | Em `/mnt/d` o Git não enxerga `chmod`; os 104 arquivos que o Nova versiona como 100755 foram marcados com `git update-index --chmod=+x` |

## 2. Pastas, projetos e commits

`(master)` = sem `revision` no `default.xml`, vale o `default revision="master"`.

| Pasta | Projeto (github.com/nova-video-player/…) | Ramo no default.xml | Commit (release v6.6.2) |
|---|---|---|---|
| `AVP` | aos-AVP | nova | 5c5d4ec81b47ba59be8f91c9d877b49b0ab1c01e |
| `Video` | aos-Video | v6.6 | 842ac7592ea9188b60a34acd08a450b88d0c4545 |
| `MediaLib` | aos-MediaLib | v6.6 | 4aa2ef11bb8b775a7663f8fc33f86595793a1e9b |
| `FileCoreLibrary` | aos-FileCoreLibrary | v6.6 | 465e0baac0e3f40c52bcbb30ed216755e0171b9d |
| `native/avos` | aos-avos | v6.6 | 95a9e6b36a07dc996a37adc485d0ef460f1818f2 |
| `native/audiocompress` | aos-audiocompress | master | 3a3f9324d2550bd305060e1c52b803121ea09aad |
| `native/ffmpeg-android-builder` | ffmpeg-android-builder-nobin | v9_0 | e9ce2d2511e223e11a3bb41509314007d1fc5572 |
| `native/dav1d-android-builder` | dav1d-android-builder-nobin | v1_5 | 4c604fd5123909a6a28b48be35f1d0bc964dc9ab |
| `native/opus-android-builder` | opus-android-builder-nobin | 20241012 | 91e0c9d81dcb6469f2422fcac7e10b840a0e3da2 |
| `native/libmysofa-android-builder` | libmysofa-android-builder | main | dc97741d01fa3cee8c81312b24bb4414c3f85d5c |
| `native/zlib-android-builder` | zlib-android-builder | main | d0d3cbaacb4b9e5695225afcd76d1a6d3b10c1a2 |
| `native/libpng-android-builder` | libpng-android-builder | main | e4e9d60b8543d21bbef8ae1c40404c32a06db815 |
| `native/libxml2-android-builder` | libxml2-android-builder | main | 3b34564946997af8f3ecc38a8d6842ecd8a69909 |
| `native/libfreetype-android-builder` | libfreetype-android-builder | main | 60784616139012a42e943929fd1bc32ecf9629a7 |
| `native/libfribidi-android-builder` | libfribidi-android-builder | main | e4f95b0756367e9b889811502d93613bc1d679fe |
| `native/harfbuzz-android-builder` | harfbuzz-android-builder | main | 71fb17057890c01559675dec703440dce472a764 |
| `native/libunibreak-android-builder` | libunibreak-android-builder | main | 749c62001d13dc1996fb95e5963d72519b9d62e3 |
| `native/fontconfig-android-builder` | fontconfig-android-builder | main | c51ce4b686828f75cb33e11875b9d5f2b6891323 |
| `native/libass-android-builder` | libass-android-builder | main | 69919b2d3deff70d81e31902e9660f6dbc432273 |
| `native/openssl-android-builder` | openssl-android-builder-nobin | v4_1 | 8b961fa1f0103976f8b1e33a06be492de1a95349 |
| `native/libnativehelper` | aos-libnativehelper | (master) | ab46fb536b594b25f93611ad7ef0f27b8fc24edb |
| `native/boost` | aos-boost-android-builder | (master) | 43daa8621f4b74239d8fd52a545fe3344f0ff43c |
| `native/libtorrent-android-builder` | aos-libtorrent-android-builder | main | 7e2cd1ed460a2a6ca4f5d5e32dde8f37f0cdb431 |
| `native/torrentd` | torrentd-nobin | (master) | 0a3e55d0e256725a8694c7fe3c23a8a927301152 |
| `native/prebuilt/ffmpeg` | prebuilt-ffmpeg | v9_0 | 699cb5c1a51ca3327f4507b1381b367a3999841a |
| `native/prebuilt/opus` | prebuilt-opus | main | bd2b8d8867fb4c8d964300c5e981933140fb3170 |
| `native/prebuilt/dav1d` | prebuilt-dav1d | v1_5 | f89559d62f671881431146b94c4364e568a623c8 |
| `native/prebuilt/openssl` | prebuilt-openssl | v4_1 | a115df3031d7154de5048d15a0f75d7e047b4114 |
| `native/prebuilt/libmysofa` | prebuilt-libmysofa | v1_3 | ee9ba48ad4fb347c77d016662bc4744f5b677367 |
| `native/prebuilt/torrentd` | prebuilt-torrentd | main | c97c5f52717c222c1b7b0d8d9381e876a3ccbf21 |
| `native/prebuilt/zlib` | prebuilt-zlib | main | 9652902edc3fb6d4ef2530c4e164771d704cd976 |
| `native/prebuilt/libpng` | prebuilt-libpng | main | bd3a2ed5450521783451252b9fc77c25832e88d1 |
| `native/prebuilt/libxml2` | prebuilt-libxml2 | main | 2ea77c966e41d4a9c561a5f48cf94e528739f21a |
| `native/prebuilt/freetype` | prebuilt-freetype | main | ed95689f3f9d89c140941cd917166df13ec4eb16 |
| `native/prebuilt/fribidi` | prebuilt-fribidi | main | e98837fa4125def4dd2ee9f40c66cbf7646e7eb4 |
| `native/prebuilt/harfbuzz` | prebuilt-harfbuzz | main | 946eebc3130822bf6e620847120ff20eb81df274 |
| `native/prebuilt/libunibreak` | prebuilt-libunibreak | main | afcf4bc13971ef204f6995c66102a891a54f96a1 |
| `native/prebuilt/fontconfig` | prebuilt-fontconfig | main | f1650a7dcc1426c25447828108d3d8fca9f7f2a3 |
| `native/prebuilt/libass` | prebuilt-libass | main | e2a856c061bfb22569b815eb7b3c5581037e208c |

## 3. Roteiro para atualizar do Nova

No WSL, com o Git do Ubuntu (nunca o do Windows):

1. Baixar o `manifest.xml` da nova release (`https://github.com/nova-video-player/aos-AVP/releases/download/vX.Y.Z/manifest.xml`) para `~/nova-manifest-vX.Y.Z.xml`.
2. Em `~/nova-src`: `repo sync -j4 -m ~/nova-manifest-vX.Y.Z.xml`.
3. Para cada projeto cujo commit mudou: `git -C ~/nova-src/<pasta> diff <commit antigo> <commit novo> > ~/nova-<pasta>.patch` (commits antigos na tabela 2).
4. Aplicar no repositório: `git apply --directory=<pasta> ~/nova-<pasta>.patch` (com `--3way` se houver conflito com mudanças da StarkTech). Conferir links simbólicos e bit de execução (`git update-index --chmod=+x` nos scripts novos).
5. Atualizar a tabela 2, trocar o manifesto em `docs/operacional/` e revisar `docs/arquitetura/licencas.md` (nativo novo, versão ou licença diferente).
6. Build e teste pelo `docs/operacional/build.md`.
