package com.purbon.kafka.topology;

import static java.lang.System.exit;

import com.purbon.kafka.topology.model.FlatDescription;
import com.purbon.kafka.topology.serdes.FlatDescriptionSerde;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.admin.AdminClient;

public class BuilderCLI {

  public static final String TOPOLOGY_OPTION = "topology";
  public static final String TOPOLOGY_DESC = "Topology config file.";

  public static final String BROKERS_OPTION = "brokers";
  public static final String BROKERS_DESC = "The Apache Kafka server(s) to connect to.";

  public static final String ADMIN_CLIENT_CONFIG_OPTION = "clientConfig";
  public static final String ADMIN_CLIENT_CONFIG_DESC = "The AdminClient configuration file.";

  public static final String ALLOW_DELETE_OPTION = "allowDelete";
  public static final String ALLOW_DELETE_DESC =
      "Permits delete operations for topics and configs.";

  public static final String DRY_RUN_OPTION = "dryRun";
  public static final String DRY_RUN_DESC = "Print the execution plan without altering anything.";

  public static final String QUIET_OPTION = "quiet";
  public static final String QUIET_DESC = "Print minimum status update";

  public static final String HELP_OPTION = "help";
  public static final String HELP_DESC = "Prints usage information.";

  public static final String VERSION_OPTION = "version";
  public static final String VERSION_DESC = "Prints useful version information.";

  public static final String EXTRACT_OPTION = "extract";
  public static final String EXTRACT_DESC = "Extract to current cluster state and outputs as JSON";

  public static final String RECOVER_OPTION = "recover";
  public static final String RECOVER_DESC = "Recovers previously extracted state";

  public static final String APP_NAME = "kafka-topology-builder";

  private HelpFormatter formatter;
  private CommandLineParser parser;
  private Options options;

  public BuilderCLI() {
    formatter = new HelpFormatter();
    parser = new DefaultParser();
    options = buildOptions();
  }

  private Options buildOptions() {

    final Option topologyFileOption =
        Option.builder().longOpt(TOPOLOGY_OPTION).hasArg().desc(TOPOLOGY_DESC).required().build();

    final Option brokersListOption =
        Option.builder().longOpt(BROKERS_OPTION).hasArg().desc(BROKERS_DESC).required().build();

    final Option adminClientConfigFileOption =
        Option.builder()
            .longOpt(ADMIN_CLIENT_CONFIG_OPTION)
            .hasArg()
            .desc(ADMIN_CLIENT_CONFIG_DESC)
            .required()
            .build();

    final Option allowDeleteOption =
        Option.builder()
            .longOpt(ALLOW_DELETE_OPTION)
            .hasArg(false)
            .desc(ALLOW_DELETE_DESC)
            .required(false)
            .build();

    final Option dryRunOption =
        Option.builder()
            .longOpt(DRY_RUN_OPTION)
            .hasArg(false)
            .desc(DRY_RUN_DESC)
            .required(false)
            .build();

    final Option quietOption =
        Option.builder()
            .longOpt(QUIET_OPTION)
            .hasArg(false)
            .desc(QUIET_DESC)
            .required(false)
            .build();

    final Option versionOption =
        Option.builder()
            .longOpt(VERSION_OPTION)
            .hasArg(false)
            .desc(VERSION_DESC)
            .required(false)
            .build();

    final Option recoverOption =
        Option.builder()
            .longOpt(RECOVER_OPTION)
            .hasArg(true)
            .desc(RECOVER_DESC)
            .required(false)
            .build();

    final Option extractOption =
        Option.builder()
            .longOpt(EXTRACT_OPTION)
            .hasArg(false)
            .desc(EXTRACT_DESC)
            .required(false)
            .build();

    final Option helpOption =
        Option.builder().longOpt(HELP_OPTION).hasArg(false).desc(HELP_DESC).required(false).build();

    final Options options = new Options();

    options.addOption(topologyFileOption);
    options.addOption(brokersListOption);
    options.addOption(adminClientConfigFileOption);

    options.addOption(allowDeleteOption);
    options.addOption(dryRunOption);
    options.addOption(quietOption);
    options.addOption(versionOption);

    options.addOption(extractOption);
    options.addOption(recoverOption);

    options.addOption(helpOption);

    return options;
  }

  public static void main(String[] args) throws IOException {

    BuilderCLI cli = new BuilderCLI();
    cli.run(args);
    exit(0);
  }

  public void run(String[] args) throws IOException {
    printHelpOrVersion(args);
    CommandLine cmd = parseArgsOrExit(args);

    String topology = cmd.getOptionValue(TOPOLOGY_OPTION);
    Map<String, String> config = parseConfig(cmd);

    if (config.get(EXTRACT_OPTION).equals("true")) {
      config.put("bootstrap.servers", config.get(BROKERS_OPTION));
      try (AdminClient adminClient = AdminClient.create((Map) config)) {
        final FlatDescription flatDescription = FlatDescription.extractFromCluster(adminClient);
        System.out.println(FlatDescriptionSerde.convertToJsonString(flatDescription, true));
      }
    } else if (config.get(RECOVER_OPTION) != null) {
      recoverFromFlatDescription(config.get(RECOVER_OPTION), config);
    } else {
      processTopology(topology, config);

      System.out.println("Kafka Topology updated");
    }
  }

  private void recoverFromFlatDescription(
      String pathToFlatDescription, Map<String, String> config) {
    final File file = new File(pathToFlatDescription);
    final FlatDescription desiredState = FlatDescriptionSerde.convertFromJsonFile(file);

    final FDManager manager = new FDManager();
    final boolean allowDeletes = Boolean.valueOf(config.get(ALLOW_DELETE_OPTION));
    // TODO: check if this is really the right UpdateAction
    // for a recover case, we might want to override all settings
    config.put("bootstrap.servers", config.get(BROKERS_OPTION));
    final AdminClient adminClient = AdminClient.create((Map) config);
    final FlatDescription currentState = FlatDescription.extractFromCluster(adminClient);
    final List<FDManager.AbstractAction> actions =
        manager.generatePlan(
            currentState, desiredState, allowDeletes, new FDManager.IncrementalUpdateNoCheck());
    TopologyBuilderConfig builderConfig = new TopologyBuilderConfig(config);
    actions.forEach(
        action -> {
          // TODO: we need a better solution on how to deal with partial failures during execution
          if (builderConfig.isDryRun()) {
            System.out.println(action);
          } else {
            action.run(adminClient);
          }
        });
    exit(0);
  }

  public Map<String, String> parseConfig(CommandLine cmd) {
    String brokersList = cmd.getOptionValue(BROKERS_OPTION);
    boolean allowDelete = cmd.hasOption(ALLOW_DELETE_OPTION);
    boolean dryRun = cmd.hasOption(DRY_RUN_OPTION);
    boolean quiet = cmd.hasOption(QUIET_OPTION);
    String adminClientConfigFile = cmd.getOptionValue(ADMIN_CLIENT_CONFIG_OPTION);

    Map<String, String> config = new HashMap<>();
    config.put(BROKERS_OPTION, brokersList);
    config.put(ALLOW_DELETE_OPTION, String.valueOf(allowDelete));
    config.put(DRY_RUN_OPTION, String.valueOf(dryRun));
    config.put(QUIET_OPTION, String.valueOf(quiet));
    config.put(ADMIN_CLIENT_CONFIG_OPTION, adminClientConfigFile);
    config.put(EXTRACT_OPTION, String.valueOf(cmd.hasOption(EXTRACT_OPTION)));
    config.put(RECOVER_OPTION, cmd.getOptionValue(RECOVER_OPTION));
    return config;
  }

  public void printHelpOrVersion(String[] args) {

    List<String> listOfArgs = Arrays.asList(args);

    if (listOfArgs.contains("--" + HELP_OPTION)) {
      formatter.printHelp(APP_NAME, options);
      exit(0);
    } else if (listOfArgs.contains("--" + VERSION_OPTION)) {
      System.out.println(KafkaTopologyBuilder.getVersion());
      exit(0);
    }
  }

  public CommandLine parseArgsOrExit(String[] args) {
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println("Parsing failed cause of " + e.getMessage());
      formatter.printHelp("cli", options);
      exit(1);
    }
    return cmd;
  }

  public void processTopology(String topologyFile, Map<String, String> config) throws IOException {
    try (KafkaTopologyBuilder builder = KafkaTopologyBuilder.build(topologyFile, config)) {
      builder.run();
    }
  }
}
