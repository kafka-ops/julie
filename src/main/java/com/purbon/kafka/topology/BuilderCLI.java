package com.purbon.kafka.topology;

import static java.lang.System.exit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BuilderCLI {

  private static final Logger LOGGER = LogManager.getLogger(BuilderCLI.class);

  public static final String TOPOLOGY_OPTION = "topology";
  public static final String TOPOLOGY_DESC = "Topology config file.";

  public static final String PLANS_OPTION = "plans";
  public static final String PLANS_DESC = "File describing the predefined plans";

  public static final String BROKERS_OPTION = "brokers";
  public static final String BROKERS_DESC = "The Apache Kafka server(s) to connect to.";

  public static final String ADMIN_CLIENT_CONFIG_OPTION = "clientConfig";
  public static final String ADMIN_CLIENT_CONFIG_DESC = "The AdminClient configuration file.";

  public static final String ALLOW_DELETE_OPTION = "allowDelete";
  public static final String ALLOW_DELETE_DESC =
      "Permits delete operations for topics and configs. (deprecated, to be removed)";

  public static final String DRY_RUN_OPTION = "dryRun";
  public static final String DRY_RUN_DESC = "Print the execution plan without altering anything.";

  public static final String QUIET_OPTION = "quiet";
  public static final String QUIET_DESC = "Print minimum status update";

  public static final String HELP_OPTION = "help";
  public static final String HELP_DESC = "Prints usage information.";

  public static final String VERSION_OPTION = "version";
  public static final String VERSION_DESC = "Prints useful version information.";

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

    final Option plansFileOption =
        Option.builder().longOpt(PLANS_OPTION).hasArg().desc(PLANS_DESC).required(false).build();

    final Option brokersListOption =
        Option.builder()
            .longOpt(BROKERS_OPTION)
            .hasArg()
            .desc(BROKERS_DESC)
            .required(false)
            .build();

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

    final Option helpOption =
        Option.builder().longOpt(HELP_OPTION).hasArg(false).desc(HELP_DESC).required(false).build();

    final Options options = new Options();

    options.addOption(topologyFileOption);
    options.addOption(plansFileOption);
    options.addOption(brokersListOption);
    options.addOption(adminClientConfigFileOption);

    options.addOption(allowDeleteOption);
    options.addOption(dryRunOption);
    options.addOption(quietOption);
    options.addOption(versionOption);
    options.addOption(helpOption);

    return options;
  }

  public static void main(String[] args) throws Exception {
    BuilderCLI cli = new BuilderCLI();
    cli.run(args);
    exit(0);
  }

  public void run(String[] args) throws Exception {
    printHelpOrVersion(args);
    CommandLine cmd = parseArgsOrExit(args);

    Map<String, String> config = parseConfig(cmd);

    processTopology(
        cmd.getOptionValue(TOPOLOGY_OPTION), cmd.getOptionValue(PLANS_OPTION, "default"), config);
    System.out.println("Kafka Topology updated");
  }

  private Map<String, String> parseConfig(CommandLine cmd) {
    Map<String, String> config = new HashMap<>();
    if (cmd.hasOption(BROKERS_OPTION)) {
      config.put(BROKERS_OPTION, cmd.getOptionValue(BROKERS_OPTION));
    }
    config.put(ALLOW_DELETE_OPTION, String.valueOf(cmd.hasOption(ALLOW_DELETE_OPTION)));
    // Add deprecation note for using allow delete CLI option
    if (cmd.hasOption(ALLOW_DELETE_OPTION)) {
      LOGGER.info(
          String.format(
              "CLI option %s is currently deprecated. Expected to be removed in next major version.",
              ALLOW_DELETE_OPTION));
    }
    config.put(DRY_RUN_OPTION, String.valueOf(cmd.hasOption(DRY_RUN_OPTION)));
    config.put(QUIET_OPTION, String.valueOf(cmd.hasOption(QUIET_OPTION)));
    config.put(ADMIN_CLIENT_CONFIG_OPTION, cmd.getOptionValue(ADMIN_CLIENT_CONFIG_OPTION));
    return config;
  }

  private void printHelpOrVersion(String[] args) {

    List<String> listOfArgs = Arrays.asList(args);

    if (listOfArgs.contains("--" + HELP_OPTION)) {
      formatter.printHelp(APP_NAME, options);
      exit(0);
    } else if (listOfArgs.contains("--" + VERSION_OPTION)) {
      System.out.println(KafkaTopologyBuilder.getVersion());
      exit(0);
    }
  }

  private CommandLine parseArgsOrExit(String[] args) {
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

  void processTopology(String topologyFile, String plansFile, Map<String, String> config)
      throws Exception {
    try (KafkaTopologyBuilder builder =
        KafkaTopologyBuilder.build(topologyFile, plansFile, config)) {
      builder.run();
    }
  }
}
