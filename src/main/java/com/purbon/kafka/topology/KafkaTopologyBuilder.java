package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.BROKERS_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.QUITE_OPTION;
import static com.purbon.kafka.topology.TopologyBuilderConfig.ACCESS_CONTROL_DEFAULT_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.ACCESS_CONTROL_IMPLEMENTATION_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_KAFKA_CLUSTER_ID_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_KC_CLUSTER_ID_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_PASSWORD_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_SERVER;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_SR_CLUSTER_ID_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_USER_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.RBAC_ACCESS_CONTROL_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.REDIS_HOST_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.REDIS_PORT_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.REDIS_STATE_PROCESSOR_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.STATE_PROCESSOR_DEFAULT_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.STATE_PROCESSOR_IMPLEMENTATION_CLASS;

import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.clusterstate.FileSateProcessor;
import com.purbon.kafka.topology.clusterstate.RedisSateProcessor;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.roles.RBACProvider;
import com.purbon.kafka.topology.roles.SimpleAclsProvider;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;

public class KafkaTopologyBuilder {

  private final String topologyFile;
  private final TopologySerdes parser;
  private final TopologyBuilderAdminClient builderAdminClient;
  private final boolean quiteOut;
  private final TopologyBuilderConfig config;

  public KafkaTopologyBuilder(String topologyFile, Map<String, String> cliParams) {
    this(
        topologyFile,
        new TopologySerdes(),
        new TopologyBuilderConfig(cliParams, buildProperties(cliParams)),
        buildTopologyAdminClient(new TopologyBuilderConfig(cliParams, buildProperties(cliParams))),
        Boolean.valueOf(cliParams.getOrDefault(QUITE_OPTION, "false")));
  }

  public KafkaTopologyBuilder(
      String topologyFile,
      TopologySerdes parser,
      TopologyBuilderConfig config,
      TopologyBuilderAdminClient adminClient,
      boolean quiteOut) {
    this.topologyFile = topologyFile;
    this.parser = parser;
    this.config = config;
    this.builderAdminClient = adminClient;
    this.quiteOut = quiteOut;
  }

  public void run() throws IOException {

    try {
      Topology topology = buildTopology(topologyFile);
      config.validateWith(topology);

      AccessControlProvider aclsProvider = buildAccessControlProvider();
      ClusterState cs = buildStateProcessor();

      AccessControlManager accessControlManager =
          new AccessControlManager(aclsProvider, cs, config.params());

      TopicManager topicManager = new TopicManager(builderAdminClient, config);
      topicManager.sync(topology);
      accessControlManager.sync(topology);
      if (!quiteOut) {
        topicManager.printCurrentState(System.out);
        accessControlManager.printCurrentState(System.out);
      }
    } finally {
      builderAdminClient.close();
    }
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
                  return new Topology();
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

  public AccessControlProvider buildAccessControlProvider() throws IOException {
    String accessControlClass =
        config
            .getOrDefault(
                ACCESS_CONTROL_IMPLEMENTATION_CLASS,
                "com.purbon.kafka.topology.roles.SimpleAclsProvider")
            .toString();

    try {
      Class<?> clazz = Class.forName(accessControlClass);

      if (accessControlClass.equalsIgnoreCase(ACCESS_CONTROL_DEFAULT_CLASS)) {
        Constructor<?> constructor = clazz.getConstructor(TopologyBuilderAdminClient.class);
        return (SimpleAclsProvider) constructor.newInstance(builderAdminClient);
      } else if (accessControlClass.equalsIgnoreCase(RBAC_ACCESS_CONTROL_CLASS)) {

        String mdsServer = config.getProperty(MDS_SERVER);
        String mdsUser = config.getProperty(MDS_USER_CONFIG);
        String mdsPassword = config.getProperty(MDS_PASSWORD_CONFIG);

        MDSApiClient apiClient = new MDSApiClient(mdsServer);

        // Pass Cluster IDS
        String kafkaClusterID = config.getProperty(MDS_KAFKA_CLUSTER_ID_CONFIG);
        apiClient.setKafkaClusterId(kafkaClusterID);
        String schemaRegistryClusterID = config.getProperty(MDS_SR_CLUSTER_ID_CONFIG);
        apiClient.setSchemaRegistryClusterID(schemaRegistryClusterID);
        String kafkaConnectClusterID = config.getProperty(MDS_KC_CLUSTER_ID_CONFIG);
        apiClient.setConnectClusterID(kafkaConnectClusterID);

        // Login and authenticate with the server
        apiClient.login(mdsUser, mdsPassword);
        apiClient.authenticate();

        Constructor<?> constructor = clazz.getConstructor(MDSApiClient.class);
        return (RBACProvider) constructor.newInstance(apiClient);
      } else {
        throw new IOException(accessControlClass + " Unknown access control provided.");
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  private static Properties buildProperties(Map<String, String> cliParams) {
    Properties props = new Properties();
    if (cliParams.get(ADMIN_CLIENT_CONFIG_OPTION) != null) {
      try {
        props.load(new FileInputStream(cliParams.get(ADMIN_CLIENT_CONFIG_OPTION)));
      } catch (IOException e) {
        // TODO: Can be ignored
      }
    }
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, cliParams.get(BROKERS_OPTION));
    props.put(AdminClientConfig.RETRIES_CONFIG, Integer.MAX_VALUE);

    return props;
  }

  private static TopologyBuilderAdminClient buildTopologyAdminClient(TopologyBuilderConfig config) {
    AdminClient kafkaAdminClient = buildKafkaAdminClient(config.params());
    return new TopologyBuilderAdminClient(kafkaAdminClient, config);
  }

  private static AdminClient buildKafkaAdminClient(Map<String, String> cliParams) {
    Properties props = buildProperties(cliParams);
    // props.put("default.api.timeout.ms", 10000);
    // props.put("request.timeout.ms", 10000);
    return AdminClient.create(props);
  }
}
