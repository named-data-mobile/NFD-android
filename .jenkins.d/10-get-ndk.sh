#!/usr/bin/env bash
set -e
set -x

CRYSTAX_NDK_VERSION=10.3.1

URL=https://www.crystax.net/download/
NDK=crystax-ndk-$CRYSTAX_NDK_VERSION

NDK_FILE="$NDK-linux-x86_64.tar.xz"

if [ ! -f downloads/$NDK_FILE ]; then
    mkdir downloads || true
    cd downloads
    wget --no-check-certificate $URL$NDK_FILE
    cd ..
fi

if [ ! -d $NDK ]; then
    export XZ_DEFAULTS=--memlimit=300MiB
    echo -en 'travis_fold:start:NDK\r'

    # To save space
    EXCLUDES="toolchains/*-clang3.6 toolchains/*-3.6 \
            toolchains/*-clang3.7 \
            toolchains/*-3.7 \
            toolchains/*-4.9 \
            sources/cxx-stl/gabi++ \
            sources/cxx-stl/llvm-libc++ \
            sources/cxx-stl/llvm-libc++abi \
            sources/cxx-stl/stlport \
            sources/cxx-stl/llvm-libc++ \
            sources/cxx-stl/gnu-libstdc++/4.9 \
            sources/icu \
            sources/boost/*/libs/*/gnu-4.9 \
            sources/boost/*/libs/*/llvm-3.6 \
            sources/boost/*/libs/*/llvm-3.7 \
            sources/objc \
            sources/python"

    pv -f downloads/$NDK_FILE | tar xJf - $(for i in $EXCLUDES; do echo "--exclude $i"; done | xargs)
    echo -en 'travis_fold:end:NDK\r'
fi

echo ndk.dir=`pwd`/$NDK >> local.properties

cd crystax-ndk-$CRYSTAX_NDK_VERSION/sources
curl -L -o openssl.tar.gz https://github.com/named-data-mobile/crystax-prebuilt-openssl/archive/crystax-$CRYSTAX_NDK_VERSION.tar.gz
tar zx --strip-components 1 -C openssl -f openssl.tar.gz
rm openssl.tar.gz
cd ../..
