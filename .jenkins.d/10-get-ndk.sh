#!/usr/bin/env bash
set -e
set -x

if [ ! -f downloads/crystax-ndk-10.1.0-linux-x86_64.7z ]; then
    mkdir downloads || true
    cd downloads
    wget --no-check-certificate https://www.crystax.net/download/crystax-ndk-10.1.0-linux-x86_64.7z
    cd ..
fi

if [ ! -d crystax-ndk-10.1.0 ]; then
  echo -en 'travis_fold:start:NDK\r'
  7z x downloads/crystax-ndk-10.1.0-linux-x86_64.7z > /dev/null
  find crystax-ndk-10.1.0 -name byteswap.h -exec sed -i -e 's/ swap/ bswap/g' {} \;
  echo -en 'travis_fold:end:NDK\r'
fi

echo ndk.dir=`pwd`/crystax-ndk-10.1.0 >> local.properties
