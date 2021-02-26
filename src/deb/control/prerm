#!/usr/bin/env bash

rm -rf /usr/local/julie-ops/
unlink /usr/bin/julie-ops
userdel -f julie-kafka

if grep -q -E "^julie-kafka:" /etc/group;
then
  groupdel julie-kafka
fi