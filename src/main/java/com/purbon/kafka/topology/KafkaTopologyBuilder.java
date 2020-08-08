package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.TopologyBuilderConfig.REDIS_HOST_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.REDIS_PORT_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.REDIS_STATE_PROCESSOR_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.STATE_PROCESSOR_DEFAULT_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.STATE_PROCESSOR_IMPLEMENTATION_CLASS;

import com.purbon.kafka.topology.api.mds.MDSApiClientBuilder;
import com.purbon.kafka.topology.clusterstate.FileSateProcessor;
import com.purbon.kafka.topology.clusterstate.RedisSateProcessor;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class KafkaTopologyBuilder {

  public static final String SCHEMA_REGISTRY_URL = "confluent.schema.registry.url";

  private final String topologyFile;
  private Topology topology;
  private final TopologySerdes parser;
  private final TopologyBuilderAdminClient adminClient;
  private final TopologyBuilderConfig config;
  private final AccessControlProvider accessControlProvider;

  public KafkaTopologyBuilder(
      String topologyFile,
      TopologyBuilderConfig config,
      TopologyBuilderAdminClient adminClient,
      AccessControlProvider accessControlProvider) {
    this(topologyFile, new TopologySerdes(), config, adminClient, accessControlProvider);
  }

  public KafkaTopologyBuilder(
      String topologyFile,
      TopologySerdes parser,
      TopologyBuilderConfig config,
      TopologyBuilderAdminClient adminClient,
      AccessControlProvider accessControlProvider) {
    this(topologyFile, new TopologyImpl(), parser, config, adminClient, accessControlProvider);
  }

  public KafkaTopologyBuilder(
      Topology topology,
      TopologyBuilderConfig config,
      TopologyBuilderAdminClient adminClient,
      AccessControlProvider accessControlProvider) {
    this("", topology, new TopologySerdes(), config, adminClient, accessControlProvider);
  }

  public KafkaTopologyBuilder(
      Topology topology,
      TopologySerdes parser,
      TopologyBuilderConfig config,
      TopologyBuilderAdminClient adminClient,
      AccessControlProvider accessControlProvider) {
    this("", topology, parser, config, adminClient, accessControlProvider);
  }

  public KafkaTopologyBuilder(
      String topologyFile,
      Topology topology,
      TopologySerdes parser,
      TopologyBuilderConfig config,
      TopologyBuilderAdminClient adminClient,
      AccessControlProvider accessControlProvider) {
    this.topologyFile = topologyFile;
    this.topology = topology;
    this.parser = parser;
    this.config = config;
    this.adminClient = adminClient;
    this.accessControlProvider = accessControlProvider;
  }

  public KafkaTopologyBuilder(Topology topology, TopologyBuilderConfig builderConfig)
      throws IOException {
    this.topologyFile = "";
    this.topology = topology;
    this.parser = new TopologySerdes();
    this.config = builderConfig;
    this.adminClient = new TopologyBuilderAdminClientBuilder(builderConfig).build();
    this.accessControlProvider =
        new AccessControlProviderFactory(
                builderConfig, adminClient, new MDSApiClientBuilder(builderConfig))
            .get();
  }

  public void run() throws IOException {

    if (!topologyFile.isEmpty()) {
      topology = buildTopology(topologyFile);
    }

    config.validateWith(topology);

    ClusterState cs = buildStateProcessor();

    AccessControlManager accessControlManager =
        new AccessControlManager(accessControlProvider, cs, config.params());
    accessControlManager.sync(topology);

    String schemaRegistryUrl = (String) config.getOrDefault(SCHEMA_REGISTRY_URL, "http://foo:8082");
    SchemaRegistryClient schemaRegistryClient =
        new CachedSchemaRegistryClient(schemaRegistryUrl, 10);
    SchemaRegistryManager schemaRegistryManager = new SchemaRegistryManager(schemaRegistryClient);

    TopicManager topicManager = new TopicManager(adminClient, schemaRegistryManager, config);
    topicManager.sync(topology);

    if (!config.isQuite()) {
      topicManager.printCurrentState(System.out);
      accessControlManager.printCurrentState(System.out);
    }
  }

  public void close() {
    adminClient.close();
  }

  public Topology buildTopology(String fileOrDir) throws IOException {
    List<Topology> topologies = parseListOfTopologies(fileOrDir);
    Topology topology = topologies.get(0);
    if (topologies.size() > 1) {
      List<Topology> subTopologies = topologies.subList(1, topologies.size());
      for (Topology subTopology : subTopologies) {
        if (!topology.getTeam().equalsIgnoreCase(subTopology.getTeam())) {
          throw new IOException("Topologies from different teams are not allowed");
        }
        subTopology.getProjects().forEach(project -> topology.addProject(project));
      }
    }
    return topology;
  }

  private List<Topology> parseListOfTopologies(String fileOrDir) throws IOException {
    List<Topology> topologies = new ArrayList<>();
    boolean isDir = Files.isDirectory(Paths.get(fileOrDir));
    if (isDir) {
      Files.list(Paths.get(fileOrDir))
          .sorted()
          .map(
              path -> {
                try {
                  return parser.deserialise(path.toFile());
                } catch (IOException e) {
                  e.printStackTrace();
                  return new TopologyImpl();
                }
              })
          .forEach(subTopology -> topologies.add(subTopology));
    } else {
      Topology firstTopology = parser.deserialise(new File(fileOrDir));
      topologies.add(firstTopology);
    }
    return topologies;
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
