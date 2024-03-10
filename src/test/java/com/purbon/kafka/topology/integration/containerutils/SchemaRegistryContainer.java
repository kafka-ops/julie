package com.purbon.kafka.topology.integration.containerutils;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class SchemaRegistryContainer extends GenericContainer<SchemaRegistryContainer> {

  private static final DockerImageName DEFAULT_IMAGE =
      DockerImageName.parse("confluentinc/cp-schema-registry").withTag("7.6.0");

  public static final int SR_PORT = 8081;

  public SchemaRegistryContainer(AlternativeKafkaContainer kafka) {
    this(DEFAULT_IMAGE, kafka);
  }

  public SchemaRegistryContainer(
      final DockerImageName dockerImageName, AlternativeKafkaContainer kafka) {
    super(dockerImageName);
    String kafkaHost = kafka.getNetworkAliases().get(1);
    withExposedPorts(SR_PORT);
    withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry");
    withEnv("SCHEMA_REGISTRY_KAFKASTORE_SECURITY_PROTOCOL", "SASL_PLAINTEXT");
    withEnv(
        "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
        "SASL_PLAINTEXT://" + kafkaHost + ":" + 9091);
    withEnv("SCHEMA_REGISTRY_KAFKASTORE_SASL_JAAS_CONFIG", saslConfig());
    withEnv("SCHEMA_REGISTRY_KAFKASTORE_SASL_MECHANISM", "PLAIN");
    withNetwork(kafka.getNetwork());
  }

  private String saslConfig() {
    StringBuilder sb = new StringBuilder();
    sb.append("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"");
    sb.append("kafka");
    sb.append("\" password=\"");
    sb.append("kafka");
    sb.append("\";");
    return sb.toString();
  }

  public String getUrl() {
    return "http://" + getHost() + ":" + getMappedPort(8081);
  }
}
