#!/usr/bin/env bash

time terraform apply -var myip=$2 -var region="eu-west-1" -parallelism=20
