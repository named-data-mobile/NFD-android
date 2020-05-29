#!/usr/bin/env bash
set -e
set -x

SDK=android-sdk-linux

mkdir -p $SDK
cd $SDK

wget https://dl.google.com/android/repository/commandlinetools-linux-6514223_latest.zip
unzip commandlinetools-linux-6514223_latest.zip
rm commandlinetools-linux-6514223_latest.zip
mkdir -p cmdline-tools
mv tools cmdline-tools/latest

export ANDROID_HOME=`pwd`
export PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

echo "y" | sdkmanager "platform-tools"
sdkmanager "platforms;android-29"

cd ..

echo sdk.dir=`pwd`/$SDK >> local.properties
