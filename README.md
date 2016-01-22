NFD on Android
==============

[![Build Status](https://travis-ci.org/named-data/NFD-android.svg?branch=master)](https://travis-ci.org/named-data/NFD-android)

## Prerequisites

To compile code, the following is necessary

- Recent version of [Android SDK](http://developer.android.com/sdk/index.html), android-21 SDK
  and 21.1.2 build tools (for build), android-19 SDK (for compatibility), and several other SDK
  components
- [CrystalX Android NDK](https://www.crystax.net/en/download) version 10.3.1
- [Gradle build automation system](http://gradle.org/) version 2.10

Example script for Ubuntu 14.04 to get all dependencies, download SDK and NDK:

    CRYSTAX_NDK_VERSION=10.3.1
    GRADLE_VERSION=2.10
    SDK_VERSION=24.4.1

    BUILD_TOOLS_VERSION=21.1.2
    COMPILE_SDK_VERSION=21

    sudo apt-get install -y build-essential git openjdk-7-jdk unzip

    # SDK binaries need i386 libraries
    sudo dpkg --add-architecture i386;
    sudo apt-get update -qq; sudo apt-get install -y libc6:i386 libncurses5:i386 libstdc++6:i386 zlib1g:i386

    wget https://www.crystax.net/download/crystax-ndk-$CRYSTAX_NDK_VERSION-linux-x86_64.tar.xz
    tar xf crystax-ndk-$CRYSTAX_NDK_VERSION-linux-x86_64.tar.xz
    rm crystax-ndk-$CRYSTAX_NDK_VERSION-linux-x86_64.tar.xz

    wget http://dl.google.com/android/android-sdk_r$SDK_VERSION-linux.tgz
    tar zxf android-sdk_r$SDK_VERSION-linux.tgz
    rm android-sdk_r$SDK_VERSION-linux.tgz

    export ANDROID_HOME=`pwd`/android-sdk-linux
    export PATH=${PATH}:${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools

    echo "y" | android update sdk --filter platform-tools,build-tools-$BUILD_TOOLS_VERSION,android-$COMPILE_SDK_VERSION,extra-android-support,extra-android-m2repository,extra-google-m2repository --no-ui --all --force
    echo "y" | android update sdk --filter "android-19" --no-ui --all --force

    wget https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip
    unzip gradle-$GRADLE_VERSION-bin.zip
    rm gradle-$GRADLE_VERSION-bin.zip
    export PATH=$PATH:`pwd`/gradle-$GRADLE_VERSION/bin

## Building


    git clone --recursive http://gerrit.named-data.net/NFD-android
    echo sdk.dir=`pwd`/android-sdk-linux > NFD-android/local.properties
    echo ndk.dir=`pwd`/crystax-ndk-10.3.1 >> NFD-android/local.properties
    cd NFD-android

    ../gradle/gradle-2.10/bin/gradle assembleRelease


## Setting up environment using Vagrant

The development setup can be set up with [Vagrant](https://www.vagrantup.com/) and scripts provided
in `.vagrant/` folder.  After vagrant command-line is installed, the following will create VM
environment and fetch all necessary dependencies:

    cd .vagrant
    vagrant up
    vagrant ssh

Refer to vagrant documentation for more information.
