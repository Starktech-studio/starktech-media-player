### libxml2-android-builder

A simple shell script to cross-compile libxml2 project for Android targets.

Builds the binaries and libs using static linking.

Typical usage:
```
bash ./build.sh
```

Requirements:
- Android SDK & NDK
- Autotools (autoconf, automake, libtool)
- prebuilt zlib (via zlib-android-builder)
- some dev tools
