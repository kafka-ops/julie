FROM debian:jessie

EXPOSE 749 88/udp

ENV DEBIAN_FRONTEND noninteractive
RUN apt-get -qq update
RUN apt-get -qq install locales krb5-kdc krb5-admin-server
RUN apt-get -qq install vim
RUN apt-get -qq clean

ENV REALM ${REALM:-TEST.CONFLUENT.IO}
ENV SUPPORTED_ENCRYPTION_TYPES ${SUPPORTED_ENCRYPTION_TYPES:-aes256-cts-hmac-sha1-96:normal}
ENV KADMIN_PRINCIPAL ${KADMIN_PRINCIPAL:-kadmin/admin}
ENV KADMIN_PASSWORD ${KADMIN_PASSWORD:-adminpassword}

RUN mkdir /etc/kdc && touch /etc/krb5.conf && ln -s /etc/krb5.conf /etc/kdc/krb5.conf


COPY init-script.sh /opt/
CMD /opt/init-script.sh