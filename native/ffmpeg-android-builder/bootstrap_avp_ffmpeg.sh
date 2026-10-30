#!/bin/bash

source ../../AVP/android-setup-light.sh

mkdir -p ../prebuilt/ffmpeg
PREBUILT_DIR=$($READLINK -f ../prebuilt/ffmpeg)

# skip if all prebuilt libs already exist
if [ -f "${PREBUILT_DIR}/dist-full-armeabi-v7a/lib/libavcodec.so" ] && \
   [ -f "${PREBUILT_DIR}/dist-full-arm64-v8a/lib/libavcodec.so" ] && \
   [ -f "${PREBUILT_DIR}/dist-full-x86/lib/libavcodec.so" ] && \
   [ -f "${PREBUILT_DIR}/dist-full-x86_64/lib/libavcodec.so" ]; then
  echo "All ffmpeg prebuilt libs already exist, skipping"
  exit 0
fi

for ARCH in arm arm64 x86 x86_64
do
  case "${ARCH}" in
    'arm')
      ABI=armeabi-v7a ;;
    'arm64')
      ABI=arm64-v8a ;;
    *)
      ABI=${ARCH} ;;
    esac
  if [ ! -d dist-full-${ABI} ]
  then
   ./build.sh -a ${ARCH} -c config_full.sh || exit $?
  fi
done
