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
team: "foo"
source: "db"
projects:
- name: "data-lake"
  users:
    consumers:
      - "app0"
      - "app1"
    producers:
      - "app0"
      - "app3"
    streams:
    - name: "app2"
      topics:
        read:
        - "foo"
        write:
        - "bar"
    connectors:
      - "conn1"
  topics:
  - name: "foo" # generated name: projects.data-lake.foo
    config:
      partitions: 1
      retention.ms: 100
  - name: "bar" # generated name: projects.data-lake.bar
    config:
      partitions: 1
  zookeepers:
    - "zk1:2181"
- name: "monitoring"
  users:
    consumers:
    - "app2"
    producers: []
    streams: []
    connectors:
    - "conn1"
  topics: []
```