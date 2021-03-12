#!/usr/bin/env bash

cp  ../../target/julie-ops.jar .
cp ../../src/main/scripts/julie-ops-cli.sh .

docker build --compress -t purbon/kafka-topology-builder .

rm julie-ops.jar
rm julie-ops-cli.sh