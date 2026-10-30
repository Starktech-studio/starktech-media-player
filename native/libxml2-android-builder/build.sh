#!/bin/bash

source ../../AVP/android-setup-light.sh

LOCAL_PATH=$($READLINK -f .)
mkdir -p ../prebuilt/libxml2
PREBUILT_DIR=$($READLINK -f ../prebuilt/libxml2)

if [ -f "${PREBUILT_DIR}/lib/armeabi-v7a/libxml2.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/arm64-v8a/libxml2.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/x86/libxml2.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/x86_64/libxml2.a" ]; then
  echo "All libxml2 prebuilt libs already exist, skipping"
  exit 0
fi

if [ ! -d "libxml2" ]
then
  git clone https://gitlab.gnome.org/GNOME/libxml2.git
  cd libxml2
  git checkout v2.15.4
  cd ..
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

  ZLIB_PREBUILT=$($READLINK -f ../prebuilt/zlib)

  export CPPFLAGS="-I${ZLIB_PREBUILT}/include"
  export CFLAGS="-fPIC -O3"
  export CXXFLAGS="-fPIC -O3"
  export LDFLAGS="-L${ZLIB_PREBUILT}/lib/${ABI} -Wl,-z,max-page-size=16384"

  export PKG_CONFIG_PATH="${ZLIB_PREBUILT}/lib/${ABI}/pkgconfig"
  export PKG_CONFIG_LIBDIR="${ZLIB_PREBUILT}/lib/${ABI}/pkgconfig"

  if [ ! -f "${PREBUILT_DIR}/lib/${ABI}/libxml2.a" ]
  then
    echo "Building libxml2 for ${ABI}..."
    cd libxml2
    ./autogen.sh --host=${TARGET} --prefix="${PREFIX}" --libdir="${PREFIX}/lib/${ABI}" --enable-static --disable-shared --without-python --without-icu --without-iconv --with-zlib
    make clean
    make -j${CORES}
    make install
    cd ..
  else
    echo "Libxml2 already built for ${ABI}"
  fi
done
