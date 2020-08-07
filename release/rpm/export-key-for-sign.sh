#!/usr/bin/env bash

KEY_ID=${1:-"4AD63D67683A97D149247EC3393416696134E892"}
KEY_NAME=${2:-"pere.urbon+kafka@gmail.com"}

gpg --export-secret-keys $KEY_ID > keys/private.key
gpg -a --export $KEY_NAME > keys/rpm-gen-key