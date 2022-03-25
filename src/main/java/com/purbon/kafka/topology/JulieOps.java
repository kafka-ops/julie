package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.Constants.*;

import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClientBuilder;
import com.purbon.kafka.topology.api.connect.KConnectApiClient;
import com.purbon.kafka.topology.api.ksql.KsqlApiClient;
import com.purbon.kafka.topology.api.mds.MDSApiClientBuilder;
import com.purbon.kafka.topology.backend.*;
import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import com.purbon.kafka.topology.serviceAccounts.VoidPrincipalProvider;
import com.purbon.kafka.topology.utils.Pair;
import io.confluent.kafka.schemaregistry.SchemaProvider;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JulieOps implements AutoCloseable {

  private static final Logger LOGGER = LogManager.getLogger(JulieOps.class);

  private TopicManager topicManager;
  private final PrincipalUpdateManager principalUpdateManager;
  private final PrincipalDeleteManager principalDeleteManager;
  private AccessControlManager accessControlManager;
  private KafkaConnectArtefactManager connectorManager;
  private KSqlArtefactManager kSqlArtefactManager;
  private final Map<String, Topology> topologies;
  private final Configuration config;
  private final PrintStream outputStream;

  private JulieOps(
      Map<String, Topology> topologies,
      Configuration config,
      TopicManager topicManager,
      AccessControlManager accessControlManager,
      PrincipalUpdateManager principalUpdateManager,
      PrincipalDeleteManager principalDeleteManager,
      KafkaConnectArtefactManager connectorManager,
      KSqlArtefactManager kSqlArtefactManager) {
    this.topologies = topologies;
    this.config = config;
    this.topicManager = topicManager;
    this.accessControlManager = accessControlManager;
    this.principalUpdateManager = principalUpdateManager;
    this.principalDeleteManager = principalDeleteManager;
    this.connectorManager = connectorManager;
    this.kSqlArtefactManager = kSqlArtefactManager;
    this.outputStream = System.out;
  }

  public static JulieOps build(String topologyFile, Map<String, String> config) throws Exception {
    return build(topologyFile, "default", config);
  }

  public static JulieOps build(String topologyFile, String plansFile, Map<String, String> config)
      throws Exception {

    verifyRequiredParameters(topologyFile, config);
    Configuration builderConfig = Configuration.build(config);
    TopologyBuilderAdminClient adminClient =
        new TopologyBuilderAdminClientBuilder(builderConfig).build();
    AccessControlProviderFactory factory =
        new AccessControlProviderFactory(
            builderConfig, adminClient, new MDSApiClientBuilder(builderConfig));

    PrincipalProviderFactory principalProviderFactory = new PrincipalProviderFactory(builderConfig);

    return build(
        topologyFile,
        plansFile,
        builderConfig,
        adminClient,
        factory.get(),
        factory.builder(),
        principalProviderFactory.get());
  }

  public static JulieOps build(
      String topologyFileOrDir,
      Configuration config,
      TopologyBuilderAdminClient adminClient,
      AccessControlProvider accessControlProvider,
      BindingsBuilderProvider bindingsBuilderProvider)
      throws Exception {
    return build(
        topologyFileOrDir,
        "default",
        config,
        adminClient,
        accessControlProvider,
        bindingsBuilderProvider,
        new VoidPrincipalProvider());
  }

  public static JulieOps build(
      String topologyFileOrDir,
      String plansFile,
      Configuration config,
      TopologyBuilderAdminClient adminClient,
      AccessControlProvider accessControlProvider,
      BindingsBuilderProvider bindingsBuilderProvider,
      PrincipalProvider principalProvider)
      throws Exception {

    Map<String, Topology> topologies;
    if (plansFile.equals("default")) {
      topologies = TopologyObjectBuilder.build(topologyFileOrDir, config);
    } else {
      topologies = TopologyObjectBuilder.build(topologyFileOrDir, plansFile, config);
    }

    TopologyValidator validator = new TopologyValidator(config);

    for (Topology topology : topologies.values()) {
      List<String> validationResults = validator.validate(topology);
      if (!validationResults.isEmpty()) {
        String resultsMessage = String.join("\n", validationResults);
        throw new ValidationException(resultsMessage);
      }
      config.validateWith(topology);
    }

    AccessControlManager accessControlManager =
        new AccessControlManager(
            accessControlProvider, bindingsBuilderProvider, config.getJulieRoles(), config);

    RestService restService = new RestService(config.getConfluentSchemaRegistryUrl());
    Map<String, ?> schemaRegistryConfig = config.asMap();

    List<SchemaProvider> providers =
        Arrays.asList(
            new AvroSchemaProvider(), new JsonSchemaProvider(), new ProtobufSchemaProvider());
    SchemaRegistryClient schemaRegistryClient =
        new CachedSchemaRegistryClient(
            restService,
            10,
            providers,
            schemaRegistryConfig.isEmpty() ? null : schemaRegistryConfig,
            null);
    SchemaRegistryManager schemaRegistryManager =
        new SchemaRegistryManager(schemaRegistryClient, topologyFileOrDir);

    TopicManager topicManager = new TopicManager(adminClient, schemaRegistryManager, config);

    PrincipalUpdateManager principalUpdateManager =
        new PrincipalUpdateManager(principalProvider, config);
    PrincipalDeleteManager principalDeleteManager =
        new PrincipalDeleteManager(principalProvider, config);

    KafkaConnectArtefactManager connectorManager =
        configureKConnectArtefactManager(config, topologyFileOrDir);

    KSqlArtefactManager kSqlArtefactManager =
        configureKSqlArtefactManager(config, topologyFileOrDir);

    return new JulieOps(
        topologies,
        config,
        topicManager,
        accessControlManager,
        principalUpdateManager,
        principalDeleteManager,
        connectorManager,
        kSqlArtefactManager);
  }

  private static KafkaConnectArtefactManager configureKConnectArtefactManager(
      Configuration config, String topologyFileOrDir) {
    Map<String, KConnectApiClient> clients =
        config.getKafkaConnectServers().entrySet().stream()
            .map(
                entry ->
                    new Pair<>(
                        entry.getKey(),
                        new KConnectApiClient(entry.getValue(), entry.getKey(), config)))
            .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

    if (clients.isEmpty()) {
      LOGGER.debug(
          "No KafkaConnect clients configured for JulieOps to use, please verify your config file");
    }

    return new KafkaConnectArtefactManager(clients, config, topologyFileOrDir);
  }

  private static KSqlArtefactManager configureKSqlArtefactManager(
      Configuration config, String topologyFileOrDir) {

    Map<String, KsqlApiClient> clients = new HashMap<>();
    if (config.hasKSQLServer()) {
      KsqlApiClient client = new KsqlApiClient(config.getKSQLClientConfig());
      clients.put("default", client);
    }

    if (clients.isEmpty()) {
      LOGGER.debug(
          "No KSQL clients configured for JulieOps to use, please verify your config file");
    }

    return new KSqlArtefactManager(clients, config, topologyFileOrDir);
  }

  static void verifyRequiredParameters(String topologyFile, Map<String, String> config)
      throws IOException {
    if (!Files.exists(Paths.get(topologyFile))) {
      throw new IOException("Topology file does not exist");
    }

    String configFilePath = config.get(CommandLineInterface.CLIENT_CONFIG_OPTION);
    if (!Files.exists(Paths.get(configFilePath))) {
      throw new IOException("AdminClient config file does not exist");
    }
  }

  void run(BackendController backendController, PrintStream printStream) throws IOException {
    ExecutionPlan plan = ExecutionPlan.init(backendController, printStream);
    LOGGER.debug(
        String.format(
            "Running topology builder with topicManager=[%s], accessControlManager=[%s], dryRun=[%s], isQuiet=[%s]",
            topicManager, accessControlManager, config.isDryRun(), config.isQuiet()));

    // Create users should always be first, so user exists when making acl link
    for (Topology topology : topologies.values()) {
      principalUpdateManager.updatePlan(topology, plan);
    }
    topicManager.updatePlan(plan, topologies);
    accessControlManager.updatePlan(plan, topologies);
    connectorManager.updatePlan(plan, topologies);
    kSqlArtefactManager.updatePlan(plan, topologies);
    // Delete users should always be last,
    // avoids any unlinked acls, e.g. if acl delete or something errors then there is a link still
    // from the account, and can be re-run or manually fixed more easily
    for (Topology topology : topologies.values()) {
      principalDeleteManager.updatePlan(topology, plan);
    }

    plan.run(config.isDryRun());

    if (!config.isQuiet() && !config.isDryRun()) {
      topicManager.printCurrentState(System.out);
      accessControlManager.printCurrentState(System.out);
      principalUpdateManager.printCurrentState(System.out);
      connectorManager.printCurrentState(System.out);
      kSqlArtefactManager.printCurrentState(System.out);
    }
  }

  public void run() throws IOException {
    if (config.doValidate()) {
      return;
    }
    run(buildBackendController(config), outputStream);
  }

  public void close() {
    topicManager.close();
  }

  public static String getVersion() {
    InputStream resourceAsStream =
        JulieOps.class.getResourceAsStream(
            "/META-INF/maven/com.purbon.kafka/julie-ops/pom.properties");
    Properties prop = new Properties();
    try {
      prop.load(resourceAsStream);
      return prop.getProperty("version");
    } catch (IOException e) {
      e.printStackTrace();
      return "unknown";
    }
  }

  private static BackendController buildBackendController(Configuration config) throws IOException {

    String backendClass = config.getStateProcessorImplementationClassName();
    Backend backend;
    try {
      if (backendClass.equalsIgnoreCase(STATE_PROCESSOR_DEFAULT_CLASS)) {
        backend = new FileBackend();
      } else if (backendClass.equalsIgnoreCase(REDIS_STATE_PROCESSOR_CLASS)) {
        backend = new RedisBackend(config);
      } else if (backendClass.equalsIgnoreCase(S3_STATE_PROCESSOR_CLASS)) {
        backend = new S3Backend();
      } else if (backendClass.equalsIgnoreCase(GCP_STATE_PROCESSOR_CLASS)) {
        backend = new GCPBackend();
      } else if (backendClass.equalsIgnoreCase(KAFKA_STATE_PROCESSOR_CLASS)) {
        backend = new KafkaBackend();
      } else {
        throw new IOException(backendClass + " Unknown state processor provided.");
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
    backend.configure(config);
    return new BackendController(backend);
  }

  void setTopicManager(TopicManager topicManager) {
    this.topicManager = topicManager;
  }

  void setAccessControlManager(AccessControlManager accessControlManager) {
    this.accessControlManager = accessControlManager;
  }

  public void setConnectorManager(KafkaConnectArtefactManager connectorManager) {
    this.connectorManager = connectorManager;
  }

  public void setKsqlArtefactManager(KSqlArtefactManager kSqlArtefactManager) {
    this.kSqlArtefactManager = kSqlArtefactManager;
  }
}
