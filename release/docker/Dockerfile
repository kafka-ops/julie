FROM azul/zulu-openjdk-alpine:11.0.12 as packager

RUN { \
        java --version ; \
        echo "jlink version:" && \
        /usr/lib/jvm/zulu11/bin/jlink --version ; \
    }

ENV JAVA_MINIMAL=/opt/jre

# build modules distribution
# All modules available
RUN  /usr/lib/jvm/zulu11/bin/jlink \
    --verbose \
    --module-path /usr/lib/jvm/zulu11/jmods/ \
    --add-modules \
        java.net.http,java.base,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,java.security.sasl,jdk.security.auth\
        # java.naming - javax/naming/NamingException
        # java.desktop - java/beans/PropertyEditorSupport
        # java.management - javax/management/MBeanServer
        # java.security.jgss - org/ietf/jgss/GSSException
        # java.instrument - java/lang/instrument/IllegalClassFormatException
        # java.net.http - general http client for java 11
        # java.security.sasl - SASL mechanisms including DIGEST-MD5, CRAM-MD5, and NTLM
        # jdk.security.auth - Implementations for common security authentication modules
    --compress 2 \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --output "$JAVA_MINIMAL"

# Second stage, add only our minimal "JRE" distr and our app
FROM alpine

MAINTAINER pere.urbon@gmail.com
ENV container docker
ENV BUILDER_HOME /usr/local/julie-ops
ENV PATH="${BUILDER_HOME}:${PATH}"

ENV JAVA_MINIMAL=/opt/jre
ENV PATH="$PATH:$JAVA_MINIMAL/bin"

USER root

RUN apk add bash krb5-server krb5-libs krb5-conf

COPY --from=packager "$JAVA_MINIMAL" "$JAVA_MINIMAL"

RUN mkdir -p /usr/local/julie-ops/bin && chmod 755 /usr/local/julie-ops
COPY julie-ops.jar /usr/local/julie-ops/bin
COPY julie-ops-cli.sh /usr/local/julie-ops

CMD ["julie-ops-cli.sh"]
