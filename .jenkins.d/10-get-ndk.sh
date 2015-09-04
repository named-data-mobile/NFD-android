#!/usr/bin/env bash
set -e
set -x

URL=https://www.crystax.net/download/
NDK=crystax-ndk-10.2.1

NDK_FILE="$NDK-linux-x86_64.tar.bz2"

if [ ! -f downloads/$NDK_FILE ]; then
    mkdir downloads || true
    cd downloads
    wget --no-check-certificate $URL$NDK_FILE
    cd ..
fi

if [ ! -d $NDK ]; then
    echo -en 'travis_fold:start:NDK\r'
    pv -f downloads/$NDK_FILE | tar xjf -
    find $NDK -name byteswap.h -exec sed -i -e 's/ swap/ bswap/g' {} \;
    echo -en 'travis_fold:end:NDK\r'
fi

echo ndk.dir=`pwd`/$NDK >> local.properties
