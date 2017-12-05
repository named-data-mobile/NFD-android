LOCAL_PATH := $(call my-dir)
LOCAL_PATH_SAVED := $(LOCAL_PATH)

NFD_BOOST_LIBS = system filesystem chrono program_options random thread

# nfd itself
include $(CLEAR_VARS)
LOCAL_MODULE := nfd-daemon
LOCAL_SHARED_LIBRARIES := libssl_shared libcrypto_shared ndn-cxx $(addsuffix _shared,$(addprefix boost_,$(NFD_BOOST_LIBS)))
NFD_DAEMON_SRC_FILES := \
    core/city-hash.cpp \
    core/config-file.cpp \
    core/global-io.cpp \
    core/manager-base.cpp \
    core/network-interface-predicate.cpp \
    core/network.cpp \
    core/privilege-helper.cpp \
    core/random.cpp \
    core/rtt-estimator.cpp \
    core/scheduler.cpp \
    ../nfd-android/custom-logger.cpp \
    ../nfd-android/custom-logger-factory.cpp \
    \
    daemon/face/channel.cpp \
    daemon/face/face-counters.cpp \
    daemon/face/face-system.cpp \
    daemon/face/face.cpp \
    daemon/face/generic-link-service.cpp \
    daemon/face/internal-face.cpp \
    daemon/face/internal-transport.cpp \
    daemon/face/link-service.cpp \
    daemon/face/lp-fragmenter.cpp \
    daemon/face/lp-reassembler.cpp \
    daemon/face/lp-reliability.cpp \
    daemon/face/multicast-udp-transport.cpp \
    daemon/face/null-face.cpp \
    daemon/face/protocol-factory.cpp \
    daemon/face/tcp-channel.cpp \
    daemon/face/tcp-factory.cpp \
    daemon/face/tcp-transport.cpp \
    daemon/face/transport.cpp \
    daemon/face/udp-channel.cpp \
    daemon/face/udp-factory.cpp \
    daemon/face/udp-protocol.cpp \
    daemon/face/unicast-udp-transport.cpp \
    daemon/face/websocket-channel.cpp \
    daemon/face/websocket-factory.cpp \
    daemon/face/websocket-transport.cpp \
    daemon/fw/access-strategy.cpp \
    daemon/fw/algorithm.cpp \
    daemon/fw/asf-measurements.cpp \
    daemon/fw/asf-probing-module.cpp \
    daemon/fw/asf-strategy.cpp \
    daemon/fw/best-route-strategy.cpp \
    daemon/fw/best-route-strategy2.cpp \
    daemon/fw/client-control-strategy.cpp \
    daemon/fw/face-table.cpp \
    daemon/fw/forwarder.cpp \
    daemon/fw/multicast-strategy.cpp \
    daemon/fw/ncc-strategy.cpp \
    daemon/fw/process-nack-traits.cpp \
    daemon/fw/retx-suppression-exponential.cpp \
    daemon/fw/retx-suppression-fixed.cpp \
    daemon/fw/strategy.cpp \
    daemon/fw/unsolicited-data-policy.cpp \
    daemon/main.cpp \
    daemon/mgmt/command-authenticator.cpp \
    daemon/mgmt/face-manager.cpp \
    daemon/mgmt/fib-manager.cpp \
    daemon/mgmt/forwarder-status-manager.cpp \
    daemon/mgmt/general-config-section.cpp \
    daemon/mgmt/nfd-manager-base.cpp \
    daemon/mgmt/strategy-choice-manager.cpp \
    daemon/mgmt/tables-config-section.cpp \
    daemon/nfd.cpp \
    daemon/table/cleanup.cpp \
    daemon/table/cs-entry-impl.cpp \
    daemon/table/cs-entry.cpp \
    daemon/table/cs-policy-lru.cpp \
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
    daemon/table/name-tree-hashtable.cpp \
    daemon/table/name-tree-iterator.cpp \
    daemon/table/name-tree.cpp \
    daemon/table/network-region-table.cpp \
    daemon/table/pit-entry.cpp \
    daemon/table/pit-face-record.cpp \
    daemon/table/pit-in-record.cpp \
    daemon/table/pit-iterator.cpp \
    daemon/table/pit-out-record.cpp \
    daemon/table/pit.cpp \
    daemon/table/strategy-choice-entry.cpp \
    daemon/table/strategy-choice.cpp \
    daemon/table/strategy-info-host.cpp \
    \
    rib/auto-prefix-propagator.cpp \
    rib/fib-update.cpp \
    rib/fib-updater.cpp \
    rib/propagated-entry.cpp \
    rib/readvertise/client-to-nlsr-readvertise-policy.cpp \
    rib/readvertise/nfd-rib-readvertise-destination.cpp \
    rib/readvertise/readvertise-destination.cpp \
    rib/readvertise/readvertise.cpp \
    rib/readvertise/readvertised-route.cpp \
    rib/rib-entry.cpp \
    rib/rib-manager.cpp \
    rib/rib-update-batch.cpp \
    rib/rib-update.cpp \
    rib/rib.cpp \
    rib/route.cpp \
    rib/service.cpp
LOCAL_SRC_FILES := $(addprefix NFD/,$(NFD_DAEMON_SRC_FILES))
LOCAL_CPPFLAGS := \
    -I$(LOCAL_PATH)/nfd-android \
    -I$(LOCAL_PATH)/NFD \
    -I$(LOCAL_PATH)/NFD/core \
    -I$(LOCAL_PATH)/NFD/daemon \
    -I$(LOCAL_PATH)/NFD/rib \
    -I$(LOCAL_PATH)/NFD/websocketpp
LOCAL_LDLIBS := -llog
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/nfd-android $(LOCAL_PATH)/NFD
include $(BUILD_SHARED_LIBRARY)
