# The Kafka Topology builder tool

<a href="https://codeclimate.com/github/purbon/kafka-topology-builder/maintainability"><img src="https://api.codeclimate.com/v1/badges/ef4bcda7d1b5fd0a4f1e/maintainability" /></a>

[![Build Status](https://travis-ci.org/purbon/kafka-topology-builder.svg?branch=master)](https://travis-ci.org/purbon/kafka-topology-builder)


This tool helps you build proper ACLs for Apache Kafka. The Kafka ACL builder tool knows what do you
need for each of the products/projects you are planning, either Kafka Connect, Kafka Streams or others.

## Process 

One of the typical questions while building an Apache Kafka infrastructure is how to handle topics, configurations and the required
permissions to use them (Access Control List).

### Automating the Topic Management with Jenkins (CI/CD)

![KafkaTopologyBuilder](imgs/kafka-topology-builder.png)


## An example topology

```yaml 
---
team: "team"
source: "source"
projects:
- name: "foo"
  zookeepers: []
  consumers:
  - principal: "User:app0"
  - principal: "User:app1"
  producers: []
  streams:
  - principal: "User:App0"
    topics:
      read:
      - "topicA"
      - "topicB"
      write:
      - "topicC"
      - "topicD"
  connectors:
  - principal: "User:Connect1"
    topics:
      read:
      - "topicA"
      - "topicB"
  - principal: "User:Connect2"
    topics:
      write:
      - "topicC"
      - "topicD"
  topics:
  - name: "foo" # topicName: team.source.foo.foo
    config:
      replication.factor: "2"
      num.partitions: "3"
  - name: "bar" # topicName: team.source.foo.bar
    config:
      replication.factor: "2"
      num.partitions: "3"
- name: "bar"
  zookeepers: []
  consumers: []
  producers: []
  streams: []
  connectors: []
  topics:
  - name: "bar" # topicName: team.source.bar.bar
    config:
      replication.factor: "2"
      num.partitions: "3"
```