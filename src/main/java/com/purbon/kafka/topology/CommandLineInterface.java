package com.purbon.kafka.topology;

import static java.lang.System.exit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommandLineInterface {

  private static final Logger LOGGER = LogManager.getLogger(CommandLineInterface.class);

  public static final String TOPOLOGY_OPTION = "topology";
  public static final String TOPOLOGY_DESC = "Topology config file.";

  public static final String PLANS_OPTION = "plans";
  public static final String PLANS_DESC = "File describing the predefined plans";

  public static final String BROKERS_OPTION = "brokers";
  public static final String BROKERS_DESC = "The Apache Kafka server(s) to connect to.";

  public static final String CLIENT_CONFIG_OPTION = "clientConfig";
  public static final String CLIENT_CONFIG_DESC = "The client configuration file.";

  public static final String OVERRIDING_CLIENT_CONFIG_OPTION = "overridingClientConfig";
  public static final String OVERRIDING_CLIENT_CONFIG_DESC =
      "The overriding AdminClient configuration file.";

  public static final String DRY_RUN_OPTION = "dryRun";
  public static final String DRY_RUN_DESC = "Print the execution plan without altering anything.";

  public static final String QUIET_OPTION = "quiet";
  public static final String QUIET_DESC = "Print minimum status update";

  public static final String VALIDATE_OPTION = "validate";
  public static final String VALIDATE_DESC = "Only run configured validations in your topology";

  public static final String HELP_OPTION = "help";
  public static final String HELP_DESC = "Prints usage information.";

  public static final String VERSION_OPTION = "version";
  public static final String VERSION_DESC = "Prints useful version information.";

  public static final String APP_NAME = "julie-ops";

  private HelpFormatter formatter;
  private CommandLineParser parser;
  private Options options;

  public CommandLineInterface() {
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

    final Option clientConfigFileOption =
        Option.builder()
            .longOpt(CLIENT_CONFIG_OPTION)
            .hasArg()
            .desc(CLIENT_CONFIG_DESC)
            .required()
            .build();

    final Option overridingAdminClientConfigFileOption =
        Option.builder()
            .longOpt(OVERRIDING_CLIENT_CONFIG_OPTION)
            .hasArg()
            .desc(OVERRIDING_CLIENT_CONFIG_DESC)
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

    final Option validateOption =
        Option.builder()
            .longOpt(VALIDATE_OPTION)
            .hasArg(false)
            .desc(VALIDATE_DESC)
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
    options.addOption(clientConfigFileOption);

    options.addOption(overridingAdminClientConfigFileOption);
    options.addOption(dryRunOption);
    options.addOption(quietOption);
    options.addOption(validateOption);
    options.addOption(versionOption);
    options.addOption(helpOption);

    return options;
  }

  public static void main(String[] args) throws Exception {
    CommandLineInterface cli = new CommandLineInterface();
    cli.run(args);
    exit(0);
  }

  public void run(String[] args) throws Exception {
    printHelpOrVersion(args);
    CommandLine cmd = parseArgsOrExit(args);

    Map<String, String> config = parseConfig(cmd);

    processTopology(
        cmd.getOptionValue(TOPOLOGY_OPTION), cmd.getOptionValue(PLANS_OPTION, "default"), config);
    if (!cmd.hasOption(DRY_RUN_OPTION) && !cmd.hasOption(VALIDATE_OPTION)) {
      System.out.println("Kafka Topology updated");
    }
  }

  private Map<String, String> parseConfig(CommandLine cmd) {
    Map<String, String> config = new HashMap<>();
    if (cmd.hasOption(BROKERS_OPTION)) {
      config.put(BROKERS_OPTION, cmd.getOptionValue(BROKERS_OPTION));
    }
    config.put(DRY_RUN_OPTION, String.valueOf(cmd.hasOption(DRY_RUN_OPTION)));
    config.put(QUIET_OPTION, String.valueOf(cmd.hasOption(QUIET_OPTION)));
    config.put(VALIDATE_OPTION, String.valueOf(cmd.hasOption(VALIDATE_OPTION)));
    config.put(
        OVERRIDING_CLIENT_CONFIG_OPTION, cmd.getOptionValue(OVERRIDING_CLIENT_CONFIG_OPTION));
    config.put(CLIENT_CONFIG_OPTION, cmd.getOptionValue(CLIENT_CONFIG_OPTION));
    config.put(
        OVERRIDING_CLIENT_CONFIG_OPTION, cmd.getOptionValue(OVERRIDING_CLIENT_CONFIG_OPTION));
    return config;
  }

  private void printHelpOrVersion(String[] args) {

    List<String> listOfArgs = Arrays.asList(args);

    if (listOfArgs.contains("--" + HELP_OPTION)) {
      formatter.printHelp(APP_NAME, options);
      exit(0);
    } else if (listOfArgs.contains("--" + VERSION_OPTION)) {
      System.out.println(JulieOps.getVersion());
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
    try (JulieOps builder = JulieOps.build(topologyFile, plansFile, config)) {
      builder.buildAndExecutePlan();
    }
  }
}
