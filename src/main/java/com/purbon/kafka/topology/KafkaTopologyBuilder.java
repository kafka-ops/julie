package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.TopologyBuilderConfig.REDIS_HOST_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.REDIS_PORT_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.REDIS_STATE_PROCESSOR_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.STATE_PROCESSOR_DEFAULT_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.STATE_PROCESSOR_IMPLEMENTATION_CLASS;

import com.purbon.kafka.topology.api.mds.MDSApiClientBuilder;
import com.purbon.kafka.topology.clusterstate.FileStateProcessor;
import com.purbon.kafka.topology.clusterstate.RedisStateProcessor;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

public class KafkaTopologyBuilder implements AutoCloseable {

  public static final String SCHEMA_REGISTRY_URL = "confluent.schema.registry.url";

  private TopicManager topicManager;
  private AccessControlManager accessControlManager;
  private Topology topology;
  private TopologyBuilderConfig config;

  private KafkaTopologyBuilder(
      Topology topology,
      TopologyBuilderConfig config,
      TopicManager topicManager,
      AccessControlManager accessControlManager) {
    this.topology = topology;
    this.config = config;
    this.topicManager = topicManager;
    this.accessControlManager = accessControlManager;
  }

  public static KafkaTopologyBuilder build(
      String topologyFile, Map<String, String> config, Properties adminProperties)
      throws IOException {

    TopologyBuilderConfig builderConfig = new TopologyBuilderConfig(config, adminProperties);
    TopologyBuilderAdminClient adminClient =
        new TopologyBuilderAdminClientBuilder(builderConfig).build();
    AccessControlProviderFactory accessControlProviderFactory =
        new AccessControlProviderFactory(
            builderConfig, adminClient, new MDSApiClientBuilder(builderConfig));

    KafkaTopologyBuilder builder =
        build(topologyFile, builderConfig, adminClient, accessControlProviderFactory.get());
    builder.verifyRequiredParameters(topologyFile, config);
    return builder;
  }

  public static KafkaTopologyBuilder build(
      String topologyFileOrDir,
      TopologyBuilderConfig config,
      TopologyBuilderAdminClient adminClient,
      AccessControlProvider accessControlProvider)
      throws IOException {

    Topology topology = TopologyDescriptorBuilder.build(topologyFileOrDir);
    config.validateWith(topology);

    ClusterState cs = buildStateProcessor(config);

    AccessControlManager accessControlManager =
        new AccessControlManager(accessControlProvider, cs, config.params());

    String schemaRegistryUrl = (String) config.getOrDefault(SCHEMA_REGISTRY_URL, "http://foo:8082");
    SchemaRegistryClient schemaRegistryClient =
        new CachedSchemaRegistryClient(schemaRegistryUrl, 10);
    SchemaRegistryManager schemaRegistryManager =
        new SchemaRegistryManager(schemaRegistryClient, topologyFileOrDir);

    TopicManager topicManager = new TopicManager(adminClient, schemaRegistryManager, config);

    return new KafkaTopologyBuilder(topology, config, topicManager, accessControlManager);
  }

  public void verifyRequiredParameters(String topologyFile, Map<String, String> config)
      throws IOException {
    if (!Files.exists(Paths.get(topologyFile))) {
      throw new IOException("Topology file does not exist");
    }

    String configFilePath = config.get(BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION);

    if (!Files.exists(Paths.get(configFilePath))) {
      throw new IOException("AdminClient config file does not exist");
    }
  }

  public void run() throws IOException {

    topicManager.sync(topology);
    accessControlManager.sync(topology);

    if (!config.isQuiet() && !config.isDryRun()) {
      topicManager.printCurrentState(System.out);
      accessControlManager.printCurrentState(System.out);
    }
  }

  public void close() {
    topicManager.close();
  }

  public static String getVersion() {
    InputStream resourceAsStream =
        KafkaTopologyBuilder.class.getResourceAsStream(
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

  private static ClusterState buildStateProcessor(TopologyBuilderConfig config) throws IOException {

    String stateProcessorClass =
        config
            .getOrDefault(STATE_PROCESSOR_IMPLEMENTATION_CLASS, STATE_PROCESSOR_DEFAULT_CLASS)
            .toString();

    try {
      if (stateProcessorClass.equalsIgnoreCase(STATE_PROCESSOR_DEFAULT_CLASS)) {
        return new ClusterState(new FileStateProcessor());
      } else if (stateProcessorClass.equalsIgnoreCase(REDIS_STATE_PROCESSOR_CLASS)) {
        String host = config.getProperty(REDIS_HOST_CONFIG);
        int port = Integer.parseInt(config.getProperty(REDIS_PORT_CONFIG));
        return new ClusterState(new RedisStateProcessor(host, port));
      } else {
        throw new IOException(stateProcessorClass + " Unknown state processor provided.");
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  void setTopicManager(TopicManager topicManager) {
    this.topicManager = topicManager;
  }

  void setAccessControlManager(AccessControlManager accessControlManager) {
    this.accessControlManager = accessControlManager;
  }
}
