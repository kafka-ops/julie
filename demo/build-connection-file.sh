#!/usr/bin/env bash


CONFIG_FILE="
bootstrap.servers=pkc-4ygn6.europe-west3.gcp.confluent.cloud:9092 \n
security.protocol=SASL_SSL \n
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule   required username=\"$CLUSTER_API_KEY\"   password=\"$CLUSTER_API_SECRET\"; \n
ssl.endpoint.identification.algorithm=https \n
sasl.mechanism=PLAIN \n
# Confluent Cloud Schema Registry \n
# schema.registry.url=https://psrc-4r0k9.westus2.azure.confluent.cloud \n
# basic.auth.credentials.source=USER_INFO \n
# schema.registry.basic.auth.user.info={{ SR_API_KEY }}:{{ SR_API_SECRET }}
"

echo -e $CONFIG_FILE
