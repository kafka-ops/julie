#!/usr/bin/env bash

HOME_DIR=`pwd`
NETWORK="kerberos-demo.local"

docker run -v  $HOME_DIR:/app \
           -v kerberos_secret:/var/lib/secret \
           -v $HOME_DIR/docker/kerberos/krb5/krb5.conf:/etc/krb5.conf \
           --network $NETWORK \
           --hostname client.kerberos-demo.local \
           purbon/kafka-topology-builder:latest /app/example/scripts/run-kerberos-app.sh