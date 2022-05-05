#!/usr/bin/env bash

docker-compose exec broker kafka-console-consumer --bootstrap-server thusnelda:9095 \
                    --topic test \
                    --consumer.config /etc/client-configs/thusnelda.properties --from-beginning
