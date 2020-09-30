package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.DRY_RUN_OPTION;

import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

  public static final String CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG = "schema.registry.url";
  public static final String CONFLUENT_SCHEMA_REGISTRY_URL_DEFAULT = "mock://";

  public static final String CONFLUENT_MONITORING_TOPIC_CONFIG = "confluent.monitoring.topic";
  public static final String CONFLUENT_MONITORING_TOPIC_DEFAULT = "_confluent-monitoring";

  public static final String CONFLUENT_COMMAND_TOPIC_CONFIG = "confluent.command.topic";
  public static final String CONFLUENT_COMMAND_TOPIC_DEFAULT = "_confluent-command";

  public static final String CONFLUENT_METRICS_TOPIC_CONFIG = "confluent.metrics.topic";
  public static final String CONFLUENT_METRICS_TOPIC_DEFAULT = "_confluent-metrics";

  public static final String TOPIC_PREFIX_FORMAT_CONFIG = "topology.topic.prefix.format";
  public static final String TOPIC_PREFIX_FORMAT_DEFAULT = "default";

  public static final String PROJECT_PREFIX_FORMAT_CONFIG = "topology.project.prefix.format";
  public static final String PROJECT_PREFIX_FORMAT_DEFAULT = "default";

  public static final String TOPIC_PREFIX_SEPARATOR_CONFIG = "topology.topic.prefix.separator";
  public static final String TOPIC_PREFIX_SEPARATOR_DEFAULT = ".";

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

  public Map<String, ?> asMap() {
    return asMap("");
  }

  public Map<String, ?> asMap(String filter) {
    Map<String, Object> map = new HashMap<>();
    properties.keySet().stream()
        .filter(o -> filter.isEmpty() || String.valueOf(o).startsWith(filter))
        .forEach(key -> map.put(String.valueOf(key), properties.get(key)));
    return map;
  }

  public void validateWith(Topology topology) throws ConfigurationException {

    validateGeneralConfiguration(topology);

    boolean isRBAC = this.getAccessControlClassName().equalsIgnoreCase(RBAC_ACCESS_CONTROL_CLASS);

    if (isRBAC) {
      validateRBACConfiguration(topology);
    }
  }

  public void validateRBACConfiguration(Topology topology) throws ConfigurationException {
    raiseIfNull(MDS_SERVER, MDS_USER_CONFIG, MDS_PASSWORD_CONFIG);
    raiseIfNull(MDS_KAFKA_CLUSTER_ID_CONFIG);

    final boolean hasSchemaRegistry = !topology.getPlatform().getSchemaRegistry().isEmpty();
    final boolean hasKafkaConnect =
        !topology.getProjects().stream().allMatch(project -> project.getConnectors().isEmpty());

    if (hasSchemaRegistry) {
      raiseIfNull(MDS_SR_CLUSTER_ID_CONFIG);
    } else if (hasKafkaConnect && properties.getProperty(MDS_KC_CLUSTER_ID_CONFIG) == null) {
      raiseIfNull(MDS_KC_CLUSTER_ID_CONFIG);
    }
  }

  private void validateGeneralConfiguration(Topology topology) throws ConfigurationException {
    if (countOfSchemas(topology) > 0) {
      raiseIfNull(CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG);
    }
    boolean topicPrefixDefinedButNotProjectPrefix =
        !getTopicPrefixFormat().equals(TOPIC_PREFIX_FORMAT_DEFAULT)
            && getProjectPrefixFormat().equals(PROJECT_PREFIX_FORMAT_DEFAULT);

    boolean projectPrefixDefinedButNotTopicPrefix =
        getTopicPrefixFormat().equals(TOPIC_PREFIX_FORMAT_DEFAULT)
            && !getProjectPrefixFormat().equals(PROJECT_PREFIX_FORMAT_DEFAULT);

    if (topicPrefixDefinedButNotProjectPrefix || projectPrefixDefinedButNotTopicPrefix) {
      throw new ConfigurationException(
          TOPIC_PREFIX_FORMAT_CONFIG
              + "and "
              + PROJECT_PREFIX_FORMAT_CONFIG
              + "need to be defined together");
    }
  }

  private static long countOfSchemas(Topology topology) {
    return topology.getProjects().stream()
        .flatMap((Function<Project, Stream<Topic>>) project -> project.getTopics().stream())
        .map(topic -> topic.getSchemas())
        .filter(topicSchemas -> topicSchemas != null)
        .count();
  }

  private void raiseIfNull(String... keys) throws ConfigurationException {
    for (String key : keys) {
      raiseIfValueIsNull(key, properties.getProperty(key));
    }
  }

  private void raiseIfValueIsNull(String key, String value) throws ConfigurationException {
    if (value == null) {
      throw new ConfigurationException(
          "Required configuration " + key + " is missing, please add it to your configuration");
    }
  }

  public String getProperty(String key) {
    return properties.getProperty(key);
  }

  public List<String> getPropertyAsList(String key, String defaultVal, String regexp) {
    Object val = properties.getOrDefault(key, defaultVal);
    return Arrays.asList(String.valueOf(val).split(regexp));
  }

  public Object getOrDefault(Object key, Object _default) {
    return properties.getOrDefault(key, _default);
  }

  public List<String> getKafkaInternalTopicPrefixes() {
    return getPropertyAsList(
            KAFKA_INTERNAL_TOPIC_PREFIXES, KAFKA_INTERNAL_TOPIC_PREFIXES_DEFAULT, ",")
        .stream()
        .map(String::trim)
        .collect(Collectors.toList());
  }

  public String getConfluentSchemaRegistryUrl() {
    return properties
        .getOrDefault(CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG, CONFLUENT_SCHEMA_REGISTRY_URL_DEFAULT)
        .toString();
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

  public String getAccessControlClassName() {
    return properties
        .getOrDefault(ACCESS_CONTROL_IMPLEMENTATION_CLASS, ACCESS_CONTROL_DEFAULT_CLASS)
        .toString();
  }

  public String getStateProcessorImplementationClassName() {
    return properties
        .getOrDefault(STATE_PROCESSOR_IMPLEMENTATION_CLASS, STATE_PROCESSOR_DEFAULT_CLASS)
        .toString();
  }

  public String getTopicPrefixFormat() {
    return properties
        .getOrDefault(TOPIC_PREFIX_FORMAT_CONFIG, TOPIC_PREFIX_FORMAT_DEFAULT)
        .toString();
  }

  public String getProjectPrefixFormat() {
    return properties
        .getOrDefault(PROJECT_PREFIX_FORMAT_CONFIG, PROJECT_PREFIX_FORMAT_DEFAULT)
        .toString();
  }

  public String getTopicPrefixSeparator() {
    return properties
        .getOrDefault(TOPIC_PREFIX_SEPARATOR_CONFIG, TOPIC_PREFIX_SEPARATOR_DEFAULT)
        .toString();
  }

  public boolean allowDelete() {
    return Boolean.valueOf(cliParams.getOrDefault(BuilderCLI.ALLOW_DELETE_OPTION, "true"));
  }

  public boolean isQuiet() {
    return Boolean.valueOf(cliParams.getOrDefault(BuilderCLI.QUIET_OPTION, "false"));
  }

  public boolean isDryRun() {
    return Boolean.valueOf(cliParams.getOrDefault(DRY_RUN_OPTION, "false"));
  }

  private static Properties buildProperties(Map<String, String> cliParams) {
    Properties props = new Properties();
    final String adminClientConfigPath = cliParams.get(BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION);
    if (adminClientConfigPath != null) {
      try {
        props.load(new FileInputStream(adminClientConfigPath));
      } catch (IOException e) {
        System.err.println("Could not load properties file " + adminClientConfigPath);
      }
    }
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, cliParams.get(BuilderCLI.BROKERS_OPTION));
    props.put(AdminClientConfig.RETRIES_CONFIG, Integer.MAX_VALUE);

    return props;
  }

  public Properties getProperties() {
    return this.properties;
  }
}
