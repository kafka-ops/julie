#!/usr/bin/env bash
mvn clean package
mvn install:install-file -Dfile=target/julie-ops.jar \
                         -DgroupId=com.purbon.kafka \
                         -DartifactId=julie-ops \
                         -Dversion=1.0.0-rc.1 \
                         -Dpackaging=jar
