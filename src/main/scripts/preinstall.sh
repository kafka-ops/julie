#!/usr/bin/env bash

if id "julie-kafka" &>/dev/null;
then
  echo "User julie-kafka already exist"
else
   adduser -M -r julie-kafka
fi
