# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


LOCAL_PATH := $(call my-dir)

local_src_files := \
    JNIHelp.cpp \
    JniConstants.cpp \
    toStringArray.cpp


#
# NDK-only build for the target (device), using libc++.
# - Relies only on NDK exposed functionality.
# - This doesn't include JniInvocation.
#

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libnvpnativehelper_compat
LOCAL_CLANG := true
LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/include/nativehelper
LOCAL_EXPORT_C_INCLUDE_DIRS := \
    $(LOCAL_PATH)/include/nativehelper
LOCAL_CFLAGS := -Werror
LOCAL_SRC_FILES := $(local_src_files)
LOCAL_LDFLAGS := Wl,-z,max-page-size=16384 -llog -ldl
include $(BUILD_SHARED_LIBRARY)
