FROM alpine:3.14
MAINTAINER pere.urbon@gmail.com
ENV container docker
ENV BUILDER_HOME /usr/local/julie-ops
ENV PATH="${BUILDER_HOME}:${PATH}"


USER root

RUN apk add bash openjdk11 krb5-server krb5-libs krb5-conf krb5

RUN mkdir -p /usr/local/julie-ops/bin && chmod 755 /usr/local/julie-ops
COPY julie-ops.jar /usr/local/julie-ops/bin
COPY julie-ops-cli.sh /usr/local/julie-ops

CMD ["julie-ops-cli.sh"]
