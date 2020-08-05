#!/usr/bin/env bash

RPM_FILE=$1
IDENTITY=${2:-"pere.urbon+kafka@gmail.com"}

rpm --define "_gpg_name <$IDENTITY>" --addsign $RPM_FILE
rpm --checksig $RPM_FILE