NFD on Android
==============

[![Build Status](https://travis-ci.org/named-data-mobile/NFD-android.svg?branch=master)](https://travis-ci.org/named-data-mobile/NFD-android)

## Prerequisites

To compile code, the following is necessary

- Recent version of [Android SDK](http://developer.android.com/sdk/index.html)

Example script for Ubuntu 16.04 to get all dependencies, download SDK and NDK:

    sudo apt -q update
    sudo apt -qy upgrade
    sudo apt-get install -y build-essential git openjdk-8-jdk unzip ruby ruby-rugged
    sudo apt-get install -y lib32stdc++6 lib32z1 lib32z1-dev

    mkdir android-sdk-linux
    cd android-sdk-linux
    wget https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip
    unzip sdk-tools-linux-3859397.zip
    rm sdk-tools-linux-3859397.zip

    export ANDROID_HOME=`pwd`
    export PATH=${PATH}:${ANDROID_HOME}/tools/bin:${ANDROID_HOME}/platform-tools

    echo "y" | sdkmanager "platform-tools"
    sdkmanager "platforms;android-28" "ndk-bundle"

    cd ndk-bundle
    git clone https://github.com/named-data-mobile/android-crew-staging crew.dir

    CREW_OWNER=named-data-mobile crew.dir/crew install target/sqlite target/openssl target/boost
    CREW_OWNER=named-data-mobile crew.dir/crew install target/ndn_cxx target/nfd

    cd ..

The above `crew` scripts will install pre-compiled versions of sqlite, openssl, and boost libraries.
For more details about the crew tool, refer to README-dev.md.

## Building

    git clone --recursive http://gerrit.named-data.net/NFD-android
    echo sdk.dir=`pwd`/android-sdk-linux > NFD-android/local.properties
    echo ndk.dir=`pwd`/android-sdk-linux/ndk-bundle >> NFD-android/local.properties
    cd NFD-android

    # Build in release mode (you will need to have proper signing keys configured, see README-dev.md)
    ./gradlew assembleRelease

    # Build in debug mode
    ./gradlew assembleDebug

You can also automatically install debug/release NDN-android to the connected phone

    # build and install release version (will require signing key configuration)
    ./gradlew installRelease

    # build and install debug version
    ./gradlew installDebug

Note that you can limit architectures being built using `NDK_BUILD_ABI` variable.  For example,

    export NDK_BUILD_ABI=armeabi-v7a,x86_64

will limit build to `armeabi-v7a` and `x86_64`.

By default, the build script will try to parallelize build to the number of CPUs.  This can be
overridden using `NDK_BUILD_PARALLEL` variable.

To upload `.apk` files to Google Play (need configuration of keys and Google Play credentials, see README-dev.md):

    ./gradlew publishRelease

## Setting up environment using Vagrant

The development setup can be set up with [Vagrant](https://www.vagrantup.com/) and scripts provided
in `.vagrant/` folder.  After vagrant command-line is installed, the following will create VM
environment and fetch all necessary dependencies:

    cd .vagrant
    vagrant up
    vagrant ssh

Refer to vagrant documentation for more information.
