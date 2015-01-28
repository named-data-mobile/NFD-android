LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := nfd-example
LOCAL_SRC_FILES := android-logger-streambuf.cpp wrappers-example.cpp
# LOCAL_SHARED_LIBRARIES :=
LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)
