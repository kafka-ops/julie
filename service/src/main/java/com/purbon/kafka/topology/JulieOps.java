package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.JulieOpsAuxiliary.buildBackendController;
import static com.purbon.kafka.topology.JulieOpsAuxiliary.configureAndBuildAuditor;
import static com.purbon.kafka.topology.JulieOpsAuxiliary.configureKConnectArtefactManager;
import static com.purbon.kafka.topology.JulieOpsAuxiliary.configureKSqlArtefactManager;
import static com.purbon.kafka.topology.JulieOpsAuxiliary.configureLogsInDebugMode;

import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClientBuilder;
import com.purbon.kafka.topology.api.mds.MDSApiClientBuilder;
import com.purbon.kafka.topology.audit.Auditor;
import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Plan;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import com.purbon.kafka.topology.serviceAccounts.VoidPrincipalProvider;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** JulieOps Main Controller class */
@Getter
@Setter
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

  /**
   * Builder method for the controller
   *
   * @param topologyFile A topology file or directory
   * @param plansFile A file describing the plans to be used with the application
   * @param config A set of configuration properties for the application
   * @return JulieOps
   * @throws Exception Any error detected during the building process
   */
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

  /**
   * Simplified builder method, only used for tests
   *
   * @param topologyFileOrDir A topology file with the desired state description, could be single
   *     file or a directory
   * @param config A set of configuration properties for the application
   * @param adminClient And instance of TopologyBuilderAdminClient to be used for admin ops by
   *     JulieOps
   * @param accessControlProvider An instance of a valid provider for access control rules
   * @param bindingsBuilderProvider An instance of a valid provider for building bindings maps
   * @return JulieOps
   * @throws Exception Any error detected during the building process
   */
  static JulieOps build(
      String topologyFileOrDir,
      Configuration config,
      TopologyBuilderAdminClient adminClient,
      AccessControlProvider accessControlProvider,
      BindingsBuilderProvider bindingsBuilderProvider)
      throws Exception {
    return build(
        topologyFileOrDir,
        Plan.DEFAULT_VALUE,
        config,
        adminClient,
        accessControlProvider,
        bindingsBuilderProvider,
        new VoidPrincipalProvider());
  }

  /** Create an instance of a JulieOps Controller, used to run and manage all inter */
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
    if (plansFile.equals(Plan.DEFAULT_VALUE)) {
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

    configureLogsInDebugMode(config);

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

  void run(BackendController backendController, PrintStream printStream, Auditor auditor)
      throws IOException {
    ExecutionPlan plan = ExecutionPlan.init(backendController, printStream, auditor);
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
    run(buildBackendController(config), outputStream, configureAndBuildAuditor(config));
  }

  public void close() {
    topicManager.close();
  }

  public static String getVersion() {
    InputStream resourceAsStream =
        JulieOps.class.getResourceAsStream(
            "/META-INF/maven/com.purbon.kafka.topology/service/pom.properties");
    Properties prop = new Properties();
    try {
      prop.load(resourceAsStream);
      return prop.getProperty("version");
    } catch (IOException e) {
      e.printStackTrace();
      return "unknown";
    }
  }

  static void verifyRequiredParameters(String topologyFile, Map<String, String> config)
      throws IOException {
    if (!Files.exists(Paths.get(topologyFile))) {
      throw new IOException("Topology file does not exist");
    }

    String configFilePath = config.get(Constants.CLIENT_CONFIG_OPTION);
    if (!Files.exists(Paths.get(configFilePath))) {
      throw new IOException("AdminClient config file does not exist");
    }
  }
}
