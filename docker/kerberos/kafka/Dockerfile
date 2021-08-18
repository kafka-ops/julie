FROM centos:centos8
MAINTAINER d.gasparina@gmail.com
ENV container docker

# 1. Adding Confluent repository
RUN rpm --import https://packages.confluent.io/rpm/5.4/archive.key
COPY confluent.repo /etc/yum.repos.d/confluent.repo
RUN yum clean all

# 2. Install zookeeper and kafka
RUN yum install -y java-11-openjdk
RUN yum install -y confluent-kafka-2.12
RUN yum install -y confluent-control-center

# 3. Configure Kafka for Kerberos
RUN yum install -y krb5-workstation krb5-libs
COPY server.properties /etc/kafka/server.properties
COPY command.properties /etc/kafka/command.properties
COPY kafka.sasl.jaas.config /etc/kafka/kafka_server_jaas.conf

EXPOSE 9093

ENV KAFKA_OPTS="-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf -Dzookeeper.sasl.client.username=zkservice"

CMD kafka-server-start /etc/kafka/server.properties