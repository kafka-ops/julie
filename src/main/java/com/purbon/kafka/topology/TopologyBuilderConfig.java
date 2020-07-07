package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.BROKERS_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.QUITE_OPTION;

import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topology;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.admin.AdminClientConfig;

public class TopologyBuilderConfig {

  public static final String KAFKA_INTERNAL_TOPIC_PREFIXES = "kafka.internal.topic.prefixes";
  public static final String KAFKA_INTERNAL_TOPIC_PREFIXES_DEFAULT = "_";

  public static final String ACCESS_CONTROL_IMPLEMENTATION_CLASS =
      "topology.builder.access.control.class";

  public static final String ACCESS_CONTROL_DEFAULT_CLASS =
      "com.purbon.kafka.topology.roles.SimpleAclsProvider";
  public static final String RBAC_ACCESS_CONTROL_CLASS =
      "com.purbon.kafka.topology.roles.RBACProvider";

  public static final String STATE_PROCESSOR_IMPLEMENTATION_CLASS =
      "topology.builder.state.processor.class";

  public static final String STATE_PROCESSOR_DEFAULT_CLASS =
      "com.purbon.kafka.topology.clusterstate.FileStateProcessor";
  public static final String REDIS_STATE_PROCESSOR_CLASS =
      "com.purbon.kafka.topology.clusterstate.RedisStateProcessor";

  public static final String REDIS_HOST_CONFIG = "topology.builder.redis.host";

  public static final String REDIS_PORT_CONFIG = "topology.builder.redis.port";

  public static final String MDS_SERVER = "topology.builder.mds.server";
  public static final String MDS_USER_CONFIG = "topology.builder.mds.user";
  public static final String MDS_PASSWORD_CONFIG = "topology.builder.mds.password";
  public static final String MDS_KAFKA_CLUSTER_ID_CONFIG = "topology.builder.mds.kafka.cluster.id";
  public static final String MDS_SR_CLUSTER_ID_CONFIG =
      "topology.builder.mds.schema.registry.cluster.id";
  public static final String MDS_KC_CLUSTER_ID_CONFIG =
      "topology.builder.mds.kafka.connect.cluster.id";

  public static final String CONFLUENT_MONITORING_TOPIC_CONFIG = "confluent.monitoring.topic";
  public static final String CONFLUENT_MONITORING_TOPIC_DEFAULT = "_confluent-monitoring";

  public static final String CONFLUENT_COMMAND_TOPIC_CONFIG = "confluent.command.topic";
  public static final String CONFLUENT_COMMAND_TOPIC_DEFAULT = "_confluent-command";

  public static final String CONFLUENT_METRICS_TOPIC_CONFIG = "confluent.metrics.topic";
  public static final String CONFLUENT_METRICS_TOPIC_DEFAULT = "_confluent-metrics";

  private final Map<String, String> cliParams;
  private final Properties properties;

  public TopologyBuilderConfig() {
    this(new HashMap<>(), new Properties());
  }

  public TopologyBuilderConfig(Map<String, String> cliParams) {
    this(cliParams, buildProperties(cliParams));
  }

  public TopologyBuilderConfig(Map<String, String> cliParams, Properties properties) {
    this.cliParams = cliParams;
    this.properties = properties;
  }

  public void validateWith(Topology topology) throws ConfigurationException {

    raiseIfNull(ACCESS_CONTROL_IMPLEMENTATION_CLASS);

    boolean isRbac =
        properties
            .getProperty(ACCESS_CONTROL_IMPLEMENTATION_CLASS)
            .equalsIgnoreCase(RBAC_ACCESS_CONTROL_CLASS);
    if (!isRbac) {
      return;
    }

    raiseIfNull(MDS_SERVER, MDS_USER_CONFIG, MDS_PASSWORD_CONFIG);

    boolean hasSchemaRegistry = !topology.getPlatform().getSchemaRegistry().isEmpty();
    boolean hasKafkaConnect = false;
    List<Project> projects = topology.getProjects();
    for (int i = 0; !hasKafkaConnect && i < projects.size(); i++) {
      Project project = projects.get(i);
      hasKafkaConnect = !project.getConnectors().isEmpty();
    }

    raiseIfNull(MDS_KAFKA_CLUSTER_ID_CONFIG);

    if (hasSchemaRegistry) {
      raiseIfNull(MDS_SR_CLUSTER_ID_CONFIG);
    } else if (hasKafkaConnect && properties.getProperty(MDS_KC_CLUSTER_ID_CONFIG) == null) {
      raiseIfNull(MDS_KC_CLUSTER_ID_CONFIG);
    }
  }

  private void raiseIfNull(String... keys) throws ConfigurationException {
    for (String key : keys) {
      raiseIfNull(key, properties.getProperty(key));
    }
  }

  private void raiseIfNull(String key, String value) throws ConfigurationException {
    if (value == null) {
      throw new ConfigurationException(
          "Required configuration " + key + " is missing, please add it to your configuration");
    }
  }

  public Map<String, String> params() {
    return cliParams;
  }

  public String getProperty(String key) {
    return properties.getProperty(key);
  }

  public List<String> getPropertyAsList(String key, String def, String regexp) {
    Object val = properties.getOrDefault(key, def);
    return Arrays.asList(String.valueOf(val).split(regexp));
  }

  public Object getOrDefault(Object key, Object _default) {
    return properties.getOrDefault(key, _default);
  }

  public String getConfluentMonitoringTopic() {
    return properties
        .getOrDefault(CONFLUENT_MONITORING_TOPIC_CONFIG, CONFLUENT_MONITORING_TOPIC_DEFAULT)
        .toString();
  }

  public String getConfluentCommandTopic() {
    return properties
        .getOrDefault(CONFLUENT_COMMAND_TOPIC_CONFIG, CONFLUENT_COMMAND_TOPIC_DEFAULT)
        .toString();
  }

  public String getConfluentMetricsTopic() {
    return properties
        .getOrDefault(CONFLUENT_METRICS_TOPIC_CONFIG, CONFLUENT_METRICS_TOPIC_DEFAULT)
        .toString();
  }

  public boolean isQuite() {
    return Boolean.valueOf(cliParams.getOrDefault(QUITE_OPTION, "false"));
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

  public Properties getProperties() {
    return this.properties;
  }
}
