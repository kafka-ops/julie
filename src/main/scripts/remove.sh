#!/usr/bin/env bash

rm -rf /usr/local/kafka-topology-builder/
unlink /usr/bin/kafka-topology-builder
userdel -f ktb-kafka

if grep -q -E "^ktb-kafka:" /etc/group;
then
  groupdel ktb-kafka
fi