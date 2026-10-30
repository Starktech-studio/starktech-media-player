# opus-android-builder

A simple shell script to cross-compile libopus project for Android targets.

Builds the binaries/libs for the following architectures: armeabi-v7a arm64-v8a x86 x86_64.

Outputs are written to `../prebuilt/opus/lib/<ABI>/` and public headers to
`../prebuilt/opus/include/opus/`. Rerunning the script installs missing headers
while retaining existing libraries. A complete prebuilt bundle can be reused
without a source checkout.

Typical usage:
```
./build.sh
```
Requirements:
- some dev tools
- android SDK installed and $ANDROID_HOME variable correctly set
