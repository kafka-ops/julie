package com.purbon.kafka.topology.integration.containerutils;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;

public final class ContainerTestUtils {

  private ContainerTestUtils() {}

  public static AdminClient getSaslAdminClient(final AlternativeKafkaContainer container) {
    return getSaslAdminClient(container.getBootstrapServers());
  }

  public static AdminClient getSaslAdminClient(final String boostrapServers) {
    return AdminClient.create(
        getSaslConfig(
            boostrapServers,
            SaslPlaintextKafkaContainer.DEFAULT_SUPER_USERNAME,
            SaslPlaintextKafkaContainer.DEFAULT_SUPER_PASSWORD));
  }

  private static Map<String, Object> getSaslConfig(
      final String bootstrapServers, final String username, final String password) {
    final Map<String, Object> map = getBaseConfig(bootstrapServers);
    map.put("security.protocol", "SASL_PLAINTEXT");
    map.put("sasl.mechanism", "PLAIN");
    map.put(
        "sasl.jaas.config",
        "org.apache.kafka.common.security.plain.PlainLoginModule required username="
            + escape(username)
            + " password="
            + escape(password)
            + ";");
    return map;
  }

  private static Map<String, Object> getBaseConfig(final String bootstrapServers) {
    final Map<String, Object> map = new HashMap<>();
    map.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    return map;
  }

  private static String escape(final String s) {
    return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }

}
