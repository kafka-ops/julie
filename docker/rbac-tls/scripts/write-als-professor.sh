#!/usr/bin/env bash

docker-compose exec broker kafka-console-producer --broker-list broker:9094 \
                    --topic test \
                    --producer.config /etc/kafka/professor.properties
