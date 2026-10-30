### libfreetype-android-builder

A simple shell script to cross-compile FreeType project for Android targets.

Builds the binaries and libs using static linking.

Typical usage:
```
bash ./build.sh
```

Requirements:
- Android SDK & NDK
- Autotools (autoconf, automake, libtool)
- prebuilt libpng (via libpng-android-builder)
- prebuilt zlib (via zlib-android-builder)
- some dev tools
