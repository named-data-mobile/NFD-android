#!/usr/bin/env bash
set -e
set -x

echo -en 'travis_fold:start:deps\r'

sudo apt-get install -y build-essential git openjdk-7-jdk unzip
sudo apt-get install -y lib32stdc++6 lib32z1 lib32z1-dev

echo -en 'travis_fold:end:deps\r'
