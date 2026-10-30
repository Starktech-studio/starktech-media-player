#!/bin/bash

source ../../AVP/android-setup-light.sh

LOCAL_PATH=$($READLINK -f .)
mkdir -p ../prebuilt/libmysofa
PREBUILT_DIR=$($READLINK -f ../prebuilt/libmysofa)

# skip git clone if all prebuilt libs already exist
if [ -f "${PREBUILT_DIR}/lib/armeabi-v7a/libmysofa.so" ] && \
   [ -f "${PREBUILT_DIR}/lib/arm64-v8a/libmysofa.so" ] && \
   [ -f "${PREBUILT_DIR}/lib/x86/libmysofa.so" ] && \
   [ -f "${PREBUILT_DIR}/lib/x86_64/libmysofa.so" ]; then
  echo "All libmysofa prebuilt libs already exist, skipping"
  mkdir -p ${PREBUILT_DIR}/include
  cp libmysofa/src/hrtf/mysofa.h ${PREBUILT_DIR}/include/
  exit 0
fi

if [ ! -d "libmysofa" ]
then
  git clone https://github.com/hoene/libmysofa.git --depth=1 -b v1.3.5
fi

mkdir -p ${PREBUILT_DIR}/include
cp libmysofa/src/hrtf/mysofa.h ${PREBUILT_DIR}/include/

API_LEVEL=23

for ABI in armeabi-v7a arm64-v8a x86 x86_64
do
  BUILD_DIR="build-${ABI}"

  if [ ! -f "${PREBUILT_DIR}/lib/${ABI}/libmysofa.so" ]
  then

    rm -rf ${BUILD_DIR}
    mkdir -p ${BUILD_DIR}

    pushd ${BUILD_DIR}
    ${CMAKE_PATH}/bin/cmake \
      -GNinja \
      -DANDROID_PLATFORM=${API_LEVEL} \
      -DBUILD_TESTS=OFF \
      -DBUILD_SHARED_LIBS=ON \
      -DANDROID_ABI=${ABI} \
      -DANDROID_NDK=${NDK_PATH} \
      -DCMAKE_LIBRARY_OUTPUT_DIRECTORY=${PREBUILT_DIR}/lib/${ABI} \
      -DCMAKE_BUILD_TYPE=Release  \
      -DCMAKE_TOOLCHAIN_FILE=${NDK_PATH}/build/cmake/android.toolchain.cmake \
      -DANDROID_NATIVE_API_LEVEL=${API_LEVEL} \
      -DCMAKE_SHARED_LINKER_FLAGS="-Wl,-z,max-page-size=16384" \
      -DCMAKE_EXE_LINKER_FLAGS="-Wl,-z,max-page-size=16384" \
      -DCMAKE_MODULE_LINKER_FLAGS="-Wl,-z,max-page-size=16384" \
      -DCMAKE_POLICY_VERSION_MINIMUM=3.5 \
      ../libmysofa
	  ninja
    popd
    cp ${BUILD_DIR}/src/mysofa_export.h ${PREBUILT_DIR}/include/
  else
    echo "Already built for ${ABI}"
  fi
done
