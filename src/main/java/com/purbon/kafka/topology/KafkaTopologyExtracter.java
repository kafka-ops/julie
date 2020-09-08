package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.TopologyBuilderConfig.REDIS_HOST_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.REDIS_PORT_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.REDIS_STATE_PROCESSOR_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.STATE_PROCESSOR_DEFAULT_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.STATE_PROCESSOR_IMPLEMENTATION_CLASS;

import com.purbon.kafka.topology.clusterstate.FileSateProcessor;
import com.purbon.kafka.topology.clusterstate.RedisSateProcessor;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class KafkaTopologyExtracter {

  public static final String SCHEMA_REGISTRY_URL = "confluent.schema.registry.url";

  private final String topologyFile;
  private final TopologySerdes parser;
  private final TopologyBuilderAdminClient adminClient;
  private final TopologyBuilderConfig config;
  private final AccessControlProvider accessControlProvider;

  public KafkaTopologyExtracter(
      String topologyFile,
      TopologyBuilderConfig config,
      TopologyBuilderAdminClient adminClient,
      AccessControlProvider accessControlProvider) {
    this(topologyFile, new TopologySerdes(), config, adminClient, accessControlProvider);
  }

  public KafkaTopologyExtracter(
      String topologyFile,
      TopologySerdes parser,
      TopologyBuilderConfig config,
      TopologyBuilderAdminClient adminClient,
      AccessControlProvider accessControlProvider) {
    this.topologyFile = topologyFile;
    this.parser = parser;
    this.config = config;
    this.adminClient = adminClient;
    this.accessControlProvider = accessControlProvider;
  }

  public void extract() throws IOException {
    ClusterState cs = buildStateProcessor();

    AccessControlManager accessControlManager =
        new AccessControlManager(accessControlProvider, cs, config.params());
    accessControlManager.extract(topologyFile, parser);

    String schemaRegistryUrl = (String) config.getOrDefault(SCHEMA_REGISTRY_URL, "http://foo:8082");
    SchemaRegistryClient schemaRegistryClient =
        new CachedSchemaRegistryClient(schemaRegistryUrl, 10);
    SchemaRegistryManager schemaRegistryManager = new SchemaRegistryManager(schemaRegistryClient);

    TopicManager topicManager = new TopicManager(adminClient, schemaRegistryManager, config);

    if (!config.isQuite()) {
      topicManager.printCurrentState(System.out);
      accessControlManager.printCurrentState(System.out);
    }
  }

  public void close() {
    adminClient.close();
  }

  public static String getVersion() {
    InputStream resourceAsStream =
        KafkaTopologyExtracter.class.getResourceAsStream(
            "/META-INF/maven/com.purbon.kafka/kafka-topology-builder/pom.properties");
    Properties prop = new Properties();
    try {
      prop.load(resourceAsStream);
      return prop.getProperty("version");
    } catch (IOException e) {
      e.printStackTrace();
      return "unkown";
    }
  }

  private ClusterState buildStateProcessor() throws IOException {

    String stateProcessorClass =
        config
            .getOrDefault(STATE_PROCESSOR_IMPLEMENTATION_CLASS, STATE_PROCESSOR_DEFAULT_CLASS)
            .toString();

    try {
      if (stateProcessorClass.equalsIgnoreCase(STATE_PROCESSOR_DEFAULT_CLASS)) {
        return new ClusterState(new FileSateProcessor());
      } else if (stateProcessorClass.equalsIgnoreCase(REDIS_STATE_PROCESSOR_CLASS)) {
        String host = config.getProperty(REDIS_HOST_CONFIG);
        int port = Integer.valueOf(config.getProperty(REDIS_PORT_CONFIG));
        return new ClusterState(new RedisSateProcessor(host, port));
      } else {
        throw new IOException(stateProcessorClass + " Unknown state processor provided.");
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }
}
