%define _unpackaged_files_terminate_build 0
%define __jar_repack 0
Name: kafka-topology-builder
Version: 1.2.2
Release: 1
Summary: A Kafka topology builder tool
License: MIT (c) 2019, Pere Urbon
Distribution: Trash 2019
Vendor: Pere Urbon
URL: https://github.com/purbon/
Group: Application/Collectors
Packager: Pere Urbon
autoprov: yes
autoreq: yes
Prefix: /usr/local
BuildArch: noarch
#BuildRoot: /tmp/kafka-topology-builder/target/rpm/kafka-topology-builder/buildroot
BuildRoot: %{_tmppath}/%{name}-%{version}-build

%description
A helper project for Kafka Platform teams to build an automated Topic, Configuration (and Schemas in the future)
    Management solution.

%install

#if [ -d $RPM_BUILD_ROOT ];
#then
#  mv /tmp/kafka-topology-builder/target/rpm/kafka-topology-builder/tmp-buildroot/* $RPM_BUILD_ROOT
#else
#  mv /tmp/kafka-topology-builder/target/rpm/kafka-topology-builder/tmp-buildroot $RPM_BUILD_ROOT
#fi

%files

%attr(440,ktb-kafka,ktb-kafka)  "/usr/local/kafka-topology-builder/bin/kafka-topology-builder.jar"
%attr(775,ktb-kafka,ktb-kafka)  "/usr/local/kafka-topology-builder/bin/kafka-topology-builder.sh"
%config %attr(640,ktb-kafka,ktb-kafka) "/usr/local/kafka-topology-builder/conf"

%pre
#!/usr/bin/env bash

adduser -M -r ktb-kafka

%post
#!/usr/bin/env bash

ln -s /usr/local/kafka-topology-builder/bin/kafka-topology-builder.sh /usr/bin/kafka-topology-builder
chown ktb-kafka.ktb-kafka /usr/local/kafka-topology-builder/bin/kafka-topology-builder.sh

%postun
#!/usr/bin/env bash

rm -rf /usr/local/kafka-topology-builder/
unlink /usr/bin/kafka-topology-builder
userdel -f ktb-kafka