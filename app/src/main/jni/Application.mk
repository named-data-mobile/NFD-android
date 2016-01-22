APP_ABI := all

APP_STL := gnustl_shared
APP_CPPFLAGS += -fexceptions -frtti -std=c++11 -Wno-deprecated-declarations

NDK_TOOLCHAIN_VERSION := 5
APP_PLATFORM := android-19
