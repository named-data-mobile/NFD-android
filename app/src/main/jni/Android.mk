LOCAL_PATH := $(call my-dir)
LOCAL_PATH_SAVED := $(LOCAL_PATH)

include $(CLEAR_VARS)
LOCAL_MODULE := nfd-wrapper
LOCAL_SRC_FILES := nfd-wrapper.cpp
LOCAL_SHARED_LIBRARIES := nfd-daemon ndn-cxx boost_system_shared
LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)

include $(LOCAL_PATH_SAVED)/ndn-cxx.mk
include $(LOCAL_PATH_SAVED)/nfd.mk

$(call import-module,boost/1.57.0)
