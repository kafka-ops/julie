package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.BROKERS_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.QUITE_OPTION;
import static com.purbon.kafka.topology.TopologyBuilderConfig.ACCESS_CONTROL_DEFAULT_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.ACCESS_CONTROL_IMPLEMENTATION_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_KAFKA_CLUSTER_ID_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_PASSWORD_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_SR_CLUSTER_ID_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_USER_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.RBAC_ACCESS_CONTROL_CLASS;

import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.roles.RBACProvider;
import com.purbon.kafka.topology.roles.SimpleAclsProvider;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;

public class KafkaTopologyBuilder {

  private final String topologyFile;
  private final TopologySerdes parser;
  private final Properties properties;
  private final TopologyBuilderAdminClient builderAdminClient;
  private final boolean quiteOut;
  private final Map<String, String> cliParams;

  public KafkaTopologyBuilder(String topologyFile, Map<String, String> cliParams) {
    this.topologyFile = topologyFile;
    this.parser = new TopologySerdes();
    this.cliParams = cliParams;
    this.properties = buildProperties(cliParams);
    this.builderAdminClient = buildTopologyAdminClient(cliParams);
    this.quiteOut = Boolean.valueOf(cliParams.getOrDefault(QUITE_OPTION, "false"));
  }

  public void run() throws IOException {

    Topology topology = parser.deserialise(new File(topologyFile));

    AccessControlProvider aclsProvider = buildAccessControlProvider();
    AccessControlManager accessControlManager = new AccessControlManager(aclsProvider, cliParams);

    TopicManager topicManager = new TopicManager(builderAdminClient, cliParams);

    topicManager.sync(topology);
    accessControlManager.sync(topology);
    if (!quiteOut) {
      topicManager.printCurrentState(System.out);
      accessControlManager.printCurrentState(System.out);
    }
  }

  private AccessControlProvider buildAccessControlProvider() throws IOException {
    String accessControlClass =
        properties
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

        String mdsServer = properties.getProperty(MDS_USER_CONFIG);
        String mdsUser = properties.getProperty(MDS_USER_CONFIG);
        String mdsPassword = properties.getProperty(MDS_PASSWORD_CONFIG);

        MDSApiClient apiClient = new MDSApiClient(mdsServer);

        // Pass Cluster IDS
        String kafkaClusterID = properties.getProperty(MDS_KAFKA_CLUSTER_ID_CONFIG);
        apiClient.setKafkaClusterId(kafkaClusterID);
        String schemaRegistryClusterID = properties.getProperty(MDS_SR_CLUSTER_ID_CONFIG);
        apiClient.setSchemaRegistryClusterID(schemaRegistryClusterID);

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

  private Properties buildProperties(Map<String, String> cliParams) {
    Properties props = new Properties();
    if (cliParams.get(ADMIN_CLIENT_CONFIG_OPTION) != null) {
      try {
        props.load(new FileInputStream(cliParams.get(ADMIN_CLIENT_CONFIG_OPTION)));
      } catch (IOException e) {
        // TODO: Can be ignored
      }
    } else {
      props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, cliParams.get(BROKERS_OPTION));
    }
    props.put(AdminClientConfig.RETRIES_CONFIG, Integer.MAX_VALUE);

    return props;
  }

  private TopologyBuilderAdminClient buildTopologyAdminClient(Map<String, String> cliParams) {
    AdminClient kafkaAdminClient = buildKafkaAdminClient(cliParams);
    return new TopologyBuilderAdminClient(kafkaAdminClient);
  }

  private AdminClient buildKafkaAdminClient(Map<String, String> cliParams) {
    Properties props = buildProperties(cliParams);
    return AdminClient.create(props);
  }
}
