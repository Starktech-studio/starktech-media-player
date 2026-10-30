#!/bin/bash

source ../../AVP/android-setup-light.sh

LOCAL_PATH=$($READLINK -f .)
mkdir -p ../prebuilt/freetype
PREBUILT_DIR=$($READLINK -f ../prebuilt/freetype)

# Check if prebuilt libraries already exist
if [ -f "${PREBUILT_DIR}/lib/armeabi-v7a/libfreetype.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/arm64-v8a/libfreetype.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/x86/libfreetype.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/x86_64/libfreetype.a" ]; then
  echo "All freetype prebuilt libs already exist, skipping"
  exit 0
fi

if [ ! -d "freetype" ]
then
  git clone https://github.com/freetype/freetype.git --depth=1 -b VER-2-14-3
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

  # zlib + libpng are required for color emoji / embedded bitmap glyph support
  ZLIB_PREBUILT=$($READLINK -f ../prebuilt/zlib)
  LIBPNG_PREBUILT=$($READLINK -f ../prebuilt/libpng)

  export PKG_CONFIG_PATH="${ZLIB_PREBUILT}/lib/${ABI}/pkgconfig:${LIBPNG_PREBUILT}/lib/${ABI}/pkgconfig"
  export PKG_CONFIG_LIBDIR="${ZLIB_PREBUILT}/lib/${ABI}/pkgconfig:${LIBPNG_PREBUILT}/lib/${ABI}/pkgconfig"

  export CPPFLAGS="-I${ZLIB_PREBUILT}/include -I${LIBPNG_PREBUILT}/include/libpng16"
  export CFLAGS="-fPIC -O3"
  export CXXFLAGS="-fPIC -O3"
  export LDFLAGS="-L${PREFIX}/lib/${ABI} -L${ZLIB_PREBUILT}/lib/${ABI} -L${LIBPNG_PREBUILT}/lib/${ABI} -Wl,-z,max-page-size=16384"

  if [ ! -f "${PREBUILT_DIR}/lib/${ABI}/libfreetype.a" ]
  then
    echo "Building freetype for ${ABI}..."
    cd freetype
    ./autogen.sh
    ./configure --host=${TARGET} --prefix="${PREFIX}" --libdir="${PREFIX}/lib/${ABI}" --enable-static --disable-shared --without-harfbuzz --with-png=yes --with-zlib=yes --without-bzip2 --without-brotli
    make clean
    make -j${CORES}
    make install
    cd ..
  else
    echo "Freetype already built for ${ABI}"
  fi
done
