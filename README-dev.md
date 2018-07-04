Developer Notes
===============

## To build debug version of NDN-Android

Configure `ndk.dir` and `sdk.dir` in `local.properties` file.  For example:

    sdk.dir=/opt/android-sdk
    ndk.dir=/opt/android-sdk/ndk-bundle

Build process can be customized using several environment variables:

- `NDK_DEBUG`

   If set, the native code is built in debug mode.  For example:

        NDK_DEBUG=1 ./gradlew assembleDebug

## To build the release version of NFD

In order to build the release version of NFD, it needs to be properly signed.  In order to specify keystore, keystore's password, key in the keystore, and key's password, add the following configuration into `local.properties` file:


    keystore=<path-to-.keystore-file>
    keystore.password=<keystore-password>
    keystore.key.alias=<key-alias-in-keystore>
    keystore.key.password=<key-password>

For more information about application signing and instructions on how to generate keys, refer to [Android Documentation](https://developer.android.com/studio/publish/app-signing).

## To publish the release version on Google Play

The release version of NFD can be automatically uploaded to Google Play, if Google Play parameters are configured in `local.properties` files:

    google-play=<path-to-json-config>

Note that `.json` config can be downloaded from Google Developer Console.  For more information, refer to [Google Play Publishing Plugin](https://github.com/Triple-T/gradle-play-publisher).

## Building NDN-Android with different version of underlying NFD, ndn-cxx, or other dependencies

NDN-Android build assumes existence of prebuilt binaries (=shared libraries) for all dependencies, including NFD, ndn-cxx, boost, sqlite, libssl.  As of version 0.6.2-1, these dependencies are managed by an external tool called [crew tool](https://github.com/named-data-mobile/android-crew-staging), a hacked version of the official tool that was designed for [CrystaX NDK](https://github.com/crystax/android-crew-staging) (the hacked version targeting the official NDK r18, support specifically clang, and not all packages would work).

The basic workflow with the crew tool is as follows:

- Getting the tool into the right place relative to NDK folder:

        cd /opt/android-sdk/android-ndk
        git clone https://github.com/named-data-mobile/android-crew-staging crew.dir

- Installing precompiled binary (downloaded from Github relese page)

        ./crew.dir/crew install target/<package>[:<version>]

    `target/` indicates that the package is intended for Android target.  The official tool also supports installing dependencies for the host system (`host/`), but the hacked version has not been tested for that.

    `<package>` one of the available packages.

    `:<version>` an optional version, in case there are multiple versions available

    To get list of all available packages and their versions, you can use `./crew.dir/crew list` command.

- Installing packages from source

    The process is similar to installing precompiled binary but split into two stages: getting source and actually building the package

    Get the source

        ./crew.dir/crew source <package>[:<version>]

    Build

        ./crew.dir/crew build target/<package>[:<version>]

    The `source` command downloading source of the specified package (URL or git repo defined in `crew.dir/formula/packages/<package>.rb` --- note that getting code from git repo may fail with old versions of ruby.  If you are getting strange segfaults, try using a different version of ruby, known to work with version 2.5.1p57).   After downloading, it will apply any patches for the package version in `./crew.dir/patches/packages/<package>/<version>` (all `.patch` files will be applied to the source).

    If additional patches need to be applied or old patch re-applied, you need to remove source and add source again:

        ./crew.dir/crew remove-source <package>[:<version>]
        ./crew.dir/crew source <package>[:<version>]

    NOTE. To build package from a different source code, you need to modify the package definition (`./crew.dir/formula/packages/<package>.rb`): source code can be downloaded from an online archive or any git source (using tag or commit-id).  See the existing packages for reference.

- To delete the downloaded version of a package:

        ./crew.dir/crew remove <package>[:<version>]

- To delete source and the built version of a package:

        ./crew.dir/crew remove-source <package>[:<version>]
        ./crew.dir/crew remove <package>[:<version>]

Note that whenever `ndn_cxx`, `nfd`, or dependencies package is (re-)built or (re-)installed, you just need to clean and re-build NDN-android (`./gradlew clean` and `./gradlew assembleDebug`).  It will automatically use the available package.  However, if you changed version of built/installed NFD, you will need to update `app/src/main/jni/Android.mk`, which hardcodes the specific version of NFD package.  All other dependencies are transitively defined by the crew tool and do not need to be explicitly defined.

### Quick reference for crew tool internals

A trimmed file structure:

    ├── etc
    │   ├── ca-certificates.crt
    │   └── shasums.txt
    ├── library
    │   ├── arch.rb
    │   ├── base_package.rb
    │   ├── build.rb
    │   ├── build_dependency.rb
    │   ├── cmd
    │   │   ├── <...>.rb
    │   ├── command_options.rb
    │   ├── compiler_wrapper_helper.rb
    │   ├── deb.rb
    │   ├── exceptions.rb
    │   ├── extend
    │   │   ├── dir.rb
    │   │   └── module.rb
    │   ├── formula.rb
    │   ├── formulary.rb
    │   ├── github.rb
    │   ├── global.rb
    │   ├── host_base.rb
    │   ├── multi_version.rb
    │   ├── package.rb
    │   ├── patch.rb
    │   ├── platform.rb
    │   ├── properties.rb
    │   ├── release.rb
    │   ├── shasum.rb
    │   ├── single_version.rb
    │   ├── target_base.rb
    │   ├── tool.rb
    │   ├── toolchain.rb
    │   ├── utility.rb
    │   └── utils.rb
    ├── formula
    │   ├── packages
    │   │   ├── <package-formula>.rb
    │   └── tools
    │       ├── <tool>.rb
    │       └── ....rb
    ├── patches
    │   ├── packages
    │   │   ├── <package-name>
    │   │   │   └── <package-version>
    │   │   │       ├── <file1>.patch
    │   │   │       ├── <file2>.patch
    │   │   │       └── ...


Notable places:

- `formula/packages/<package-formula>.rb`: ruby scripts defining formula where to download source, dependencies, and how to build the package

- `patches/packages/<package-name>/<package-version>/`: patch files to be applied for a version of a package

- `library/toolchain.rb`: definition of the toolchains, including necessary compilation flags for different platforms.

- `library/package.rb`: default/available options to customize package build and basic build stages.  Note that to build a package, crew tool creates "compiler wrapper" placed in the build directory of a package using the specified name. This wrapper, internally, runs a proper compiler executable with any additional parameters (as defined in `library/toolchain.rb`).  Depending on the options defined for the package, the wrapper can also "wrap" compilation and linking flags, but it may not work.  Normally, during the build, the crew tool defines `CFLAGS`, `CXXGLAGS`, `LDFLAGS`, and bunch of other flags (including paths to various tools such as nm, ar, etc.).
