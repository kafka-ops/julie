#!/usr/bin/env bash

HOME_DIR=`pwd`
NETWORK="rbac-sasl_default"


docker run -v  $HOME_DIR:/app --network $NETWORK \
    purbon/kafka-topology-builder:latest kafka-topology-builder.sh  \
    --brokers broker:9092 \
    --topology /app/example/descriptor-with-rbac.yaml \
    --clientConfig /app/example/topology-builder-rbac.properties