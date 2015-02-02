#!/usr/bin/env bash
set -e
set -x

echo -en 'travis_fold:start:deps\r'

sudo apt-get install -y build-essential git openjdk-7-jdk unzip p7zip-full

# SDK binaries need i386 libraries
sudo dpkg --add-architecture i386;
sudo apt-get update -qq; sudo apt-get install -y libc6:i386 libncurses5:i386 libstdc++6:i386 zlib1g:i386

echo -en 'travis_fold:end:deps\r'
