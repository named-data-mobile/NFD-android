NFD on Android
==============

[![Build Status](https://travis-ci.org/named-data/NFD-android.svg?branch=master)](https://travis-ci.org/named-data/NFD-android)

## Prerequisites

To compile code, the following is necessary

- Appropriate [Android SDK](http://developer.android.com/sdk/index.html)
- [CrystalX Android NDK](https://www.crystax.net/en/download) version 10.2.1 or later

Example script for Ubuntu 14.04 to get all dependencies, download SDK and NDK:

    sudo apt-get install -y build-essential git openjdk-7-jdk unzip
    wget -q https://www.crystax.net/download/crystax-ndk-10.2.1-linux-x86_64.tar.bz2
    tar jxf crystax-ndk-10.2.1-linux-x86_64.tar.bz2

    wget -q http://dl.google.com/android/android-sdk_r24.0.2-linux.tgz
    tar zxf android-sdk_r24.0.2-linux.tgz
    rm android-sdk_r24.0.2-linux.tgz

    echo y |  ./android-sdk-linux/tools/android update sdk -a -u -t "android-19"

    wget -q https://services.gradle.org/distributions/gradle-2.6-bin.zip
    unzip gradle-2.6-bin.zip

    SDK_TOOLS_VERSION=24.0.2
    BUILD_TOOLS_VERSION=21.1.2
    COMPILE_SDK_VERSION=21

    (sudo dpkg --add-architecture i386; sudo apt-get update -qq; sudo apt-get install -y libc6:i386 libncurses5:i386 libstdc++6:i386 zlib1g:i386)

    export ANDROID_HOME=`pwd`/android-sdk-linux
    export PATH=${PATH}:${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools

    echo "y" | ~/android-sdk-linux/tools/android update sdk --filter platform-tools,build-tools-$BUILD_TOOLS_VERSION,android-$COMPILE_SDK_VERSION,extra-android-support,extra-android-m2repository,extra-google-m2repository --no-ui --all --force

    echo "y" | ~/android-sdk-linux/tools/android update sdk --filter "android-19" --no-ui --all --force

## Building


    git clone --recursive http://gerrit.named-data.net/NFD-android
    echo sdk.dir=`pwd`/android-sdk-linux > NFD-android/local.properties
    echo ndk.dir=`pwd`/crystax-ndk-10.2.1 >> NFD-android/local.properties
    cd NFD-android

    ../gradle/gradle-2.6/bin/gradle assembleRelease


## Setting up environment using Vagrant

The development setup can be set up with [Vagrant](https://www.vagrantup.com/) and scripts provided
in `.vagrant/` folder.  After vagrant command-line is installed, the following will create VM
environment and fetch all necessary dependencies:

    cd .vagrant
    vagrant up
    vagrant ssh

Refer to vagrant documentation for more information.
