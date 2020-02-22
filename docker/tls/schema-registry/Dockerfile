FROM centos
MAINTAINER d.gasparina@gmail.com
ENV container docker

# 1. Adding Confluent repository
RUN rpm --import https://packages.confluent.io/rpm/5.0/archive.key
COPY confluent.repo /etc/yum.repos.d/confluent.repo
RUN yum clean all

# 2. Install zookeeper and kafka
RUN yum install -y java-1.8.0-openjdk
RUN yum install -y confluent-schema-registry confluent-security

# 3. Configure Kafka 
COPY schema-registry.properties /etc/schema-registry/schema-registry.properties

EXPOSE 8443

CMD schema-registry-start /etc/schema-registry/schema-registry.properties
