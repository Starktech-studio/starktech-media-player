#!/bin/bash

source ../../AVP/android-setup-light.sh

LOCAL_PATH=$($READLINK -f .)
mkdir -p ../prebuilt/libunibreak
PREBUILT_DIR=$($READLINK -f ../prebuilt/libunibreak)

if [ -f "${PREBUILT_DIR}/lib/armeabi-v7a/libunibreak.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/arm64-v8a/libunibreak.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/x86/libunibreak.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/x86_64/libunibreak.a" ]; then
  echo "All libunibreak prebuilt libs already exist, skipping"
  exit 0
fi

if [ ! -d "libunibreak" ]
then
  git clone https://github.com/adah1972/libunibreak.git
  cd libunibreak
  git checkout libunibreak_8_0
  cd ..
fi

if [ ! -f "libunibreak/configure" ]; then
  cd libunibreak
  autoreconf -fiv -Wno-obsolete
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

  export CFLAGS="-fPIC -O3"
  export CXXFLAGS="-fPIC -O3"
  export LDFLAGS="-L${PREFIX}/lib/${ABI} -Wl,-z,max-page-size=16384"

  if [ ! -f "${PREBUILT_DIR}/lib/${ABI}/libunibreak.a" ]
  then
    echo "Building libunibreak for ${ABI}..."
    cd libunibreak
    ./configure --host=${TARGET} --prefix="${PREFIX}" --libdir="${PREFIX}/lib/${ABI}" --enable-static --disable-shared
    make clean
    make -j${CORES}
    make install
    cd ..
  else
    echo "Libunibreak already built for ${ABI}"
  fi
done
