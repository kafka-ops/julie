#!/usr/bin/env bash

FILE=$1
IDENTITY=${2:-"393416696134E892"}

dpkg-sig -k $IDENTITY --sign builder $FILE
