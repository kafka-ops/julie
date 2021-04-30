#!/usr/bin/env bash

docker-compose exec broker kafka-console-producer --broker-list thusnelda:9095 \
                    --topic test-thusnelda \
                    --producer.config /etc/kafka/thusnelda.properties
