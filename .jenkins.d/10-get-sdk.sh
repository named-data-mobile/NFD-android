#!/usr/bin/env bash
set -e
set -x

GRADLE_VERSION=2.10
SDK_VERSION=24.4.1
BUILD_TOOLS_VERSION=21.1.2
COMPILE_SDK_VERSION=21

SDK=android-sdk-linux

export ANDROID_HOME=`pwd`/$SDK
export PATH=${PATH}:${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools

if [ ! -d $SDK ]; then
    wget -nv http://dl.google.com/android/android-sdk_r$SDK_VERSION-linux.tgz
    tar zxf android-sdk_r$SDK_VERSION-linux.tgz
    rm android-sdk_r$SDK_VERSION-linux.tgz

    echo "y" | android update sdk --filter platform-tools,build-tools-$BUILD_TOOLS_VERSION,android-$COMPILE_SDK_VERSION,extra-android-support,extra-android-m2repository,extra-google-m2repository --no-ui --all --force
    echo "y" | android update sdk --filter "android-19" --no-ui --all --force
fi

if [ ! -d gradle-$GRADLE_VERSION ]; then
    wget -nv https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip
    unzip gradle-$GRADLE_VERSION-bin.zip
    rm gradle-$GRADLE_VERSION-bin.zip

    ln -s `pwd`/gradle-$GRADLE_VERSION/bin/gradle gradle
fi

echo sdk.dir=`pwd`/$SDK >> local.properties
