#!/usr/bin/env bash

cp  ../../target/kafka-topology-builder.jar .
cp ../../src/main/scripts/kafka-topology-builder.sh .

docker build  -t purbon/kafka-topology-builder .

rm kafka-topology-builder.jar
rm kafka-topology-builder.sh