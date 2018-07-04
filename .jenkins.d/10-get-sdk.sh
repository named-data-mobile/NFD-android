#!/usr/bin/env bash
set -e
set -x

SDK=android-sdk-linux

mkdir -p $SDK
cd $SDK
wget -q https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip
unzip sdk-tools-linux-3859397.zip
rm sdk-tools-linux-3859397.zip

export ANDROID_HOME=`pwd`
export PATH=${PATH}:${ANDROID_HOME}/tools/bin:${ANDROID_HOME}/platform-tools

echo "y" | sdkmanager "platform-tools"
sdkmanager "platforms;android-28"

cd ..

echo sdk.dir=`pwd`/$SDK >> local.properties
