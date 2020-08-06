#!/usr/bin/env bash

sudo apt -y update
sudo apt -y install maven debsigs dpkg-sig
sudo apt-get -y install openjdk-11-jdk