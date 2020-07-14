#!/usr/bin/env bash
mvn clean package
mvn install:install-file -Dfile=target/kafka-topology-builder.jar \
                         -DgroupId=com.purbon.kafka \
                         -DartifactId=kafka-topology-builder \
                         -Dversion=1.0.0-rc.1 \
                         -Dpackaging=jar
