LOCAL_PATH := $(call my-dir)
LOCAL_PATH_SAVED := $(LOCAL_PATH)

NFD_BOOST_LIBS = system filesystem chrono program_options random thread

# core
include $(CLEAR_VARS)
LOCAL_MODULE := nfd-core
LOCAL_SHARED_LIBRARIES := cryptopp ndn-cxx $(addsuffix _shared,$(addprefix boost_,$(NFD_BOOST_LIBS)))
NFD_CORE_SRC_FILES := \
    core/city-hash.cpp \
    core/config-file.cpp \
    core/global-io.cpp \
    core/network-interface.cpp \
    core/network.cpp \
    core/privilege-helper.cpp \
    core/random.cpp \
    core/scheduler.cpp
LOCAL_SRC_FILES := $(addprefix NFD/,$(NFD_CORE_SRC_FILES)) \
    nfd-android/custom-logger.cpp \
    nfd-android/custom-logger-factory.cpp
LOCAL_CPPFLAGS := -I$(LOCAL_PATH)/nfd-android -I$(LOCAL_PATH)/NFD -I$(LOCAL_PATH)/NFD/core
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/nfd-android $(LOCAL_PATH)/NFD $(LOCAL_PATH)/NFD/core
include $(BUILD_STATIC_LIBRARY)

# nfd itself
include $(CLEAR_VARS)
LOCAL_MODULE := nfd-daemon
LOCAL_SHARED_LIBRARIES := cryptopp ndn-cxx $(addsuffix _shared,$(addprefix boost_,$(NFD_BOOST_LIBS)))
LOCAL_STATIC_LIBRARIES := nfd-core
NFD_DAEMON_SRC_FILES := \
    daemon/face/channel.cpp \
    daemon/face/face.cpp \
    daemon/face/internal-client-face.cpp \
    daemon/face/internal-face.cpp \
    daemon/face/multicast-udp-face.cpp \
    daemon/face/ndnlp-data.cpp \
    daemon/face/ndnlp-partial-message-store.cpp \
    daemon/face/ndnlp-sequence-generator.cpp \
    daemon/face/ndnlp-slicer.cpp \
    daemon/face/null-face.cpp \
    daemon/face/tcp-channel.cpp \
    daemon/face/tcp-face.cpp \
    daemon/face/tcp-factory.cpp \
    daemon/face/udp-channel.cpp \
    daemon/face/udp-face.cpp \
    daemon/face/udp-factory.cpp \
    daemon/face/websocket-channel.cpp \
    daemon/face/websocket-face.cpp \
    daemon/face/websocket-factory.cpp \
    daemon/fw/access-strategy.cpp \
    daemon/fw/best-route-strategy.cpp \
    daemon/fw/best-route-strategy2.cpp \
    daemon/fw/broadcast-strategy.cpp \
    daemon/fw/client-control-strategy.cpp \
    daemon/fw/face-table.cpp \
    daemon/fw/forwarder.cpp \
    daemon/fw/multicast-strategy.cpp \
    daemon/fw/ncc-strategy.cpp \
    daemon/fw/retx-suppression-exponential.cpp \
    daemon/fw/retx-suppression-fixed.cpp \
    daemon/fw/retx-suppression.cpp \
    daemon/fw/rtt-estimator.cpp \
    daemon/fw/strategy-registry.cpp \
    daemon/fw/strategy.cpp \
    daemon/mgmt/command-validator.cpp \
    daemon/mgmt/face-manager.cpp \
    daemon/mgmt/fib-manager.cpp \
    daemon/mgmt/forwarder-status-manager.cpp \
    daemon/mgmt/general-config-section.cpp \
    daemon/mgmt/manager-base.cpp \
    daemon/mgmt/strategy-choice-manager.cpp \
    daemon/mgmt/tables-config-section.cpp \
    daemon/nfd.cpp \
    daemon/table/cs-entry-impl.cpp \
    daemon/table/cs-entry.cpp \
    daemon/table/cs-policy-lru.cpp  \
    daemon/table/cs-policy-priority-fifo.cpp \
    daemon/table/cs-policy.cpp \
    daemon/table/cs.cpp \
    daemon/table/dead-nonce-list.cpp \
    daemon/table/fib-entry.cpp \
    daemon/table/fib-nexthop.cpp \
    daemon/table/fib.cpp \
    daemon/table/measurements-accessor.cpp \
    daemon/table/measurements-entry.cpp \
    daemon/table/measurements.cpp \
    daemon/table/name-tree-entry.cpp \
    daemon/table/name-tree.cpp \
    daemon/table/network-region-table.cpp \
    daemon/table/pit-entry.cpp \
    daemon/table/pit-face-record.cpp \
    daemon/table/pit-in-record.cpp \
    daemon/table/pit-out-record.cpp \
    daemon/table/pit.cpp \
    daemon/table/strategy-choice-entry.cpp \
    daemon/table/strategy-choice.cpp \
    daemon/table/strategy-info-host.cpp \
    \
    rib/fib-update.cpp \
    rib/fib-updater.cpp \
    rib/nrd.cpp \
    rib/remote-registrator.cpp \
    rib/rib-entry.cpp \
    rib/rib-manager.cpp \
    rib/rib-status-publisher.cpp \
    rib/rib-update-batch.cpp \
    rib/rib-update.cpp \
    rib/rib.cpp \
    rib/route.cpp
LOCAL_SRC_FILES := $(addprefix NFD/,$(NFD_DAEMON_SRC_FILES))
LOCAL_CPPFLAGS := -I$(LOCAL_PATH)/NFD/daemon -I$(LOCAL_PATH)/NFD/rib -I$(LOCAL_PATH)/NFD/websocketpp
LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)
