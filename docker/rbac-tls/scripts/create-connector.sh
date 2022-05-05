#!/usr/bin/env bash

curl -i -X POST -H "Accept:application/json" \
    -u "professor:professor" \
    -k \
    -H  "Content-Type:application/json" https://localhost:8083/connectors/ \
    -d '{
               "name": "ibm-mq-source",
               "config": {
                    "connector.class": "io.confluent.connect.ibm.mq.IbmMQSourceConnector",
                    "kafka.topic": "MyKafkaTopicName",
                    "mq.hostname": "ibmmq",
                    "mq.port": "1414",
                    "mq.transport.type": "client",
                    "mq.queue.manager": "QM1",
                    "mq.channel": "DEV.APP.SVRCONN",
                    "mq.username": "app",
                    "mq.password": "passw0rd",
                    "jms.destination.name": "DEV.QUEUE.1",
                    "jms.destination.type": "queue",
                    "confluent.license": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJDb25mbHVlbnRQcm9mZXNzaW9uYWxTZXJ2aWNlcyIsImV4cCI6MTcwNDQ5OTIwMCwiaWF0IjoxNjA5ODkxMjAwLCJpc3MiOiJDb25mbHVlbnQiLCJtb25pdG9yaW5nIjp0cnVlLCJuYjQiOjE2MDk5NDQ1MjYsInN1YiI6ImNvbnRyb2wtY2VudGVyIn0.JGa0Thb5zHXfMtZliIo6kCDFrNSCoVygUsYnPZ0Sg9q2nLb-Mo6G14Jd1oX57AYdQIV4V0RnXvKUxfVmNyfXLtTFnIk3rCaQlo_jPg7Hsg9ifOgtIPR4y1yIJs2DQtult_w4xoopAl4PhtoO13CAB9uVfTzcpOto2m4G4DVRMNFKobofVp3iJaDD1cuYeYOSf94chILCGEQ98wEJ-ktBgKtpuIwAisIjBW4F6oVm7chKhILpQ478zz4fkWaU-8xz4jnjVaGqLNmVEqJ5jf7ApMsNxCv5hWbo8_gkCucMg1fcUSzR5FkhkhFfh0o7DZweJJAiZ2Snr2cKNddCVRnLbQ",
                    "confluent.topic.bootstrap.servers": "broker:9092"
          }}'
