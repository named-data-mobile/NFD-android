LOCAL_PATH := $(call my-dir)
LOCAL_PATH_SAVED := $(LOCAL_PATH)

include $(CLEAR_VARS)
LOCAL_MODULE := ndn-cxx
NDN_CXX_BOOST_LIBS = system filesystem date_time iostreams program_options chrono random
LOCAL_SHARED_LIBRARIES := cryptopp_shared libcrypto_shared libssl_shared $(addsuffix _shared,$(addprefix boost_,$(NDN_CXX_BOOST_LIBS)))
LOCAL_STATIC_LIBRARIES := sqlite3_static boost_regex_static
NDN_CXX_SRC_FILES := \
    data.cpp \
    encoding/block-helpers.cpp \
    encoding/block.cpp \
    encoding/buffer-stream.cpp \
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
    link.cpp \
    lp/cache-policy.cpp \
    lp/detail/field-info.cpp \
    lp/nack-header.cpp \
    lp/nack.cpp \
    lp/packet.cpp \
    meta-info.cpp \
    mgmt/control-response.cpp \
    mgmt/dispatcher.cpp \
    mgmt/nfd/channel-status.cpp \
    mgmt/nfd/command-options.cpp \
    mgmt/nfd/control-command.cpp \
    mgmt/nfd/control-parameters.cpp \
    mgmt/nfd/controller.cpp \
    mgmt/nfd/face-event-notification.cpp \
    mgmt/nfd/face-query-filter.cpp \
    mgmt/nfd/face-status.cpp \
    mgmt/nfd/fib-entry.cpp \
    mgmt/nfd/forwarder-status.cpp \
    mgmt/nfd/rib-entry.cpp \
    mgmt/nfd/status-dataset.cpp \
    mgmt/nfd/strategy-choice.cpp \
    mgmt/status-dataset-context.cpp \
    name-component.cpp \
    name.cpp \
    security/certificate-cache-ttl.cpp \
    security/certificate-container.cpp \
    security/command-interest-validator.cpp \
    security/detail/openssl-helper.cpp \
    security/digest-sha256.cpp \
    security/identity-container.cpp \
    security/identity.cpp \
    security/key-chain.cpp \
    security/key-container.cpp \
    security/key-params.cpp \
    security/key.cpp \
    security/pib-memory.cpp \
    security/pib-sqlite3.cpp \
    security/pib.cpp \
    security/safe-bag.cpp \
    security/sec-public-info-sqlite3.cpp \
    security/sec-public-info.cpp \
    security/sec-rule-relative.cpp \
    security/sec-rule-specific.cpp \
    security/sec-tpm-file.cpp \
    security/sec-tpm.cpp \
    security/secured-bag.cpp \
    security/security-common.cpp \
    security/signature-sha256-with-ecdsa.cpp \
    security/signature-sha256-with-rsa.cpp \
    security/signing-helpers.cpp \
    security/signing-info.cpp \
    security/transform/base64-decode.cpp \
    security/transform/base64-encode.cpp \
    security/transform/block-cipher.cpp \
    security/transform/bool-sink.cpp \
    security/transform/buffer-source.cpp \
    security/transform/digest-filter.cpp \
    security/transform/hex-decode.cpp \
    security/transform/hex-encode.cpp \
    security/transform/hmac-filter.cpp \
    security/transform/private-key.cpp \
    security/transform/public-key.cpp \
    security/transform/signer-filter.cpp \
    security/transform/step-source.cpp \
    security/transform/stream-sink.cpp \
    security/transform/stream-source.cpp \
    security/transform/strip-space.cpp \
    security/transform/transform-base.cpp \
    security/transform/verifier-filter.cpp \
    security/v1/certificate-extension.cpp \
    security/v1/certificate-subject-description.cpp \
    security/v1/certificate.cpp \
    security/v1/identity-certificate.cpp \
    security/v1/public-key.cpp \
    security/v2/additional-description.cpp \
    security/validator-config.cpp \
    security/validator-regex.cpp \
    security/validator.cpp \
    security/validity-period.cpp \
    selectors.cpp \
    signature-info.cpp \
    signature.cpp \
    transport/tcp-transport.cpp \
    transport/transport.cpp \
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
    util/io.cpp \
    util/network-monitor.cpp \
    util/random.cpp \
    util/regex/regex-top-matcher.cpp \
    util/scheduler-scoped-event-id.cpp \
    util/scheduler.cpp \
    util/segment-fetcher.cpp \
    util/signal-connection.cpp \
    util/signal-scoped-connection.cpp \
    util/sqlite3-statement.cpp \
    util/string-helper.cpp \
    util/time-unit-test-clock.cpp \
    util/time.cpp \
    ../../ndn-cxx-android/ndn-cxx-custom-logger.cpp \
    ../../ndn-cxx-android/ndn-cxx-custom-logging.cpp
LOCAL_SRC_FILES := $(addprefix ndn-cxx/src/,$(NDN_CXX_SRC_FILES))
LOCAL_CPPFLAGS := -I$(LOCAL_PATH)/ndn-cxx/src -I$(LOCAL_PATH)/ndn-cxx-android -I$(LOCAL_PATH)/../../../build/generated/source/ndn-cxx
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/ndn-cxx-android $(LOCAL_PATH)/../../../build/generated/source/include
LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)

include $(LOCAL_PATH_SAVED)/cryptopp/extras/jni/Android.mk

$(call import-module,../packages/boost/1.65.1)
$(call import-module,../packages/sqlite/3.18.0)
$(call import-module,../packages/openssl/1.0.2m)
