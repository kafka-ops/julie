# TroubleShooting JulieOps

## State management

By default, JulieOps creates a hidden file on the directory you are running julie-ops from by the name `.cluster-state`,
this file captures the state of all actions performed JulieOps.

*Note*: This is the necessary state for JulieOps to operate, something like the state in terraform.
The state could be managed as well using other mechanisms like a kafka topic, an S3 bucket, etc.
Look what your system has configured
