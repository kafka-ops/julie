#!/usr/bin/env bash
## julie-ops-cli
## author: pere.urbon@gmail.com

JULIE_OPS_HOME=/usr/local/julie-ops
JULIE_OPS_JAR=$JULIE_OPS_HOME/bin/julie-ops-cli.jar
JAVA_PATH=java

if [ -z "$JULIE_OPS_OPTIONS" ]; then
  JULIE_OPS_OPTIONS=""
fi

$JAVA_PATH $JULIE_OPS_OPTIONS -jar $JULIE_OPS_JAR "$@"
