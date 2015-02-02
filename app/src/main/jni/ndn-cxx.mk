LOCAL_PATH := $(call my-dir)
LOCAL_PATH_SAVED := $(LOCAL_PATH)

include $(CLEAR_VARS)
LOCAL_MODULE := ndn-cxx
NDN_CXX_BOOST_LIBS = system filesystem date_time iostreams regex program_options chrono random
LOCAL_SHARED_LIBRARIES := cryptopp $(addsuffix _shared,$(addprefix boost_,$(NDN_CXX_BOOST_LIBS)))
LOCAL_STATIC_LIBRARIES := sqlite3
NDN_CXX_SRC_FILES := name.cpp util/dns.cpp util/time-unit-test-clock.cpp util/segment-fetcher.cpp util/config-file.cpp util/in-memory-storage.cpp util/in-memory-storage-lfu.cpp util/in-memory-storage-fifo.cpp util/ethernet.cpp util/crypto.cpp util/regex/regex-top-matcher.cpp util/signal-scoped-connection.cpp util/dummy-client-face.cpp util/scheduler.cpp util/indented-stream.cpp util/signal-connection.cpp util/in-memory-storage-persistent.cpp util/digest.cpp util/in-memory-storage-lru.cpp util/in-memory-storage-entry.cpp util/time.cpp util/random.cpp util/face-uri.cpp interest-filter.cpp signature.cpp transport/tcp-transport.cpp transport/unix-transport.cpp management/nfd-face-event-notification.cpp management/nfd-control-command.cpp management/nfd-command-options.cpp management/nfd-face-status.cpp management/nfd-control-response.cpp management/nfd-rib-entry.cpp management/nfd-controller.cpp management/nfd-forwarder-status.cpp management/nfd-channel-status.cpp management/nfd-fib-entry.cpp management/nfd-strategy-choice.cpp management/nfd-face-query-filter.cpp management/nfd-control-parameters.cpp exclude.cpp name-component.cpp face.cpp key-locator.cpp data.cpp interest.cpp selectors.cpp meta-info.cpp signature-info.cpp encoding/nfd-constants.cpp encoding/oid.cpp encoding/cryptopp/asn_ext.cpp encoding/block.cpp encoding/buffer.cpp security/sec-public-info-sqlite3.cpp security/signature-sha256-with-rsa.cpp security/public-key.cpp security/certificate-cache-ttl.cpp security/sec-rule-specific.cpp security/sec-tpm-file.cpp security/digest-sha256.cpp security/sec-public-info.cpp security/certificate.cpp security/identity-certificate.cpp security/certificate-subject-description.cpp security/certificate-extension.cpp security/sec-tpm.cpp security/validator.cpp security/sec-rule-relative.cpp security/key-params.cpp security/secured-bag.cpp security/key-chain.cpp security/signature-sha256-with-ecdsa.cpp security/validator-config.cpp security/validator-regex.cpp
LOCAL_SRC_FILES := $(addprefix ndn-cxx/src/,$(NDN_CXX_SRC_FILES))
LOCAL_CPPFLAGS := -I$(LOCAL_PATH)/ndn-cxx/src -I$(LOCAL_PATH)/ndn-cxx-android
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/ndn-cxx-android
include $(BUILD_SHARED_LIBRARY)

include $(LOCAL_PATH_SAVED)/sqlite3/Android.mk
include $(LOCAL_PATH_SAVED)/cryptopp/Android.mk

$(call import-module,boost/1.57.0)
