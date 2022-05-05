#!/usr/bin/env bash

docker-compose exec broker kafka-console-consumer --bootstrap-server broker:9092 \
                          --topic test \
                          --consumer.config /etc/client-configs/client.properties --from-beginning
