LOCAL_PATH := $(call my-dir)
LOCAL_PATH_SAVED := $(LOCAL_PATH)

include $(CLEAR_VARS)
LOCAL_MODULE := ndn-cxx
NDN_CXX_BOOST_LIBS = system filesystem date_time iostreams program_options chrono random
LOCAL_SHARED_LIBRARIES := cryptopp $(addsuffix _shared,$(addprefix boost_,$(NDN_CXX_BOOST_LIBS)))
LOCAL_STATIC_LIBRARIES := sqlite3_static boost_regex_static
NDN_CXX_SRC_FILES := \
    data.cpp \
    encoding/block.cpp \
    encoding/buffer.cpp \
    encoding/cryptopp/asn_ext.cpp \
    encoding/encoder.cpp \
    encoding/estimator.cpp \
    encoding/nfd-constants.cpp \
    encoding/oid.cpp \
    exclude.cpp \
    face.cpp \
    interest-filter.cpp \
    interest.cpp \
    key-locator.cpp \
    management/nfd-channel-status.cpp \
    management/nfd-command-options.cpp \
    management/nfd-control-command.cpp \
    management/nfd-control-parameters.cpp \
    management/nfd-control-response.cpp \
    management/nfd-controller.cpp \
    management/nfd-face-event-notification.cpp \
    management/nfd-face-query-filter.cpp \
    management/nfd-face-status.cpp \
    management/nfd-fib-entry.cpp \
    management/nfd-forwarder-status.cpp \
    management/nfd-rib-entry.cpp \
    management/nfd-strategy-choice.cpp \
    meta-info.cpp \
    name-component.cpp \
    name.cpp \
    security/certificate-cache-ttl.cpp \
    security/certificate-extension.cpp \
    security/certificate-subject-description.cpp \
    security/certificate.cpp \
    security/digest-sha256.cpp \
    security/identity-certificate.cpp \
    security/key-chain.cpp \
    security/key-params.cpp \
    security/public-key.cpp \
    security/sec-public-info-sqlite3.cpp \
    security/sec-public-info.cpp \
    security/sec-rule-relative.cpp \
    security/sec-rule-specific.cpp \
    security/sec-tpm-file.cpp \
    security/sec-tpm.cpp \
    security/secured-bag.cpp \
    security/signature-sha256-with-ecdsa.cpp \
    security/signature-sha256-with-rsa.cpp \
    security/validator-config.cpp \
    security/validator-regex.cpp \
    security/validator.cpp \
    selectors.cpp \
    signature-info.cpp \
    signature.cpp \
    transport/tcp-transport.cpp \
    transport/unix-transport.cpp \
    util/config-file.cpp \
    util/crypto.cpp \
    util/digest.cpp \
    util/dns.cpp \
    util/dummy-client-face.cpp \
    util/ethernet.cpp \
    util/face-uri.cpp \
    util/in-memory-storage-entry.cpp \
    util/in-memory-storage-fifo.cpp \
    util/in-memory-storage-lfu.cpp \
    util/in-memory-storage-lru.cpp \
    util/in-memory-storage-persistent.cpp \
    util/in-memory-storage.cpp \
    util/indented-stream.cpp \
    util/network-monitor.cpp \
    util/random.cpp \
    util/regex/regex-top-matcher.cpp \
    util/scheduler-scoped-event-id.cpp \
    util/scheduler.cpp \
    util/segment-fetcher.cpp \
    util/signal-connection.cpp \
    util/signal-scoped-connection.cpp \
    util/time-unit-test-clock.cpp \
    util/time.cpp
LOCAL_SRC_FILES := $(addprefix ndn-cxx/src/,$(NDN_CXX_SRC_FILES))
LOCAL_CPPFLAGS := -I$(LOCAL_PATH)/ndn-cxx/src -I$(LOCAL_PATH)/ndn-cxx-android
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/ndn-cxx-android
include $(BUILD_SHARED_LIBRARY)

include $(LOCAL_PATH_SAVED)/cryptopp/Android.mk

$(call import-module,boost/1.57.0)
$(call import-module,sqlite/3)
