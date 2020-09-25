package com.purbon.kafka.topology;

import com.purbon.kafka.topology.utils.EnvVarTools;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.admin.AdminClientConfig;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static java.lang.System.exit;

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

  public static final String EXTRACT_PROPS_FROM_ENV_OPTION = "envVarPrefix";
  public static final String EXTRACT_PROPS_FROM_ENV_DESC =
      "Prefix for env vars to to extracted to properties.";

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

    final Option brokersListOption =
        Option.builder()
            .longOpt(BROKERS_OPTION)
            .hasArg()
            .desc(BROKERS_DESC)
            .required(false)
            .build();

    final Option envVarsPrefixOption =
        Option.builder()
            .longOpt(EXTRACT_PROPS_FROM_ENV_OPTION)
            .hasArg()
            .desc(EXTRACT_PROPS_FROM_ENV_DESC)
            .required(false)
            .build();

    final Option adminClientConfigFileOption =
        Option.builder()
            .longOpt(ADMIN_CLIENT_CONFIG_OPTION)
            .hasArg()
            .desc(ADMIN_CLIENT_CONFIG_DESC)
            .required(false)
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
    options.addOption(brokersListOption);
    options.addOption(envVarsPrefixOption);
    options.addOption(adminClientConfigFileOption);

    options.addOption(allowDeleteOption);
    options.addOption(dryRunOption);
    options.addOption(quietOption);
    options.addOption(versionOption);
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
    final CommandLine cmd = parseArgsOrExit(args);

    final String topologyFile = cmd.getOptionValue(TOPOLOGY_OPTION);
    final Map<String, String> config = parseConfig(cmd);
    final Properties properties = parseProperties(cmd);

    validateProperties(properties);

    processTopology(topologyFile, config, properties);
    System.out.println("Kafka Topology updated");
  }

  void processTopology(String topologyFile, Map<String, String> config, Properties properties)
      throws IOException {
    try (KafkaTopologyBuilder builder =
        KafkaTopologyBuilder.build(topologyFile, config, properties)) {
      builder.run();
    }
  }

  private Map<String, String> parseConfig(CommandLine cmd) {
    boolean allowDelete = cmd.hasOption(ALLOW_DELETE_OPTION);
    boolean dryRun = cmd.hasOption(DRY_RUN_OPTION);
    boolean quiet = cmd.hasOption(QUIET_OPTION);

    Map<String, String> config = new HashMap<>();
    config.put(ALLOW_DELETE_OPTION, String.valueOf(allowDelete));
    config.put(DRY_RUN_OPTION, String.valueOf(dryRun));
    config.put(QUIET_OPTION, String.valueOf(quiet));

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

  private void validateProperties(Properties properties) {
    if (!properties.containsKey(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG)) {
      System.err.println("No bootstrap.servers specified.");
      exit(1);
    }
  }

  private CommandLine parseArgsOrExit(String[] args) {
    try {
      return parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println("Parsing failed cause of " + e.getMessage());
      formatter.printHelp("cli", options);
      exit(1);
    }
    return null; // to satisfy compiler
  }

  /**
   * Builds properties from the given command line.
   *
   * <p>Properties are added in the following order, with latter options overwriting earlier ones
   *
   * <p>- loaded from properties file specified using ADMIN_CLIENT_CONFIG_OPTION - add
   * 'bootstrap.servers' from BROKERS_OPTION - if EXTRACT_PROPS_FROM_ENV_OPTION is specified, add
   * all env vars whose name stars with the specified prefix
   *
   * @param cmd
   * @return
   * @throws IOException
   */
  Properties parseProperties(CommandLine cmd) throws IOException {
    final Properties props = new Properties();
    final String adminClientConfigPath = cmd.getOptionValue(ADMIN_CLIENT_CONFIG_OPTION);
    if (adminClientConfigPath != null) {
      props.load(new FileInputStream(adminClientConfigPath));
    }
    final String bootstrapServers = cmd.getOptionValue(BROKERS_OPTION);
    if (bootstrapServers != null) {
      props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    }

    final String envVarPrefix = cmd.getOptionValue(EXTRACT_PROPS_FROM_ENV_OPTION);
    if (envVarPrefix != null) {
      final Map<String, String> envVarsStartingWithPrefix =
          EnvVarTools.getEnvVarsStartingWith(envVarPrefix);
      props.putAll(envVarsStartingWithPrefix);
    }
    return props;
  }
}
