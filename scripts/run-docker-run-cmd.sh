#!/usr/bin/env bash

docker run -t -i \
      -v /Users/pere/work/kafka-topology-builder/example:/example \
      purbon/kafka-topology-builder:srlatest \
      kafka-topology-builder.sh \
      --brokers pkc-4ygn6.europe-west3.gcp.confluent.cloud:9092 \
      --clientConfig /example/topology-builder-with-schema-cloud.properties \
      --topology /example/descriptor.yaml -quiet

