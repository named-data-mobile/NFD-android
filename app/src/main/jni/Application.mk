APP_ABI := armeabi-v7a arm64-v8a x86 x86_64

APP_STL := c++_shared
APP_CPPFLAGS += -fexceptions -frtti -std=c++14
# -Wno-deprecated-declarations

NDK_TOOLCHAIN_VERSION := clang
APP_PLATFORM := android-23
