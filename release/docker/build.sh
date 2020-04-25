#!/usr/bin/env bash

cp  ../../target/kafka-topology-builder-jar-with-dependencies.jar .
cp ../../src/main/scripts/kafka-topology-builder.sh .

docker build  -t purbon/kafka-topology-builder .

rm kafka-topology-builder-jar-with-dependencies.jar
rm kafka-topology-builder.sh