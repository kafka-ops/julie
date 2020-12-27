#!/usr/bin/env bash

KAFKA_TOPOLOGY_BUILDER_HOME=/usr/local/kafka-topology-builder
KTB_JAR=$KAFKA_TOPOLOGY_BUILDER_HOME/bin/kafka-topology-builder.jar
JAVA_PATH=java

if [ -z "$KTB_OPTIONS" ]; then
  KTB_OPTIONS=""
fi

$JAVA_PATH $KTB_OPTIONS -jar $KTB_JAR "$@"
