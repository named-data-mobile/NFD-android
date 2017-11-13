#!/usr/bin/env bash
set -e
set -x

android-sdk-linux/tools/bin/sdkmanager "ndk-bundle"
git clone https://github.com/cawka/android-crew-staging.git android-sdk-linux/ndk-bundle/crew.dir

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

export CREW_OWNER=cawka
# export CREW_DOWNLOAD_BASE=http://irl.cs.ucla.edu/~cawka/android-crew-staging/staging/

android-sdk-linux/ndk-bundle/crew.dir/crew install target/sqlite:3.18.0 target/openssl:1.0.2m target/boost:1.65.1

echo ndk.dir=`pwd`/android-sdk-linux/ndk-bundle >> local.properties
