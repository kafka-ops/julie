#!/usr/bin/env bash

BRANCH=${1:-"master"}
DIR="/tmp/kafka-topology-builder"
DEB_DIR="/tmp/kafka-topology-builder/target/"

git clone https://github.com/kafka-ops/kafka-topology-builder.git $DIR
cd $DIR
git checkout $BRANCH
mvn clean package

mkdir -p /vagrant/release
cp $DEB_DIR/*.deb /vagrant/release
