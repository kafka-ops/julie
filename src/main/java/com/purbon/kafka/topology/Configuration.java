package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.CommandLineInterface.*;
import static com.purbon.kafka.topology.Constants.*;

import com.purbon.kafka.topology.api.ksql.KsqlClientConfig;
import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.model.JulieRoles;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.serdes.JulieRolesSerdes;
import com.purbon.kafka.topology.serdes.TopologySerdes.FileType;
import com.purbon.kafka.topology.utils.BasicAuth;
import com.purbon.kafka.topology.utils.Pair;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Configuration {

  private static final Logger LOGGER = LogManager.getLogger(Configuration.class);

  private final Map<String, String> cliParams;
  private final Config config;

  public Configuration() {
    this(new HashMap<>(), ConfigFactory.load());
  }

  public static Configuration build(Map<String, String> cliParams) {
    return build(cliParams, cliParams.get(CLIENT_CONFIG_OPTION));
  }

  public static Configuration build(Map<String, String> cliParams, String configFile) {
    if (!configFile.isEmpty()) {
      System.setProperty("config.file", configFile);
    }
    ConfigFactory.invalidateCaches();
    Config config = ConfigFactory.load();

    String overridingConfigFile = cliParams.get(OVERRIDING_CLIENT_CONFIG_OPTION);
    if (overridingConfigFile != null) {
      Config overridingConfig = ConfigFactory.parseFile(new File(overridingConfigFile));
      config = overridingConfig.withFallback(config);
    }
    return new Configuration(cliParams, config);
  }

  public Configuration(Map<String, String> cliParams, Properties props) {
    this(cliParams, (Map) props);
  }

  public Configuration(Map<String, String> cliParams, Map<String, Object> props) {
    this(cliParams, ConfigFactory.parseMap(props).withFallback(ConfigFactory.load()));
  }

  public Configuration(Map<String, String> cliParams, Config config) {
    this.cliParams = cliParams;
    this.config = config;
  }

  public Map<String, ?> asMap() {
    return asMap("");
  }

  public Map<String, ?> asMap(String filter) {
    Map<String, Object> map = new HashMap<>();
    config.entrySet().stream()
        .filter(entry -> filter.isEmpty() || entry.getKey().startsWith(filter))
        .forEach(entry -> map.put(entry.getKey(), entry.getValue().unwrapped()));
    return map;
  }

  public Properties asProperties() {
    Properties props = new Properties();
    config.entrySet().forEach(entry -> props.put(entry.getKey(), entry.getValue().unwrapped()));
    if (cliParams.get(CommandLineInterface.BROKERS_OPTION) != null) {
      props.put(
          AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
          cliParams.get(CommandLineInterface.BROKERS_OPTION));
    }
    props.put(AdminClientConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
    return props;
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
    } else if (hasKafkaConnect && config.getString(MDS_KC_CLUSTER_ID_CONFIG) == null) {
      raiseIfNull(MDS_KC_CLUSTER_ID_CONFIG);
    }
  }

  private void validateGeneralConfiguration(Topology topology) throws ConfigurationException {
    if (hasSchemas(topology)) {
      raiseIfDefault(CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG, "mock://");
    }

    validateBrokersConfig();

    boolean topicPrefixDefinedButNotProjectPrefix =
        !getTopicPrefixFormat().equals("default") && getProjectPrefixFormat().equals("default");

    boolean projectPrefixDefinedButNotTopicPrefix =
        getTopicPrefixFormat().equals("default") && !getProjectPrefixFormat().equals("default");

    if (topicPrefixDefinedButNotProjectPrefix || projectPrefixDefinedButNotTopicPrefix) {
      throw new ConfigurationException(
          TOPIC_PREFIX_FORMAT_CONFIG
              + " and "
              + PROJECT_PREFIX_FORMAT_CONFIG
              + " need to be defined together.");
    }

    if (!getTopicPrefixFormat().startsWith(getProjectPrefixFormat())) {
      throw new ConfigurationException(
          TOPIC_PREFIX_FORMAT_CONFIG + "should start by" + PROJECT_PREFIX_FORMAT_CONFIG);
    }
  }

  private void validateBrokersConfig() throws ConfigurationException {
    boolean existServersAsConfig;
    try {
      config.getString(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG);
      existServersAsConfig = true;
    } catch (Exception ex) {
      existServersAsConfig = false;
    }

    if (cliParams.get(CommandLineInterface.BROKERS_OPTION) == null && !existServersAsConfig) {
      String msg =
          String.format(
              "Either the CLI option %s or the configuration %s should be specified",
              CommandLineInterface.BROKERS_OPTION, AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG);
      throw new ConfigurationException(msg);
    }
  }

  private static boolean hasSchemas(Topology topology) {
    return topology.getProjects().stream()
        .flatMap((Function<Project, Stream<Topic>>) project -> project.getTopics().stream())
        .anyMatch(topic -> !topic.getSchemas().isEmpty());
  }

  private void raiseIfDefault(String key, String _default) throws ConfigurationException {
    if (config.getString(key).equals(_default)) {
      throw new ConfigurationException(
          "Configuration key " + key + " should not have the default value " + _default);
    }
  }

  private void raiseIfNull(String... keys) throws ConfigurationException {
    try {
      for (String key : keys) {
        config.getString(key);
      }
    } catch (Exception ex) {
      throw new ConfigurationException(ex.getMessage());
    }
  }

  public String getProperty(String key) {
    return config.getString(key);
  }

  public List<String> getKafkaInternalTopicPrefixes() {
    return config.getStringList(KAFKA_INTERNAL_TOPIC_PREFIXES).stream()
        .map(String::trim)
        .collect(Collectors.toList());
  }

  public List<String> getServiceAccountManagedPrefixes() {
    return config.getStringList(SERVICE_ACCOUNT_MANAGED_PREFIXES).stream()
        .map(String::trim)
        .collect(Collectors.toList());
  }

  public List<String> getTopicManagedPrefixes() {
    return config.getStringList(TOPIC_MANAGED_PREFIXES).stream()
        .map(String::trim)
        .collect(Collectors.toList());
  }

  public List<String> getGroupManagedPrefixes() {
    return config.getStringList(GROUP_MANAGED_PREFIXES).stream()
        .map(String::trim)
        .collect(Collectors.toList());
  }

  public String getConfluentSchemaRegistryUrl() {
    return config.getString(CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG);
  }

  public String getConfluentMonitoringTopic() {
    return config.getString(CONFLUENT_MONITORING_TOPIC_CONFIG);
  }

  public String getConfluentCommandTopic() {
    return config.getString(CONFLUENT_COMMAND_TOPIC_CONFIG);
  }

  public String getConfluentMetricsTopic() {
    return config.getString(CONFLUENT_METRICS_TOPIC_CONFIG);
  }

  public String getAccessControlClassName() {
    return config.getString(ACCESS_CONTROL_IMPLEMENTATION_CLASS);
  }

  public String getStateProcessorImplementationClassName() {
    return config.getString(STATE_PROCESSOR_IMPLEMENTATION_CLASS);
  }

  public String getTopicPrefixFormat() {
    return config.getString(TOPIC_PREFIX_FORMAT_CONFIG);
  }

  public String getDlqTopicPrefixFormat() {
    return config.getString(DLQ_TOPIC_PREFIX_FORMAT_CONFIG);
  }

  public String getDlqTopicLabel() {
    return config.getString(DLQ_TOPIC_LABEL_CONFIG);
  }

  public String getProjectPrefixFormat() {
    return config.getString(PROJECT_PREFIX_FORMAT_CONFIG);
  }

  public String getTopicPrefixSeparator() {
    return config.getString(TOPIC_PREFIX_SEPARATOR_CONFIG);
  }

  public Boolean shouldOptimizeAcls() {
    return config.getBoolean(OPTIMIZED_ACLS_CONFIG);
  }

  public String getConfluentCloudEnv() {
    return config.getString(CCLOUD_ENV_CONFIG);
  }

  public boolean enabledExperimental() {
    return config.getBoolean(TOPOLOGY_EXPERIMENTAL_ENABLED_CONFIG);
  }

  public boolean useConfluentCloud() {
    return config.hasPath(CCLOUD_ENV_CONFIG);
  }

  public Optional<String> getInternalPrincipalOptional() {
    String internalPrincipal = null;
    if (hasProperty(JULIE_INTERNAL_PRINCIPAL)) {
      internalPrincipal = config.getString(JULIE_INTERNAL_PRINCIPAL);
    } else if (hasProperty(TOPOLOGY_BUILDER_INTERNAL_PRINCIPAL)) {
      internalPrincipal = config.getString(TOPOLOGY_BUILDER_INTERNAL_PRINCIPAL);
    }
    return Optional.ofNullable(internalPrincipal);
  }

  public boolean hasProperty(String property) {
    return config.hasPath(property);
  }

  public List<String> getTopologyValidations() {
    List<String> classes = config.getStringList(TOPOLOGY_VALIDATIONS_CONFIG);
    return classes.stream().map(String::trim).collect(Collectors.toList());
  }

  public boolean enabledConnectorTopicCreateAcl() {
    return config.getBoolean(CONNECTOR_ALLOW_TOPIC_CREATE);
  }

  public boolean isQuiet() {
    return Boolean.parseBoolean(cliParams.getOrDefault(CommandLineInterface.QUIET_OPTION, "false"));
  }

  public boolean doValidate() {
    return Boolean.parseBoolean(
        cliParams.getOrDefault(CommandLineInterface.VALIDATE_OPTION, "false"));
  }

  public boolean isDryRun() {
    return Boolean.parseBoolean(cliParams.getOrDefault(DRY_RUN_OPTION, "false"));
  }

  public FileType getTopologyFileType() {
    return config.getEnum(FileType.class, TOPOLOGY_FILE_TYPE);
  }

  public boolean isAllowDeleteTopics() {
    return config.getBoolean(ALLOW_DELETE_TOPICS);
  }

  public boolean isAllowDeleteBindings() {
    return config.getBoolean(ALLOW_DELETE_BINDINGS);
  }

  public boolean isAllowDeletePrincipals() {
    return config.getBoolean(ALLOW_DELETE_PRINCIPALS);
  }

  public boolean isAllowDeleteConnectArtefacts() {
    return config.getBoolean(ALLOW_DELETE_CONNECT_ARTEFACTS);
  }

  public boolean isAllowDeleteKsqlArtefacts() {
    return config.getBoolean(ALLOW_DELETE_KSQL_ARTEFACTS);
  }

  public boolean enabledPrincipalTranslation() {
    return config.getBoolean(TOPOLOGY_PRINCIPAL_TRANSLATION_ENABLED_CONFIG);
  }

  public boolean fetchStateFromTheCluster() {
    return config.getBoolean(TOPOLOGY_STATE_FROM_CLUSTER);
  }

  public boolean fetchTopicStateFromTheCluster() {
    return fetchStateFromTheCluster() || config.getBoolean(TOPOLOGY_TOPIC_STATE_FROM_CLUSTER);
  }

  public String getS3Bucket() {
    return config.getString(JULIE_S3_BUCKET);
  }

  public String getS3Endpoint() {
    return config.getString(JULIE_S3_ENDPOINT);
  }

  public String getS3Region() {
    return config.getString(JULIE_S3_REGION);
  }

  public String getGCPProjectId() {
    return config.getString(JULIE_GCP_PROJECT_ID);
  }

  public String getGCPBucket() {
    return config.getString(JULIE_GCP_BUCKET);
  }

  public String getMdsServer() {
    return config.getString(MDS_SERVER);
  }

  public String getKafkaClusterId() {
    return config.getString(MDS_KAFKA_CLUSTER_ID_CONFIG);
  }

  public String getSchemaRegistryClusterId() {
    return config.getString(MDS_SR_CLUSTER_ID_CONFIG);
  }

  public String getKafkaConnectClusterId() {
    return config.getString(MDS_KC_CLUSTER_ID_CONFIG);
  }

  public String getKsqlDBClusterID() {
    return config.getString(MDS_KSQLDB_CLUSTER_ID_CONFIG);
  }

  public Optional<String> getSslTrustStoreLocation() {
    try {
      return Optional.of(config.getString(SSL_TRUSTSTORE_LOCATION));
    } catch (ConfigException.Missing missingEx) {
      return Optional.empty();
    }
  }

  public Optional<String> getSslTrustStorePassword() {
    try {
      return Optional.of(config.getString(SSL_TRUSTSTORE_PASSWORD));
    } catch (ConfigException.Missing missingEx) {
      return Optional.empty();
    }
  }

  public Optional<String> getSslKeyStoreLocation() {
    try {
      return Optional.of(config.getString(SSL_KEYSTORE_LOCATION));
    } catch (ConfigException.Missing missingEx) {
      return Optional.empty();
    }
  }

  public Optional<String> getSslKeyStorePassword() {
    try {
      return Optional.of(config.getString(SSL_KEYSTORE_PASSWORD));
    } catch (ConfigException.Missing missingEx) {
      return Optional.empty();
    }
  }

  public Optional<String> getSslKeyPassword() {
    try {
      return Optional.of(config.getString(SSL_KEY_PASSWORD));
    } catch (ConfigException.Missing missingEx) {
      return Optional.empty();
    }
  }

  public Map<String, String> getKafkaConnectServers() {
    List<String> servers = config.getStringList(PLATFORM_SERVERS_CONNECT);
    return servers.stream()
        .map(server -> server.split(":"))
        .map(
            strings -> {
              String key = strings[0].strip();
              String value = String.join(":", Arrays.copyOfRange(strings, 1, strings.length));
              return new Pair<>(key, value);
            })
        .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
  }

  public Map<String, String> getServersBasicAuthMap() {
    List<String> basicAuths = config.getStringList(PLATFORM_SERVERS_BASIC_AUTH);
    return basicAuths.stream()
        .map(s -> s.split("@"))
        .map(
            strings -> {
              String key = strings[0].strip(); // label
              String value = String.join(":", Arrays.copyOfRange(strings, 1, strings.length));
              return new Pair<>(key, value);
            })
        .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
  }

  public KsqlClientConfig getKSQLClientConfig() {
    KsqlClientConfig.Builder ksqlConf =
        new KsqlClientConfig.Builder()
            .setServer(getProperty(PLATFORM_SERVER_KSQL_URL))
            .setTrustStore(
                getPropertyOrNull(PLATFORM_SERVER_KSQL_TRUSTSTORE, SSL_TRUSTSTORE_LOCATION))
            .setTrustStorePassword(
                getPropertyOrNull(PLATFORM_SERVER_KSQL_TRUSTSTORE_PW, SSL_TRUSTSTORE_PASSWORD))
            .setKeyStore(getPropertyOrNull(PLATFORM_SERVER_KSQL_KEYSTORE, SSL_KEYSTORE_LOCATION))
            .setKeyStorePassword(
                getPropertyOrNull(PLATFORM_SERVER_KSQL_KEYSTORE_PW, SSL_KEYSTORE_PASSWORD));
    if (hasProperty(PLATFORM_SERVER_KSQL_ALPN)) {
      ksqlConf.setUseAlpn(config.getBoolean(PLATFORM_SERVER_KSQL_ALPN));
    }
    if (hasProperty(PLATFORM_SERVER_KSQL_BASIC_AUTH_PASSWORD)
        && hasProperty(PLATFORM_SERVER_KSQL_BASIC_AUTH_USER)) {
      ksqlConf.setBasicAuth(
          new BasicAuth(
              getProperty(PLATFORM_SERVER_KSQL_BASIC_AUTH_USER),
              getProperty(PLATFORM_SERVER_KSQL_BASIC_AUTH_PASSWORD)));
    }

    if (hasProperty(PLATFORM_SERVER_KSQL_VERIFY_HOST)) {
      ksqlConf.setVerifyHost(config.getBoolean(PLATFORM_SERVER_KSQL_VERIFY_HOST));
    }
    return ksqlConf.build();
  }

  public boolean hasKSQLServer() {
    return config.hasPath(PLATFORM_SERVER_KSQL_URL);
  }

  private String getPropertyOrNull(String key, String defaultKey) {
    try {
      return config.getString(key);
    } catch (ConfigException.Missing e) {
      if (!defaultKey.isBlank()) {
        return getPropertyOrNull(defaultKey, "");
      }
      return null;
    }
  }

  public Optional<BasicAuth> getMdsBasicAuth() {
    BasicAuth auth = null;
    if (hasProperty(MDS_USER_CONFIG) && hasProperty(MDS_PASSWORD_CONFIG)) {
      auth = new BasicAuth(getProperty(MDS_USER_CONFIG), getProperty(MDS_PASSWORD_CONFIG));
    }
    return Optional.ofNullable(auth);
  }

  public JulieRoles getJulieRoles() throws IOException {
    JulieRolesSerdes serdes = new JulieRolesSerdes();
    try {
      String path = config.getString(JULIE_ROLES);
      return serdes.deserialise(Paths.get(path).toFile());
    } catch (ConfigException.Missing | ConfigException.WrongType ex) {
      LOGGER.debug(ex);
      return new JulieRoles();
    } catch (IOException e) {
      LOGGER.error(e);
      throw e;
    }
  }

  public boolean shouldGenerateDlqTopics() {
    return config.getBoolean(TOPOLOGY_DLQ_TOPICS_GENERATE);
  }

  public List<String> getDlqTopicsAllowList() {
    return config.getStringList(TOPOLOGY_DQL_TOPICS_ALLOW_LIST);
  }

  public List<String> getDlqTopicsDenyList() {
    return config.getStringList(TOPOLOGY_DQL_TOPICS_DENY_LIST);
  }

  public boolean areMultipleContextPerDirEnabled() {
    return config.getBoolean(JULIE_ENABLE_MULTIPLE_CONTEXT_PER_DIR);
  }

  public String getJulieKafkaConfigTopic() {
    return config.getString(JULIE_KAFKA_CONFIG_TOPIC);
  }

  public String getJulieInstanceId() {
    if (!julieInstanceId.isEmpty()) {
      return julieInstanceId;
    }
    try {
      julieInstanceId = config.getString(JULIE_INSTANCE_ID);
    } catch (ConfigException.Missing | ConfigException.WrongType errorType) {
      generateRandomJulieInstanceId();
    }
    return julieInstanceId;
  }

  private String julieInstanceId = "";
  private static final int defaultJulieInstanceIDLength = 10;

  private void generateRandomJulieInstanceId() {
    if (julieInstanceId.isEmpty()) {
      int leftLimit = 97; // letter 'a'
      int rightLimit = 122; // letter 'z'
      Random random = new Random();

      julieInstanceId =
          random
              .ints(leftLimit, rightLimit + 1)
              .limit(defaultJulieInstanceIDLength)
              .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
              .toString();
    }
  }

  public String getKafkaBackendConsumerGroupId() {
    return config.getString(JULIE_KAFKA_CONSUMER_GROUP_ID);
  }

  public Integer getKafkaBackendConsumerRetries() {
    return config.getInt(JULIE_KAFKA_CONSUMER_RETRIES);
  }

  public BasicAuth getConfluentCloudClusterAuth() {
    var user = config.getString(CCLOUD_CLUSTER_API_KEY);
    var pass = config.getString(CCLOUD_CLUSTER_API_SECRET);
    return new BasicAuth(user, pass);
  }

  public BasicAuth getConfluentCloudCloudApiAuth() {
    var user = config.getString(CCLOUD_CLOUD_API_KEY);
    var pass = config.getString(CCLOUD_CLOUD_API_SECRET);
    return new BasicAuth(user, pass);
  }

  public String getConfluentCloudClusterId() {
    return config.getString(CCLOUD_KAFKA_CLUSTER_ID_CONFIG);
  }

  public String getConfluentCloudClusterUrl() {
    return config.getString(CCLOUD_CLUSTER_URL);
  }

  public Boolean enabledPrincipalManagement() {
    return config.getBoolean(JULIE_ENABLE_PRINCIPAL_MANAGEMENT);
  }
}
