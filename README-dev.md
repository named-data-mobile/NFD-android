Developer Notes
===============

## To build debug version of NFD

Configure `ndk.dir` and `sdk.dir` in `local.properties` file.  For example:

    ndk.dir=/opt/crystax-ndk-10.2.1
    sdk.dir=/opt/android-sdk-macosx

Alternatively, you can set `ANDROID_NDK_ROOT` environment variable to point towards NDK folder.

Build process can be customized using several environment variables:

- `NDK_DEBUG`

   If set, the native code is built in debug mode.  For example:

        NDK_DEBUG=1 build assembleDebug

- `NDK_BUILD_ABI=<comma-separated-list-of-platforms>`

  If set, the native code is built only for the specified platforms.  For example, the following snippet will build NFD only for x86 platform:

        export NDK_BUILD_ABI=x86
        build assembleDebug

- `NDK_BUILD_PARALLEL=<number>`

  If set, the build will be limited with the specified number of parallel builds.  Otherwise, the build will be automatically parallelized based on the number of available CPUs.

## To build the release version of NFD

In order to build the release version of NFD, it needs to be properly signed.  In order to specify keystore, keystore's password, key in the keystore, and key's password, add the following configuration into `local.properties` file:


    keystore=<path-to-.keystore-file>
    keystore.password=<keystore-password>
    keystore.key.alias=<key-alias-in-keystore>
    keystore.key.password=<key-password>

For more information about application signing and instructions on how to generate keys, refer to [Android Documentation](http://developer.android.com/tools/publishing/app-signing.html).

## To publish the release version on Google Play

The release version of NFD can be automatically uploaded to Google Play, if Google Play parameters are configured in `local.properties` files:

    google-play=<path-to-json-config>

Note that `.json` config can be downloaded from Google Developer Console.  For more information, refer to [Google Play Publishing Plugin](https://github.com/Triple-T/gradle-play-publisher).
