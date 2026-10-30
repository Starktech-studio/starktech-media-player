# prebuilt-fribidi

Prebuilt FriBidi libraries for Android, used to accelerate [Nova Video Player](https://github.com/nova-video-player/aos-AVP) builds.

Unicode bidirectional text processing for libass.

## Source and builder

- Upstream: [FriBidi](https://github.com/fribidi/fribidi)
- Android builder: [libfribidi-android-builder](https://github.com/nova-video-player/libfribidi-android-builder)
- Nova checkout path: `native/prebuilt/fribidi`
- Manifest branch: `main`

This repository contains build outputs. Library source and Android build instructions live in the repositories linked above.

## Contents

The bundle uses the following layout:

- `include/fribidi/` — public headers.
- `lib/<ABI>/` — libraries: `libfribidi.a`.
- `lib/<ABI>/pkgconfig/` — package metadata used by dependent builds.

Supported ABIs: `armeabi-v7a`, `arm64-v8a`, `x86`, and `x86_64`.

## Building

From the root of a configured Nova checkout, run:

```sh
make native_build_native/libfribidi-android-builder
```

AVP's Makefile builds the prerequisites and the builder writes its outputs into `native/prebuilt/fribidi`. Use a fresh build workspace to regenerate the bundle: the builder skips existing archives, so running it against an old prebuilt checkout does not necessarily rebuild them.

## Updating the bundle

Keep headers, libraries and package metadata from the same build together across all four ABIs. Record the upstream commit/tag, builder commit, Android NDK version and API level in the update commit or accompanying build metadata. Ensure package metadata works from a different checkout location and contains no producer-specific absolute paths.

The selected source version and compiler options are defined by the builder. A cached bundle's version must be verified from its build records and headers; the latest builder revision alone is not evidence of how existing binaries were produced.

## Licensing

Nova-authored repository documentation and support material are licensed under the [Apache License 2.0](LICENSE).

The bundled FriBidi code retains its upstream copyright and licence notices. Include the licence texts and notices corresponding to the exact upstream source used for the published build, including applicable dependency notices. See the [upstream repository](https://github.com/fribidi/fribidi) for those files. Nova's application licence does not replace the bundled library's licence.
