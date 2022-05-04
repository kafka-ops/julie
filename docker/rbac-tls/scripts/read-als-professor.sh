#!/usr/bin/env bash

docker-compose exec broker kafka-console-consumer --bootstrap-server broker:9094 \
                    --topic test \
                    --consumer.config /etc/client-configs/professor.properties --from-beginning
