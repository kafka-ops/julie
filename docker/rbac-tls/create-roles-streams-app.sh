#!/usr/bin/env bash

## Login into MDS
XX_CONFLUENT_USERNAME=professor XX_CONFLUENT_PASSWORD=professor confluent login --url http://localhost:8090


## Create Service Roles
STREAMS_PRINCIPAL="User:zoidberg"
KAFKA_CLUSTER_ID="x64IAgb0TfOs-3-YoGB4gA"

################################### STREAMS ###################################

echo "Creating Kafka Streams role bindings"

# Allow Streams to read the input topics:
#kafka-acls -authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:alice --operation Read --topic source-topic
# Allow Streams to write to the output topics:
#kafka-acls -authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:alice --operation Write --topic target-topic

# Allow Streams to manage its own internal topics and consumer groups:
#kafka-acls -authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:alice --operation All --resource-pattern-type prefixed --topic porsche-streams-app --group porsche-streams-app


confluent iam rolebinding create \
    --principal $STREAMS_PRINCIPAL \
    --role DeveloperRead \
    --kafka-cluster-id $KAFKA_CLUSTER_ID \
    --resource "Topic:source-topic"

confluent iam rolebinding create \
        --principal $STREAMS_PRINCIPAL \
        --role DeveloperWrite \
        --kafka-cluster-id $KAFKA_CLUSTER_ID \
        --resource "Topic:target-topic"

confluent iam rolebinding create \
        --principal $STREAMS_PRINCIPAL \
        --role DeveloperRead \
        --kafka-cluster-id $KAFKA_CLUSTER_ID \
        --prefix \
        --resource "Topic:porsche-streams-app"

confluent iam rolebinding create \
        --principal $STREAMS_PRINCIPAL \
        --role DeveloperWrite \
        --kafka-cluster-id $KAFKA_CLUSTER_ID \
        --prefix \
        --resource "Topic:porsche-streams-app"

confluent iam rolebinding create \
        --principal $STREAMS_PRINCIPAL \
        --role DeveloperManage \
        --kafka-cluster-id $KAFKA_CLUSTER_ID \
        --prefix \
        --resource "Topic:porsche-streams-app"


confluent iam rolebinding create \
        --principal $STREAMS_PRINCIPAL \
        --role DeveloperRead \
        --kafka-cluster-id $KAFKA_CLUSTER_ID \
        --prefix \
        --resource "Group:porsche-streams-app"

confluent iam rolebinding create \
        --principal $STREAMS_PRINCIPAL \
        --role DeveloperWrite \
        --kafka-cluster-id $KAFKA_CLUSTER_ID \
        --prefix \
        --resource "Group:porsche-streams-app"

confluent iam rolebinding create \
        --principal $STREAMS_PRINCIPAL \
        --role DeveloperManage \
        --kafka-cluster-id $KAFKA_CLUSTER_ID \
        --prefix \
        --resource "Group:porsche-streams-app"


confluent iam rolebinding list --principal $STREAMS_PRINCIPAL --kafka-cluster-id $KAFKA_CLUSTER_ID

## created roles
#Role       | ResourceType |        Name         | PatternType
#+-----------------+--------------+---------------------+-------------+
#DeveloperManage | Topic        | porsche-streams-app | PREFIXED
#DeveloperManage | Group        | porsche-streams-app | PREFIXED
#DeveloperRead   | Topic        | source-topic        | LITERAL
#DeveloperRead   | Topic        | porsche-streams-app | PREFIXED
#DeveloperRead   | Group        | porsche-streams-app | PREFIXED
#DeveloperWrite  | Topic        | target-topic        | LITERAL
#DeveloperWrite  | Topic        | porsche-streams-app | PREFIXED
#DeveloperWrite  | Group        | porsche-streams-app | PREFIXED
