LOCAL_PATH := $(call my-dir)
LOCAL_PATH_SAVED := $(LOCAL_PATH)

include $(CLEAR_VARS)
LOCAL_MODULE := nfd-wrapper
LOCAL_SRC_FILES := nfd-wrapper.cpp
LOCAL_SHARED_LIBRARIES := nfd_shared ndn_cxx_shared boost_system_shared boost_thread_shared boost_log_shared
LOCAL_LDLIBS := -llog -latomic
LOCAL_CFLAGS := -DBOOST_LOG_DYN_LINK=1
include $(BUILD_SHARED_LIBRARY)

$(call import-module,../packages/nfd/0.6.6)
