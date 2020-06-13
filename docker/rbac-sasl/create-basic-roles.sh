#!/usr/bin/env bash

################################## GET KAFKA CLUSTER ID ########################
ZK_CONTAINER=zookeeper
ZK_PORT=2181
echo "Retrieving Kafka cluster id from docker-container '$ZK_CONTAINER' port '$ZK_PORT'"
KAFKA_CLUSTER_ID=$(docker exec -it $ZK_CONTAINER zookeeper-shell localhost:$ZK_PORT get /cluster/id 2> /dev/null | grep \"version\" | jq -r .id)
if [ -z "$KAFKA_CLUSTER_ID" ]; then
    echo "Failed to retrieve kafka cluster id from zookeeper"
    exit 1
fi

## Login into MDS
XX_CONFLUENT_USERNAME=professor XX_CONFLUENT_PASSWORD=professor confluent login --url http://localhost:8090

SUPER_USER=professor
SUPER_USER_PASSWORD=professor
SUPER_USER_PRINCIPAL="User:$SUPER_USER"

## Create Service Roles
CONNECT_PRINCIPAL="User:fry"
C3_PRINCIPAL="User:hermes"
SR_PRINCIPAL="User:leela"
OTHER_PRINCIPAL="User:zoidberg"

CONNECT=connect-cluster
SR=schema-registry
C3=c3-cluster

################################### SETUP SUPERUSER ###################################
echo "Creating Super User role bindings"

confluent iam rolebinding create \
    --principal $SUPER_USER_PRINCIPAL  \
    --role SystemAdmin \
    --kafka-cluster-id $KAFKA_CLUSTER_ID

confluent iam rolebinding create \
    --principal $SUPER_USER_PRINCIPAL \
    --role SystemAdmin \
    --kafka-cluster-id $KAFKA_CLUSTER_ID \
    --schema-registry-cluster-id $SR

confluent iam rolebinding create \
    --principal $SUPER_USER_PRINCIPAL \
    --role SystemAdmin \
    --kafka-cluster-id $KAFKA_CLUSTER_ID \
    --connect-cluster-id $CONNECT

echo "Finished setting up role bindings"
echo "    kafka cluster id: $KAFKA_CLUSTER_ID"
echo "    connect cluster id: $CONNECT"
echo "    schema registry cluster id: $SR"
echo
echo "    super user account: $SUPER_USER_PRINCIPAL"
echo "    connect service account: $CONNECT_PRINCIPAL"
echo "    schema registry service account: $SR_PRINCIPAL"
echo "    C3 service account: $C3_PRINCIPAL"
echo "    Other service account: $OTHER_PRINCIPAL"
