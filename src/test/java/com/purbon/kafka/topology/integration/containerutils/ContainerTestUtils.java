package com.purbon.kafka.topology.integration.containerutils;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.JulieOps;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.roles.SimpleAclsProvider;
import com.purbon.kafka.topology.roles.acls.AclsBindingsBuilder;
import com.purbon.kafka.topology.utils.TestUtils;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;

public final class ContainerTestUtils {

  static final String DEFAULT_CP_KAFKA_VERSION = "6.1.0";

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

  public static Map<String, Object> getSaslConfig(
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

  public static void populateAcls(
      final SaslPlaintextKafkaContainer container,
      final String topologyResource,
      final String configResource) {
    TestUtils.deleteStateFile();
    try (final AdminClient kafkaAdminClient = getSaslAdminClient(container)) {
      final JulieOps julieOps =
          getKafkaTopologyBuilder(kafkaAdminClient, topologyResource, configResource);
      try {
        julieOps.run();
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    } finally {
      /* Clean up static stuff buried deep below TopologyBuilderConfig.
       * I don't like this "hack", but I didn't immediately see
       * how to do it correctly. */
      System.clearProperty("config.file");
      ConfigFactory.invalidateCaches();
    }
  }

  private static JulieOps getKafkaTopologyBuilder(
      final AdminClient kafkaAdminClient,
      final String topologyResource,
      final String configResource) {
    final String fileOrDirPath = TestUtils.getResourceFilename(topologyResource);
    final Map<String, String> cliParams = new HashMap<>();
    final Configuration builderConfig =
        Configuration.build(cliParams, TestUtils.getResourceFilename(configResource));
    final TopologyBuilderAdminClient topologyAdminClient =
        new TopologyBuilderAdminClient(kafkaAdminClient);
    final AccessControlProvider accessControlProvider = new SimpleAclsProvider(topologyAdminClient);
    final BindingsBuilderProvider bindingsBuilderProvider = new AclsBindingsBuilder(builderConfig);
    try {
      return JulieOps.build(
          fileOrDirPath,
          builderConfig,
          topologyAdminClient,
          accessControlProvider,
          bindingsBuilderProvider);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
