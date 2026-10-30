### libass-android-builder

A simple shell script to cross-compile libass project for Android targets.

Builds the binaries and libs using static linking.

Typical usage:
```
bash ./build.sh
```

Requirements:
- Android SDK & NDK
- Autotools (autoconf, automake, libtool)
- prebuilt Fontconfig (via fontconfig-android-builder)
- prebuilt HarfBuzz (via harfbuzz-android-builder)
- prebuilt FriBidi (via libfribidi-android-builder)
- prebuilt FreeType (via libfreetype-android-builder)
- prebuilt libunibreak (via libunibreak-android-builder)
- prebuilt libxml2 (via libxml2-android-builder)
- prebuilt libpng (via libpng-android-builder)
- prebuilt zlib (via zlib-android-builder)
- some dev tools
