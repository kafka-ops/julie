package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.BROKERS_OPTION;
import static com.purbon.kafka.topology.TopologyBuilderConfig.ACCESS_CONTROL_DEFAULT_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.ACCESS_CONTROL_IMPLEMENTATION_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_PASSWORD_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_SERVER;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_USER_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.RBAC_ACCESS_CONTROL_CLASS;

import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.roles.RBACProvider;
import com.purbon.kafka.topology.roles.SimpleAclsProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;

public class KafkaTopologyBuilder {

  private final String topologyFile;
  private final Map<String, String> config;
  private final TopologySerdes parser;
  private final Properties properties;
  private final TopologyBuilderAdminClient builderAdminClient;

  public KafkaTopologyBuilder(String topologyFile, Map<String, String> config) {
    this.topologyFile = topologyFile;
    this.config = config;
    this.parser = new TopologySerdes();

    this.properties = buildProperties(config);
    this.builderAdminClient = buildTopologyAdminClient(config);

  }

  public void run() throws IOException {

    Topology topology = parser.deserialise(new File(topologyFile));

    AccessControlProvider aclsProvider = buildAccessControlProvider();
    AccessControlManager accessControlManager = new AccessControlManager(aclsProvider);

    TopicManager topicManager = new TopicManager(builderAdminClient);

    topicManager.sync(topology);
    accessControlManager.sync(topology);

  }

  private AccessControlProvider buildAccessControlProvider() throws IOException {
    String accessControlClass = config.getOrDefault(
        ACCESS_CONTROL_IMPLEMENTATION_CLASS,
        "com.purbon.kafka.topology.roles.SimpleAclsProvider"
    );

    try {
      Class<?> clazz = Class.forName(accessControlClass);

      if (accessControlClass.equalsIgnoreCase(ACCESS_CONTROL_DEFAULT_CLASS)) {
        Constructor<?> constructor = clazz.getConstructor(TopologyBuilderAdminClient.class);
        return (SimpleAclsProvider) constructor.newInstance(builderAdminClient);
      } else if (accessControlClass.equalsIgnoreCase(RBAC_ACCESS_CONTROL_CLASS)) {

        String mdsServer = config.get(MDS_SERVER);
        String mdsUser = config.get(MDS_USER_CONFIG);
        String mdsPassword = config.get(MDS_PASSWORD_CONFIG);

        MDSApiClient apiClient = new MDSApiClient(mdsServer);
        apiClient.login(mdsUser, mdsPassword);
        Constructor<?> constructor = clazz.getConstructor(MDSApiClient.class);
        return (RBACProvider) constructor.newInstance(apiClient);
      } else {
        throw new IOException(accessControlClass + " Unknown access control provided.");
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  private Properties buildProperties(Map<String, String> config) {
    Properties props = new Properties();
    if (config.get(ADMIN_CLIENT_CONFIG_OPTION) != null) {
      try {
        props.load(new FileInputStream(config.get(ADMIN_CLIENT_CONFIG_OPTION)));
      } catch (IOException e) {
        //TODO: Can be ignored
      }
    } else {
      props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, config.get(BROKERS_OPTION));
    }
    props.put(AdminClientConfig.RETRIES_CONFIG, Integer.MAX_VALUE);

    return props;
  }

  private TopologyBuilderAdminClient buildTopologyAdminClient(Map<String, String> config) {
    AdminClient kafkaAdminClient = buildKafkaAdminClient(config);
    return new TopologyBuilderAdminClient(kafkaAdminClient);
  }

  private AdminClient buildKafkaAdminClient(Map<String, String> config) {
    Properties props = buildProperties(config);
    return AdminClient.create(props);
  }

}
