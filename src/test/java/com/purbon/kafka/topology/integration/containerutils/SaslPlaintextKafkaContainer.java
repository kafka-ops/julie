package com.purbon.kafka.topology.integration.containerutils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

public final class SaslPlaintextKafkaContainer extends AlternativeKafkaContainer {

  private static final DockerImageName DEFAULT_IMAGE =
      DockerImageName.parse("confluentinc/cp-kafka").withTag("5.5.0");
  public static final String DEFAULT_SUPER_USERNAME = "kafka";
  public static final String DEFAULT_SUPER_PASSWORD = "kafka";
  private static final String JAAS_CONFIG_FILE = "/tmp/broker_jaas.conf";
  private String superUsername = DEFAULT_SUPER_USERNAME;
  private String superPassword = DEFAULT_SUPER_PASSWORD;
  private final Map<String, String> usernamesAndPasswords = new HashMap<>();

  public SaslPlaintextKafkaContainer() {
    this(DEFAULT_IMAGE);
  }

  public SaslPlaintextKafkaContainer(final DockerImageName dockerImageName) {
    super(dockerImageName);
    /* Note difference between 0.0.0.0 and localhost: The former will be replaced by the container IP. */
    withEnv(
        "KAFKA_LISTENERS",
        "SASL_PLAINTEXT://0.0.0.0:"
            + KAFKA_PORT
            + ","
            + INTERNAL_LISTENER_NAME
            + "://127.0.0.1:"
            + KAFKA_INTERNAL_PORT);
    withEnv(
        "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP",
        "SASL_PLAINTEXT:SASL_PLAINTEXT," + INTERNAL_LISTENER_NAME + ":SASL_PLAINTEXT");
    withEnv("KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL", "PLAIN");
    withEnv("KAFKA_SASL_ENABLED_MECHANISMS", "PLAIN");
    withEnv("KAFKA_OPTS", "-Djava.security.auth.login.config=" + JAAS_CONFIG_FILE);
    withSuperUser(superUsername, superPassword);
    withUser("alice", "alice-secret");
    withUser("bob", "bob-secret");
    withAclAuthorizer();
  }

  public SaslPlaintextKafkaContainer withSuperUser(final String username, final String password) {
    this.superUsername = assertValidUsernameAndPassword(username);
    this.superPassword = assertValidUsernameAndPassword(password);
    withEnv("KAFKA_SUPER_USERS", "User:" + username);
    return this;
  }

  public SaslPlaintextKafkaContainer withUser(final String usernameAndPassword) {
    return withUser(usernameAndPassword, usernameAndPassword);
  }

  public SaslPlaintextKafkaContainer withUser(final String username, final String password) {
    usernamesAndPasswords.put(
        assertValidUsernameAndPassword(username), assertValidUsernameAndPassword(password));
    return this;
  }

  public SaslPlaintextKafkaContainer withAclAuthorizer() {
    final DockerImageName imageName = DockerImageName.parse(getDockerImageName());
    final boolean old =
        imageName.getUnversionedPart().contains("confluentinc/")
            && imageName.getVersionPart().compareTo("6") <= 0;
    withEnv(
        "KAFKA_AUTHORIZER_CLASS_NAME",
        old
            ? "kafka.security.auth.SimpleAclAuthorizer"
            : "kafka.security.authorizer.AclAuthorizer");
    return this;
  }

  @Override
  protected void beforeStartupPreparations() {
    withEnv("KAFKA_LISTENER_NAME_SASL_PLAINTEXT_PLAIN_SASL_JAAS_CONFIG", createJaasLoginLine());
    uploadJaasConfig();
  }

  private void uploadJaasConfig() {
    final String jaas = "KafkaServer { " + createJaasLoginLine() + " };\n";
    copyFileToContainer(
        Transferable.of(jaas.getBytes(StandardCharsets.UTF_8), 0644), JAAS_CONFIG_FILE);
  }

  private String createJaasLoginLine() {
    /* Precondition: No usernames or passwords contain characters that need special handling for JAAS config. */
    final StringBuilder sb = new StringBuilder();
    sb.append("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"");
    sb.append(superUsername);
    sb.append("\" password=\"");
    sb.append(superPassword);
    sb.append("\" user_");
    sb.append(superUsername);
    sb.append("=\"");
    sb.append(superPassword);
    sb.append("\"");
    for (final Map.Entry<String, String> entry : usernamesAndPasswords.entrySet()) {
      sb.append(" user_");
      sb.append(entry.getKey());
      sb.append("=\"");
      sb.append(entry.getValue());
      sb.append("\"");
    }
    sb.append(";");
    return sb.toString();
  }

  private static String assertValidUsernameAndPassword(final String s) {
    /* Enforcing, in order to not have to deal with escaping for the JAAS config. */
    for (final char c : s.toCharArray()) {
      if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-')) {
        throw new RuntimeException(
            "Only letters, digits and hyphens allowed in usernames and passwords.");
      }
    }
    return s;
  }
}
