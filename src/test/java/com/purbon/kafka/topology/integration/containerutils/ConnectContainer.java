package com.purbon.kafka.topology.integration.containerutils;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

public class ConnectContainer extends GenericContainer<ConnectContainer> {

  private static final DockerImageName DEFAULT_IMAGE =
      DockerImageName.parse("confluentinc/cp-kafka-connect").withTag("7.6.0");

  private static int CONNECT_PORT = 8083;
  private static int CONNECT_SSL_PORT = 8084;

  public ConnectContainer(AlternativeKafkaContainer kafka, String truststore, String keystore) {
    this(DEFAULT_IMAGE, kafka, truststore, keystore);
  }

  public ConnectContainer(
      final DockerImageName dockerImageName,
      AlternativeKafkaContainer kafka,
      String truststore,
      String keystore) {
    super(dockerImageName);

    String kafkaHost = kafka.getNetworkAliases().get(1);
    withExposedPorts(CONNECT_PORT, CONNECT_SSL_PORT);

    withEnv("CONNECT_BOOTSTRAP_SERVERS", "SASL_PLAINTEXT://" + kafkaHost + ":" + 9091);
    withEnv("CONNECT_REST_PORT", String.valueOf(CONNECT_SSL_PORT));
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
    withEnv("CONNECT_PLUGIN_PATH", "/usr/share/java,/usr/share/filestream-connectors");
    withEnv(
        "CONNECT_LISTENERS",
        "http://0.0.0.0:" + CONNECT_PORT + ", https://0.0.0.0:" + CONNECT_SSL_PORT);

    withEnv("CONNECT_SASL_JAAS_CONFIG", saslConfig());
    withEnv("CONNECT_SASL_MECHANISM", "PLAIN");
    withEnv("CONNECT_SECURITY_PROTOCOL", "SASL_PLAINTEXT");

    withEnv("CONNECT_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", "HTTPS");
    withEnv(
        "CONNECT_LISTENERS_HTTPS_SSL_TRUSTSTORE_LOCATION",
        "/etc/kafka-connect/secrets/server.truststore");
    withEnv("CONNECT_LISTENERS_HTTPS_SSL_TRUSTSTORE_PASSWORD", "ksqldb");
    withEnv(
        "CONNECT_LISTENERS_HTTPS_SSL_KEYSTORE_LOCATION",
        "/etc/kafka-connect/secrets/server.keystore");
    withEnv("CONNECT_LISTENERS_HTTPS_SSL_KEYSTORE_PASSWORD", "ksqldb");
    withEnv("CONNECT_LISTENERS_HTTPS_SSL_KEY_PASSWORD", "ksqldb");

    this.withClasspathResourceMapping(
            truststore, "/etc/kafka-connect/secrets/server.truststore", BindMode.READ_ONLY)
        .withClasspathResourceMapping(
            keystore, "/etc/kafka-connect/secrets/server.keystore", BindMode.READ_ONLY);

    withNetworkAliases("connect");
    withNetwork(kafka.getNetwork());

    waitingFor((new HttpWaitStrategy()).forPath("/").forPort(CONNECT_PORT));
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

  public String getHttpUrl() {
    return "http://" + getHost() + ":" + getMappedPort(CONNECT_PORT);
  }

  public String getHttpsUrl() {
    return "https://" + getHost() + ":" + getMappedPort(CONNECT_SSL_PORT);
  }
}
