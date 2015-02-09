LOCAL_PATH := $(call my-dir)
LOCAL_PATH_SAVED := $(LOCAL_PATH)

include $(CLEAR_VARS)
LOCAL_MODULE := nfd-wrapper
LOCAL_SRC_FILES := android-logger-streambuf.cpp nfd-wrapper.cpp
LOCAL_LDLIBS := -llog
LOCAL_SHARED_LIBRARIES := nfd-daemon
include $(BUILD_SHARED_LIBRARY)

include $(LOCAL_PATH_SAVED)/ndn-cxx.mk
include $(LOCAL_PATH_SAVED)/nfd.mk
