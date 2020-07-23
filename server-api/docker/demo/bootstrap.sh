#!/usr/bin/env bash

curl -X POST http://localhost:8080/topologies/foo
curl -X POST http://localhost:8080/topologies/foo/projects/p1

curl -H "Content-Type: application/json" -d '{"num_partitions":"1", "replication_factor":"1", "retention_ms": "10"}' \
     -X POST http://localhost:8080/topologies/foo/projects/p1/topics/t1
curl -H "Content-Type: application/json" -d '{"num_partitions":"1", "replication_factor":"1"}' \
     -X POST http://localhost:8080/topologies/foo/projects/p1/topics/t2

curl -H "Content-Type: application/json" -d '{}' \
     -X POST http://localhost:8080/topologies/foo/projects/p1/principals/consumers/bar

curl -H "Content-Type: application/json" -d '{}' \
     -X POST http://localhost:8080/topologies/foo/projects/p1/principals/producers/zet

curl -H "Content-Type: application/json" -d '{}' \
     -X POST http://localhost:8080/topologies/foo/projects/p1/principals/producers/pro

echo ""
echo ""
echo "to apply the changes"
echo "$> curl -X POST http://localhost:8080/topologies/foo/apply"