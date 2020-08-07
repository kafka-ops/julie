#!/usr/bin/env bash

KEY_FILE=${1:-"private.key"}
KEY_ID=${2:-"6134E892"}
RPM_KEY_FILE=${3:-"rpm-gen-key"}

## Import private key in gpg
gpg --import $KEY_FILE
gpg --edit-key $KEY_ID trust quit

## import key into rpm
sudo rpm --import $RPM_KEY_FILE
touch .rpmmacros