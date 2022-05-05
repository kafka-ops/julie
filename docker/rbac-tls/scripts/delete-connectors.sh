#!/usr/bin/env bash

curl -i -X DELETE -H "Accept:application/json" \
    -u "professor:professor" \
    -k \
    -H  "Content-Type:application/json" https://localhost:8083/connectors/ibm-mq-source
