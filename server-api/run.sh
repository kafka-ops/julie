#!/usr/bin/env bash

SASL_OPTS="" #"-Djava.security.auth.login.config=demo/sasl.config"
SERVER_OPTS="-Dmicronaut.topology_builder.brokers_list=localhost:9092 -Dmicronaut.topology_builder.admin_config_file=docker/demo/topology-builder.properties"
java $SASL_OPTS $SERVER_OPTS -jar target/server-api-0.1.jar
