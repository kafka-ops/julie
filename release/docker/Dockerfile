FROM centos
MAINTAINER pere.urbon@gmail.com
ENV container docker
ENV BUILDER_HOME /usr/local/kafka-topology-builder
ENV PATH="${BUILDER_HOME}:${PATH}"

USER root

RUN yum install -y java-1.8.0-openjdk

RUN mkdir -p /usr/local/kafka-topology-builder/bin
COPY kafka-topology-builder.jar /usr/local/kafka-topology-builder/bin
COPY kafka-topology-builder.sh /usr/local/kafka-topology-builder

RUN chmod 755 /usr/local/kafka-topology-builder

CMD ["kafka-topology-builder.sh"]
