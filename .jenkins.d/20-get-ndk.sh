#!/usr/bin/env bash
set -e
set -x

android-sdk-linux/cmdline-tools/latest/bin/sdkmanager "ndk-bundle"
git clone https://github.com/named-data-mobile/android-crew-staging.git android-sdk-linux/ndk-bundle/crew.dir

if [[ ! -z $GEM_PATH ]]; then
    # Hack for unset GEM_PATH in crew tool
    ORIG_RUBY=`which ruby`
    echo '#!/usr/bin/env bash' > ruby
    echo "export GEM_HOME=$GEM_HOME" >> ruby
    echo "export GEM_PATH=$GEM_PATH" >> ruby
    echo "exec $ORIG_RUBY \$@" >> ruby
    chmod 755 ruby

    export CREW_TOOLS_DIR=`pwd`
    export PATH=`pwd`:$PATH
fi

export CREW_OWNER=named-data-mobile

android-sdk-linux/ndk-bundle/crew.dir/crew install target/sqlite target/openssl target/boost
android-sdk-linux/ndk-bundle/crew.dir/crew install target/ndn_cxx target/nfd

echo ndk.dir=`pwd`/android-sdk-linux/ndk-bundle >> local.properties
