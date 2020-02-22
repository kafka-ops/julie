FROM centos
MAINTAINER d.gasparina@gmail.com
ENV container docker

# 1. Adding Confluent repository
RUN rpm --import https://packages.confluent.io/rpm/5.3/archive.key
COPY confluent.repo /etc/yum.repos.d/confluent.repo
RUN yum clean all

# 2. Install zookeeper and kafka
RUN yum install -y java-1.8.0-openjdk
RUN yum install -y confluent-kafka-2.12

# 3. Configure zookeeper
COPY zookeeper.properties /etc/kafka/zookeeper.properties

EXPOSE 2181

CMD zookeeper-server-start /etc/kafka/zookeeper.properties 
