FROM centos
MAINTAINER seknop@gmail.com
ENV container docker

# 1. Adding Confluent repository
RUN rpm --import https://packages.confluent.io/rpm/5.3/archive.key
COPY confluent.repo /etc/yum.repos.d/confluent.repo
RUN yum clean all

# 2. Install zookeeper and kafka
RUN yum install -y java-1.8.0-openjdk
RUN yum install -y confluent-kafka-2.12
RUN yum install -y confluent-security


# 3. Configure Kafka and zookeeper for Kerberos
COPY server.properties /etc/kafka/server.properties


EXPOSE 9093

CMD kafka-server-start /etc/kafka/server.properties
