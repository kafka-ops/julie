#!/usr/bin/env bash

FILE=$1
IDENTITY=${2:-"393416696134E892"}

export GPG_TTY=$(tty)
dpkg-sig -k $IDENTITY --sign builder $FILE
