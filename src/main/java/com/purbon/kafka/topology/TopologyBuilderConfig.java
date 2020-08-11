package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION;
import static com.purbon.kafka.topology.KafkaTopologyBuilder.SCHEMA_REGISTRY_URL;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.nerdynick.commons.configuration.utils.FileConfigUtils;
import com.purbon.kafka.topology.configuration.LookupFactory;
import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.DataConfiguration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.configuration2.interpol.Lookup;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyBuilderConfig extends DataConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(TopologyBuilderConfig.class);

  public static final String KAFKA_INTERNAL_TOPIC_PREFIXES = "kafka.internal.topic.prefixes";
  public static final String KAFKA_INTERNAL_TOPIC_PREFIXES_DEFAULT = "_";

  public static final String ACCESS_CONTROL_IMPLEMENTATION_CLASS = "topology.builder.access.control.class";

  public static final String ACCESS_CONTROL_DEFAULT_CLASS = "com.purbon.kafka.topology.roles.SimpleAclsProvider";
  public static final String RBAC_ACCESS_CONTROL_CLASS = "com.purbon.kafka.topology.roles.RBACProvider";

  public static final String STATE_PROCESSOR_IMPLEMENTATION_CLASS = "topology.builder.state.processor.class";

  public static final String STATE_PROCESSOR_DEFAULT_CLASS = "com.purbon.kafka.topology.clusterstate.FileStateProcessor";
  public static final String REDIS_STATE_PROCESSOR_CLASS = "com.purbon.kafka.topology.clusterstate.RedisStateProcessor";

  public static final String REDIS_HOST_CONFIG = "topology.builder.redis.host";

  public static final String REDIS_PORT_CONFIG = "topology.builder.redis.port";

  public static final String MDS_SERVER = "topology.builder.mds.server";
  public static final String MDS_USER_CONFIG = "topology.builder.mds.user";
  public static final String MDS_PASSWORD_CONFIG = "topology.builder.mds.password";
  public static final String MDS_KAFKA_CLUSTER_ID_CONFIG = "topology.builder.mds.kafka.cluster.id";
  public static final String MDS_SR_CLUSTER_ID_CONFIG = "topology.builder.mds.schema.registry.cluster.id";
  public static final String MDS_KC_CLUSTER_ID_CONFIG = "topology.builder.mds.kafka.connect.cluster.id";

  public static final String CONFLUENT_MONITORING_TOPIC_CONFIG = "confluent.monitoring.topic";
  public static final String CONFLUENT_MONITORING_TOPIC_DEFAULT = "_confluent-monitoring";

  public static final String CONFLUENT_COMMAND_TOPIC_CONFIG = "confluent.command.topic";
  public static final String CONFLUENT_COMMAND_TOPIC_DEFAULT = "_confluent-command";

  public static final String CONFLUENT_METRICS_TOPIC_CONFIG = "confluent.metrics.topic";
  public static final String CONFLUENT_METRICS_TOPIC_DEFAULT = "_confluent-metrics";

  public static final String LOOKUPS_ENABLED = "topology.builder.lookups";

  public TopologyBuilderConfig() {
    this(new HashMap<>(), new BaseConfiguration());
  }

  public TopologyBuilderConfig(Map<String, String> cliParams) {
    this(cliParams, null);
  }

  public TopologyBuilderConfig(Map<String, String> cliParams, Configuration properties) {
    this(cliParams, properties, new LookupFactory());
  }

  public TopologyBuilderConfig(Map<String, String> cliParams, Configuration properties, LookupFactory lookupFactory) {
    super(new CompositeConfiguration(new MapConfiguration(new HashMap<>())));

    this.addProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, cliParams.get(BuilderCLI.BROKERS_OPTION));
    this.addProperty(AdminClientConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
    this.addProperty(BuilderCLI.ALLOW_DELETE_OPTION, cliParams.get(BuilderCLI.ALLOW_DELETE_OPTION));
    this.addProperty(BuilderCLI.QUITE_OPTION, cliParams.get(BuilderCLI.QUITE_OPTION));

    if (cliParams.get(ADMIN_CLIENT_CONFIG_OPTION) != null) {
      try {
        ((CompositeConfiguration) this.getConfiguration())
            .addConfiguration(FileConfigUtils.newFileConfig(cliParams.get(ADMIN_CLIENT_CONFIG_OPTION)));
      } catch (IOException e) {
        LOG.error("Failed to parse Kafka AdminAPI Config", e);
      }
    }

    if (properties != null) {
      ((CompositeConfiguration) this.getConfiguration()).addConfiguration(properties);
    }

    this.getStringList(LOOKUPS_ENABLED, "").forEach(l -> {
      final Configuration lookupConfig = this.subset(LOOKUPS_ENABLED + "." + l.toLowerCase());
      final Optional<Lookup> lookup = lookupFactory.get(l, lookupConfig);
      
      if(lookup.isPresent()){
        this.getInterpolator().registerLookup(l.toLowerCase(), lookup.get());
      } else {
        LOG.warn("Lookup `{}` is undefined or failed to be created", l);
      }
    });
  }

  public void validateWith(Topology topology) throws ConfigurationException {

    validateGeneralConfiguration(topology);

    final boolean isRbac =
        this.getString(ACCESS_CONTROL_IMPLEMENTATION_CLASS, ACCESS_CONTROL_DEFAULT_CLASS)
            .equalsIgnoreCase(RBAC_ACCESS_CONTROL_CLASS);

    if (isRbac) {
      validateRBACConfiguration(topology);
    }
  }

  public void validateRBACConfiguration(Topology topology) throws ConfigurationException {
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
    } else if (hasKafkaConnect && !this.containsKey(MDS_KC_CLUSTER_ID_CONFIG)) {
      raiseIfNull(MDS_KC_CLUSTER_ID_CONFIG);
    }
  }

  public void validateGeneralConfiguration(Topology topology) throws ConfigurationException {
    if (countOfSchemas(topology) > 0) {
      raiseIfNull(SCHEMA_REGISTRY_URL);
    }
  }

  private long countOfSchemas(Topology topology) {
    return topology.getProjects().stream()
        .flatMap((Function<Project, Stream<Topic>>) project -> project.getTopics().stream())
        .map(topic -> topic.getSchemas())
        .filter(topicSchemas -> topicSchemas != null)
        .count();
  }

  private void raiseIfNull(String... keys) throws ConfigurationException {
    for (String key : keys) {
      raiseIfNull(key);
    }
  }

  private void raiseIfNull(String key) throws ConfigurationException {
    if (!this.containsKey(key)) {
      throw new ConfigurationException(
          "Required configuration " + key + " is missing, please add it to your configuration");
    }
  }

  public String getConfluentMonitoringTopic() {
    return this.getString(CONFLUENT_MONITORING_TOPIC_CONFIG, CONFLUENT_MONITORING_TOPIC_DEFAULT)
        .toString();
  }

  public String getConfluentCommandTopic() {
    return this.getString(CONFLUENT_COMMAND_TOPIC_CONFIG, CONFLUENT_COMMAND_TOPIC_DEFAULT)
        .toString();
  }

  public String getConfluentMetricsTopic() {
    return this.getString(CONFLUENT_METRICS_TOPIC_CONFIG, CONFLUENT_METRICS_TOPIC_DEFAULT)
        .toString();
  }

  public boolean isQuite() {
    return this.getBoolean(BuilderCLI.QUITE_OPTION, false);
  }

  public boolean allowDeleteTopics() {
    return this.getBoolean(BuilderCLI.ALLOW_DELETE_OPTION, true);
  }

  public List<String> getStringList(String key, String def) {
    return Arrays.asList(this.getString(key, def).split(","));
  }

  public <T> Class<? extends T> getCls(Class<T> cls, String key, Class<? extends T> def) throws ClassNotFoundException {
    if (this.containsKey(key)) {
      return (Class<? extends T>) Class.forName(this.getString(key));
    } else {
      return def;
    }
  }
}
