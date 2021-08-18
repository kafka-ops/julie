#!/usr/bin/env bash

kinit -k -t /var/lib/secret/kafka-admin.key admin/for-kafka

julie-ops-cli.sh  \
    --brokers kafka:9093 \
    --topology /app/example/descriptor-only-topics.yaml \
    --allowDelete \
    --clientConfig /app/example/topology-builder-kerberos.properties