package com.purbon.kafka.topology.integration.containerutils;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class ConnectContainer extends GenericContainer<ConnectContainer> {

  private static final DockerImageName DEFAULT_IMAGE =
      DockerImageName.parse("confluentinc/cp-kafka-connect").withTag("6.0.2");

  private static int CONNECT_PORT = 8083;

  public ConnectContainer(AlternativeKafkaContainer kafka) {
    this(DEFAULT_IMAGE, kafka);
  }

  public ConnectContainer(final DockerImageName dockerImageName, AlternativeKafkaContainer kafka) {
    super(dockerImageName);

    String kafkaHost = kafka.getNetworkAliases().get(1);
    withExposedPorts(CONNECT_PORT);

    withEnv("CONNECT_BOOTSTRAP_SERVERS", "SASL_PLAINTEXT://" + kafkaHost + ":" + 9091);
    withEnv("CONNECT_REST_PORT", String.valueOf(CONNECT_PORT));
    withEnv("CONNECT_GROUP_ID", "kc");
    withEnv("CONNECT_CONFIG_STORAGE_TOPIC", "docker-kafka-connect-cp-configs");
    withEnv("CONNECT_OFFSET_STORAGE_TOPIC", "docker-kafka-connect-cp-offsets");
    withEnv("CONNECT_STATUS_STORAGE_TOPIC", "docker-kafka-connect-cp-status");
    withEnv("CONNECT_KEY_CONVERTER", "org.apache.kafka.connect.json.JsonConverter");
    withEnv("CONNECT_VALUE_CONVERTER", "org.apache.kafka.connect.json.JsonConverter");
    withEnv("CONNECT_REST_ADVERTISED_HOST_NAME", "connect");
    withEnv("CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR", "1");
    withEnv("CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR", "1");
    withEnv("CONNECT_STATUS_STORAGE_REPLICATION_FACTOR", "1");
    withEnv("CONNECT_PLUGIN_PATH", "/usr/share/java");
    withEnv("CONNECT_LISTENERS", "http://0.0.0.0:" + CONNECT_PORT);

    withEnv("CONNECT_SASL_JAAS_CONFIG", saslConfig());
    withEnv("CONNECT_SASL_MECHANISM", "PLAIN");
    withEnv("CONNECT_SECURITY_PROTOCOL", "SASL_PLAINTEXT");
    withNetworkAliases("connect");
    withNetwork(kafka.getNetwork());

    waitingFor(Wait.forHttp("/"));
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
    return "http://" + getHost() + ":" + getMappedPort(CONNECT_PORT);
  }
}
