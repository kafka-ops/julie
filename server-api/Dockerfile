FROM openjdk:14-alpine
COPY target/server-api-*.jar server-api.jar
EXPOSE 8080
CMD ["java", "-Dcom.sun.management.jmxremote", "-Xmx128m", "-jar", "server-api.jar"]