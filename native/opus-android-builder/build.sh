#!/bin/bash

source ../../AVP/android-setup-light.sh

LOCAL_PATH=$($READLINK -f .)
mkdir -p ../prebuilt/opus
PREBUILT_DIR=$($READLINK -f ../prebuilt/opus)

# Public headers installed by Opus with OPUS_CUSTOM_MODES=OFF (the default).
OPUS_HEADERS=(opus.h opus_defines.h opus_multistream.h opus_projection.h opus_types.h)
headers_present=yes
for header in "${OPUS_HEADERS[@]}"; do
  if [ ! -f "${PREBUILT_DIR}/include/opus/${header}" ]; then
    headers_present=no
    break
  fi
done

# Skip source checkout only when both libraries and headers are installed.
if [ "${headers_present}" = yes ] && \
   [ -f "${PREBUILT_DIR}/lib/armeabi-v7a/libopus.so" ] && \
   [ -f "${PREBUILT_DIR}/lib/arm64-v8a/libopus.so" ] && \
   [ -f "${PREBUILT_DIR}/lib/x86/libopus.so" ] && \
   [ -f "${PREBUILT_DIR}/lib/x86_64/libopus.so" ]; then
  echo "All opus prebuilt libs and headers already exist, skipping"
  exit 0
fi

if [ ! -d "opus" ]
then
  git clone https://github.com/xiph/opus.git
  # get a fixed version after 1.3.1 to get cmake support
  cd opus
  git checkout 084bb7327ce90171c4dcf2a58fe6ac58ae821dbf
  cat ../CMakeLists.txt.diff | patch -p 1
  cd ..
fi

mkdir -p "${PREBUILT_DIR}/include/opus" || exit $?
for header in "${OPUS_HEADERS[@]}"; do
  cp "${LOCAL_PATH}/opus/include/${header}" "${PREBUILT_DIR}/include/opus/" || exit $?
done

API_LEVEL=21

for ABI in armeabi-v7a arm64-v8a x86 x86_64
do
  if [ ! -f "${PREBUILT_DIR}/lib/${ABI}/libopus.so" ]
  then
    rm -rf build-${ABI}
    mkdir -p build-${ABI}
    pushd build-${ABI}
    ${CMAKE_PATH}/bin/cmake \
      -GNinja \
      -DBUILD_SHARED_LIBS=ON \
      -DANDROID_ABI=${ABI} \
      -DANDROID_NDK=${NDK_PATH} \
      -DCMAKE_LIBRARY_OUTPUT_DIRECTORY=${PREBUILT_DIR}/lib/${ABI} \
      -DCMAKE_BUILD_TYPE=Release \
      -DCMAKE_TOOLCHAIN_FILE=${NDK_PATH}/build/cmake/android.toolchain.cmake \
      -DANDROID_NATIVE_API_LEVEL=${API_LEVEL} \
      -DCMAKE_SHARED_LINKER_FLAGS="-Wl,-z,max-page-size=16384" \
      -DCMAKE_EXE_LINKER_FLAGS="-Wl,-z,max-page-size=16384" \
      -DCMAKE_MODULE_LINKER_FLAGS="-Wl,-z,max-page-size=16384" \
      ../opus
    ninja
    popd
  else
    echo "Already built for ${ABI}"
  fi
done
