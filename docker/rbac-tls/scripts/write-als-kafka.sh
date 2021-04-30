#!/usr/bin/env bash

docker-compose exec broker kafka-console-producer --broker-list broker:9092 \
                    --topic test \
                    --producer.config /etc/kafka/client.properties
