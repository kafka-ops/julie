FROM openjdk:14-alpine
COPY target/TopologyBuilder-*.jar TopologyBuilder.jar
EXPOSE 8080
CMD ["java", "-Dcom.sun.management.jmxremote", "-Xmx128m", "-jar", "TopologyBuilder.jar"]