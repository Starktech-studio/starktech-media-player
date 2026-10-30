#!/bin/bash

source ../../AVP/android-setup-light.sh

LOCAL_PATH=$($READLINK -f .)
mkdir -p ../prebuilt/libass
PREBUILT_DIR=$($READLINK -f ../prebuilt/libass)

# Check if prebuilt libraries already exist
if [ -f "${PREBUILT_DIR}/lib/armeabi-v7a/libass.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/arm64-v8a/libass.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/x86/libass.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/x86_64/libass.a" ]; then
  echo "All libass prebuilt libs already exist, skipping"
  exit 0
fi

if [ ! -d "libass" ]
then
  git clone https://github.com/libass/libass.git --depth=1 -b 0.17.5
fi

API_LEVEL=21

for ABI in armeabi-v7a arm64-v8a x86 x86_64
do
  case "${ABI}" in
    'arm64-v8a')
      TARGET=aarch64-linux-android
      ;;
    'armeabi-v7a')
      TARGET=armv7a-linux-androideabi
      ;;
    'x86')
      TARGET=i686-linux-android
      ;;
    'x86_64')
      TARGET=x86_64-linux-android
      ;;
  esac

  PREFIX="${PREBUILT_DIR}"

  OS=$(uname -s | tr '[:upper:]' '[:lower:]')
  TOOLCHAIN="${NDK_PATH}/toolchains/llvm/prebuilt/${OS}-x86_64"

  export AR="${TOOLCHAIN}/bin/llvm-ar"
  export AS="${TOOLCHAIN}/bin/llvm-as"
  export RANLIB="${TOOLCHAIN}/bin/llvm-ranlib"
  export STRIP="${TOOLCHAIN}/bin/llvm-strip"
  export CC="${TOOLCHAIN}/bin/${TARGET}${API_LEVEL}-clang"
  export CXX="${TOOLCHAIN}/bin/${TARGET}${API_LEVEL}-clang++"

  # Locate prebuilt dependencies
  FONTCONFIG_PREBUILT=$($READLINK -f ../prebuilt/fontconfig)
  HARFBUZZ_PREBUILT=$($READLINK -f ../prebuilt/harfbuzz)
  FRIBIDI_PREBUILT=$($READLINK -f ../prebuilt/fribidi)
  FREETYPE_PREBUILT=$($READLINK -f ../prebuilt/freetype)
  LIBUNIBREAK_PREBUILT=$($READLINK -f ../prebuilt/libunibreak)
  LIBXML2_PREBUILT=$($READLINK -f ../prebuilt/libxml2)
  LIBPNG_PREBUILT=$($READLINK -f ../prebuilt/libpng)
  ZLIB_PREBUILT=$($READLINK -f ../prebuilt/zlib)

  export PKG_CONFIG_PATH="${FREETYPE_PREBUILT}/lib/${ABI}/pkgconfig:${FRIBIDI_PREBUILT}/lib/${ABI}/pkgconfig:${HARFBUZZ_PREBUILT}/lib/${ABI}/pkgconfig:${FONTCONFIG_PREBUILT}/lib/${ABI}/pkgconfig:${LIBUNIBREAK_PREBUILT}/lib/${ABI}/pkgconfig:${LIBXML2_PREBUILT}/lib/${ABI}/pkgconfig:${ZLIB_PREBUILT}/lib/${ABI}/pkgconfig:${LIBPNG_PREBUILT}/lib/${ABI}/pkgconfig"
  export PKG_CONFIG_LIBDIR="${FREETYPE_PREBUILT}/lib/${ABI}/pkgconfig:${FRIBIDI_PREBUILT}/lib/${ABI}/pkgconfig:${HARFBUZZ_PREBUILT}/lib/${ABI}/pkgconfig:${FONTCONFIG_PREBUILT}/lib/${ABI}/pkgconfig:${LIBUNIBREAK_PREBUILT}/lib/${ABI}/pkgconfig:${LIBXML2_PREBUILT}/lib/${ABI}/pkgconfig:${ZLIB_PREBUILT}/lib/${ABI}/pkgconfig:${LIBPNG_PREBUILT}/lib/${ABI}/pkgconfig"

  # libass's configure.ac actually checks libunibreak via PKG_CHECK_MODULES,
  # same as freetype2/fribidi/harfbuzz above (same "checking for X >= ver"
  # message format gives this away) - it's not a plain AC_CHECK_LIB probe.
  # Setting LIBUNIBREAK_CFLAGS/LIBUNIBREAK_LIBS directly makes PKG_CHECK_MODULES
  # skip its own pkg-config lookup entirely, so this doesn't depend on
  # libunibreak.pc existing/being named exactly right.
  export LIBUNIBREAK_CFLAGS="-I${LIBUNIBREAK_PREBUILT}/include"
  export LIBUNIBREAK_LIBS="-L${LIBUNIBREAK_PREBUILT}/lib/${ABI} -lunibreak"

  export CFLAGS="-fPIC -O3 -I${FREETYPE_PREBUILT}/include -I${FREETYPE_PREBUILT}/include/freetype2 -I${FRIBIDI_PREBUILT}/include -I${HARFBUZZ_PREBUILT}/include -I${FONTCONFIG_PREBUILT}/include -I${LIBUNIBREAK_PREBUILT}/include"
  export CXXFLAGS="-fPIC -O3 -I${FREETYPE_PREBUILT}/include -I${FREETYPE_PREBUILT}/include/freetype2 -I${FRIBIDI_PREBUILT}/include -I${HARFBUZZ_PREBUILT}/include -I${FONTCONFIG_PREBUILT}/include -I${LIBUNIBREAK_PREBUILT}/include"
  export LDFLAGS="-L${FREETYPE_PREBUILT}/lib/${ABI} -L${FRIBIDI_PREBUILT}/lib/${ABI} -L${HARFBUZZ_PREBUILT}/lib/${ABI} -L${FONTCONFIG_PREBUILT}/lib/${ABI} -L${LIBUNIBREAK_PREBUILT}/lib/${ABI} -L${LIBXML2_PREBUILT}/lib/${ABI} -L${ZLIB_PREBUILT}/lib/${ABI} -L${LIBPNG_PREBUILT}/lib/${ABI} -Wl,-z,max-page-size=16384"

  LIBASS_EXTRA_FLAGS=""
  if [ "${ABI}" = "x86" ]; then
    LIBASS_EXTRA_FLAGS="--disable-asm"
  fi

  if [ ! -f "${PREBUILT_DIR}/lib/${ABI}/libass.a" ]
  then
    echo "Building libass for ${ABI}..."
    cd libass
    ./autogen.sh
    ./configure --host=${TARGET} --prefix="${PREFIX}" --libdir="${PREFIX}/lib/${ABI}" --enable-static --disable-shared --enable-fontconfig --enable-libunibreak ${LIBASS_EXTRA_FLAGS}
    make clean
    make -j${CORES}
    make install
    cd ..
  else
    echo "Libass already built for ${ABI}"
  fi
done
