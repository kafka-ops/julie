#!/usr/bin/env bash

BRANCH=${1:-"master"}
DIR="/tmp/kafka-topology-builder"
RPM_DIR="/tmp/kafka-topology-builder/target/rpm/kafka-topology-builder/RPMS/noarch/"

git clone https://github.com/purbon/kafka-topology-builder.git $DIR
cd $DIR
git checkout $BRANCH
mvn assembly:assembly
mvn rpm:rpm

mkdir -p /vagrant/release
cp $RPM_DIR/* /vagrant/release
