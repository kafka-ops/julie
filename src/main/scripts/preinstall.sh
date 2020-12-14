#!/usr/bin/env bash

if id "ktb-kafka" &>/dev/null;
then
  echo "User ktb-kafka already exist"
else
   adduser -M -r ktb-kafka
fi
