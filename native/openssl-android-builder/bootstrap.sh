#!/bin/bash

source ../../AVP/android-setup-light.sh

mkdir -p ../prebuilt/openssl
PREBUILT_DIR=$($READLINK -f ../prebuilt/openssl)

# skip if all prebuilt libs already exist
if [ -f "${PREBUILT_DIR}/dist-armeabi-v7a/lib/libcrypto.a" ] && \
   [ -f "${PREBUILT_DIR}/dist-arm64-v8a/lib/libcrypto.a" ] && \
   [ -f "${PREBUILT_DIR}/dist-x86/lib/libcrypto.a" ] && \
   [ -f "${PREBUILT_DIR}/dist-x86_64/lib/libcrypto.a" ]; then
  echo "All openssl prebuilt libs already exist, skipping"
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
  if [ ! -d dist-${ABI} ]
  then
   ./build.sh -a ${ARCH} || exit $?
  fi
done
