#!/bin/bash

source ../../AVP/android-setup-light.sh

LOCAL_PATH=$($READLINK -f .)
mkdir -p ../prebuilt/zlib
PREBUILT_DIR=$($READLINK -f ../prebuilt/zlib)

if [ -f "${PREBUILT_DIR}/lib/armeabi-v7a/libz.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/arm64-v8a/libz.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/x86/libz.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/x86_64/libz.a" ]; then
  echo "All zlib prebuilt libs already exist, skipping"
  exit 0
fi

if [ ! -d "zlib" ]
then
  git clone https://github.com/madler/zlib.git
  cd zlib
  git checkout v1.3.2
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
  export LDFLAGS="-Wl,-z,max-page-size=16384"

  if [ ! -f "${PREBUILT_DIR}/lib/${ABI}/libz.a" ]
  then
    echo "Building zlib for ${ABI}..."
    cd zlib
    make clean || true
    ./configure --prefix="${PREFIX}" --libdir="${PREFIX}/lib/${ABI}" --static
    make -j${CORES} AR="${AR}" ARFLAGS="rc"
    make install AR="${AR}" ARFLAGS="rc"
    cd ..
  else
    echo "Zlib already built for ${ABI}"
  fi
done
