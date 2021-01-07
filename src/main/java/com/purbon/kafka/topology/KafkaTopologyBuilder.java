package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.TopologyBuilderConfig.REDIS_HOST_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.REDIS_PORT_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.REDIS_STATE_PROCESSOR_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.STATE_PROCESSOR_DEFAULT_CLASS;

import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClientBuilder;
import com.purbon.kafka.topology.api.ccloud.CCloud;
import com.purbon.kafka.topology.api.ccloud.CCloudBuilder;
import com.purbon.kafka.topology.api.mds.MDSApiClientBuilder;
import com.purbon.kafka.topology.backend.FileBackend;
import com.purbon.kafka.topology.backend.RedisBackend;
import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import com.purbon.kafka.topology.serviceAccounts.VoidPrincipalProvider;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KafkaTopologyBuilder implements AutoCloseable {

  private static final Logger LOGGER = LogManager.getLogger(KafkaTopologyBuilder.class);

  private TopicManager topicManager;
  private PrincipalManager principalManager;
  private AccessControlManager accessControlManager;
  private Topology topology;
  private TopologyBuilderConfig config;
  private PrintStream outputStream;

  private KafkaTopologyBuilder(
      Topology topology,
      TopologyBuilderConfig config,
      TopicManager topicManager,
      AccessControlManager accessControlManager,
      PrincipalManager principalManager) {
    this.topology = topology;
    this.config = config;
    this.topicManager = topicManager;
    this.accessControlManager = accessControlManager;
    this.principalManager = principalManager;
    this.outputStream = System.out;
  }

  public static KafkaTopologyBuilder build(String topologyFile, Map<String, String> config)
      throws Exception {
    return build(topologyFile, "default", config);
  }

  public static KafkaTopologyBuilder build(
      String topologyFile, String plansFile, Map<String, String> config) throws Exception {


    TopologyBuilderConfig builderConfig = TopologyBuilderConfig.build(config);
    TopologyBuilderAdminClient adminClient =
        new TopologyBuilderAdminClientBuilder(builderConfig).build();

    CCloudBuilder cCloudFactory = new CCloudBuilder(builderConfig);
    CCloud cCloud = cCloudFactory.build();

    AccessControlProviderFactory factory =
        new AccessControlProviderFactory(
            builderConfig, adminClient, cCloud, new MDSApiClientBuilder(builderConfig));

    PrincipalProviderFactory principalProviderFactory = new PrincipalProviderFactory(builderConfig, cCloud);

    KafkaTopologyBuilder builder =
        build(
            topologyFile,
            plansFile,
            builderConfig,
            adminClient,
            factory.get(),
            factory.builder(),
            principalProviderFactory.get());
    builder.verifyRequiredParameters(topologyFile, config);

    return builder;
  }

  public static KafkaTopologyBuilder build(
      String topologyFileOrDir,
      TopologyBuilderConfig config,
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

  public static KafkaTopologyBuilder build(
      String topologyFileOrDir,
      String plansFile,
      TopologyBuilderConfig config,
      TopologyBuilderAdminClient adminClient,
      AccessControlProvider accessControlProvider,
      BindingsBuilderProvider bindingsBuilderProvider,
      PrincipalProvider principalProvider)
      throws Exception {

    Topology topology;
    if (plansFile.equals("default")) {
      topology = TopologyObjectBuilder.build(topologyFileOrDir, config);
    } else {
      topology = TopologyObjectBuilder.build(topologyFileOrDir, plansFile, config);
    }

    TopologyValidator validator = new TopologyValidator(config);
    List<String> validationResults = validator.validate(topology);
    if (!validationResults.isEmpty()) {
      String resultsMessage = String.join("\n", validationResults);
      throw new ValidationException(resultsMessage);
    }
    config.validateWith(topology);

    AccessControlManager accessControlManager =
        new AccessControlManager(accessControlProvider, bindingsBuilderProvider, config);

    RestService restService = new RestService(config.getConfluentSchemaRegistryUrl());
    Map<String, ?> schemaRegistryConfig = config.asMap();
    SchemaRegistryClient schemaRegistryClient =
        new CachedSchemaRegistryClient(
            restService, 10, schemaRegistryConfig.isEmpty() ? null : schemaRegistryConfig);
    SchemaRegistryManager schemaRegistryManager =
        new SchemaRegistryManager(schemaRegistryClient, topologyFileOrDir);

    TopicManager topicManager = new TopicManager(adminClient, schemaRegistryManager, config);

    PrincipalManager principalManager = new PrincipalManager(principalProvider, config);

    return new KafkaTopologyBuilder(
        topology, config, topicManager, accessControlManager, principalManager);
  }

  void verifyRequiredParameters(String topologyFile, Map<String, String> config)
      throws IOException {
    if (!Files.exists(Paths.get(topologyFile))) {
      throw new IOException("Topology file does not exist");
    }

    String configFilePath = config.get(BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION);

    if (!Files.exists(Paths.get(configFilePath))) {
      throw new IOException("AdminClient config file does not exist");
    }
  }

  void run(ExecutionPlan plan) throws IOException {
    LOGGER.debug(
        String.format(
            "Running topology builder with TopicManager=[%s], accessControlManager=[%s], dryRun=[%s], isQuite=[%s]",
            topicManager, accessControlManager, config.isDryRun(), config.isQuiet()));

    // Create users should always be first, so user exists when making acl link
    principalManager.applyCreate(topology, plan);

    topicManager.apply(topology, plan);
    accessControlManager.apply(topology, plan);

    // Delete users should always be last,
    // avoids any unlinked acls, e.g. if acl delete or something errors then there is a link still
    // from the account, and can be re-run or manually fixed more easily
    principalManager.applyDelete(topology, plan);

    plan.run(config.isDryRun());

    if (!config.isQuiet() && !config.isDryRun()) {
      topicManager.printCurrentState(System.out);
      accessControlManager.printCurrentState(System.out);
      principalManager.printCurrentState(System.out);
    }
  }

  public void run() throws IOException {
    BackendController cs = buildStateProcessor(config);
    ExecutionPlan plan = ExecutionPlan.init(cs, outputStream);
    run(plan);
  }

  public void close() {
    topicManager.close();
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
      return "unknown";
    }
  }

  private static BackendController buildStateProcessor(TopologyBuilderConfig config)
      throws IOException {

    String stateProcessorClass = config.getStateProcessorImplementationClassName();

    try {
      if (stateProcessorClass.equalsIgnoreCase(STATE_PROCESSOR_DEFAULT_CLASS)) {
        return new BackendController(new FileBackend());
      } else if (stateProcessorClass.equalsIgnoreCase(REDIS_STATE_PROCESSOR_CLASS)) {
        String host = config.getProperty(REDIS_HOST_CONFIG);
        int port = Integer.parseInt(config.getProperty(REDIS_PORT_CONFIG));
        return new BackendController(new RedisBackend(host, port));
      } else {
        throw new IOException(stateProcessorClass + " Unknown state processor provided.");
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  void setTopicManager(TopicManager topicManager) {
    this.topicManager = topicManager;
  }

  void setAccessControlManager(AccessControlManager accessControlManager) {
    this.accessControlManager = accessControlManager;
  }
}
