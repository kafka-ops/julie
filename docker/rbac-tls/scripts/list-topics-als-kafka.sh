#!/usr/bin/env bash

docker-compose exec broker kafka-topics --bootstrap-server broker:9092 \
                                        --list \
                                        --command-config /etc/client-configs/client.properties
