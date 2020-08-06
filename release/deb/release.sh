#!/usr/bin/env bash

BRANCH=${1:-"master"}
DIR="/tmp/kafka-topology-builder"
DEB_DIR="/tmp/kafka-topology-builder/target/"

git clone https://github.com/purbon/kafka-topology-builder.git $DIR
cd $DIR
git checkout $BRANCH
mvn package
mvn jdeb:jdeb

mkdir -p /vagrant/release
cp DEB_DIR/* /vagrant/release
