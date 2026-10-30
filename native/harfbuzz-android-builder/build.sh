#!/bin/bash

source ../../AVP/android-setup-light.sh

LOCAL_PATH=$($READLINK -f .)
mkdir -p ../prebuilt/harfbuzz
PREBUILT_DIR=$($READLINK -f ../prebuilt/harfbuzz)

# Check if prebuilt libraries already exist
if [ -f "${PREBUILT_DIR}/lib/armeabi-v7a/libharfbuzz.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/arm64-v8a/libharfbuzz.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/x86/libharfbuzz.a" ] && \
   [ -f "${PREBUILT_DIR}/lib/x86_64/libharfbuzz.a" ]; then
  echo "All harfbuzz prebuilt libs already exist, skipping"
  exit 0
fi

if [ ! -d "harfbuzz" ]
then
  git clone https://github.com/harfbuzz/harfbuzz.git --depth=1 -b 15.0.0
fi

API_LEVEL=21

for ABI in armeabi-v7a arm64-v8a x86 x86_64
do
  case "${ABI}" in
    'arm64-v8a')
      CPU_FAMILY='aarch64'
      TARGET=aarch64-linux-android
      ;;
    'armeabi-v7a')
      CPU_FAMILY='arm'
      TARGET=armv7a-linux-androideabi
      ;;
    'x86')
      CPU_FAMILY='x86'
      TARGET=i686-linux-android
      ;;
    'x86_64')
      CPU_FAMILY='x86_64'
      TARGET=x86_64-linux-android
      ;;
  esac

  PREFIX="${PREBUILT_DIR}"

  OS=$(uname -s | tr '[:upper:]' '[:lower:]')
  TOOLCHAIN="${NDK_PATH}/toolchains/llvm/prebuilt/${OS}-x86_64"

  export AR="${TOOLCHAIN}/bin/llvm-ar"
  export STRIP="${TOOLCHAIN}/bin/llvm-strip"
  export CC="${TOOLCHAIN}/bin/${TARGET}${API_LEVEL}-clang"
  export CXX="${TOOLCHAIN}/bin/${TARGET}${API_LEVEL}-clang++"

  ZLIB_PREBUILT=$($READLINK -f ../prebuilt/zlib)
  LIBPNG_PREBUILT=$($READLINK -f ../prebuilt/libpng)
  FREETYPE_PREBUILT=$($READLINK -f ../prebuilt/freetype)

  # Create cross-file for this ABI
  user_config="meson-cross-${ABI}.txt"
  rm -f $user_config
  cat <<EOF > $user_config
[binaries]
c = '$CC'
cpp = '$CXX'
ar = '$AR'
strip = '$STRIP'
pkg-config = 'pkg-config'

[properties]
pkg_config_libdir = ['${FREETYPE_PREBUILT}/lib/${ABI}/pkgconfig', '${ZLIB_PREBUILT}/lib/${ABI}/pkgconfig', '${LIBPNG_PREBUILT}/lib/${ABI}/pkgconfig']

[host_machine]
system = 'android'
cpu_family = '$CPU_FAMILY'
cpu = '$CPU_FAMILY'
endian = 'little'
EOF

  if [ ! -f "${PREBUILT_DIR}/lib/${ABI}/libharfbuzz.a" ]
  then
    echo "Building harfbuzz for ${ABI}..."
    cd harfbuzz
    rm -rf "build-${ABI}"
    meson setup "build-${ABI}" --cross-file "../meson-cross-${ABI}.txt" --prefix="${PREFIX}" --libdir="lib/${ABI}" --default-library=static -Dtests=disabled -Ddocs=disabled -Dfreetype=enabled  -Dsubset=disabled -Draster=disabled -Dvector=disabled -Dgpu=disabled -Dutilities=disabled
    ninja -C "build-${ABI}" install
    cd ..
  else
    echo "Harfbuzz already built for ${ABI}"
  fi
done
