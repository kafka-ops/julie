package com.purbon.kafka.topology;

import static java.lang.System.exit;

import com.purbon.kafka.topology.api.mds.MDSApiClientBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.*;

public class BuilderCLI {

  public static final String TOPOLOGY_OPTION = "topology";
  public static final String TOPOLOGY_DESC = "Topology config file.";

  public static final String EXTRACT_TOPOLOGY_OPTION = "extractTopology";
  public static final String EXTRACT_TOPOLOGY_DESC = "Extract current Topology to file.";

  public static final String BROKERS_OPTION = "brokers";
  public static final String BROKERS_DESC = "The Apache Kafka server(s) to connect to.";

  public static final String ADMIN_CLIENT_CONFIG_OPTION = "clientConfig";
  public static final String ADMIN_CLIENT_CONFIG_DESC = "The AdminClient configuration file.";

  public static final String ALLOW_DELETE_OPTION = "allowDelete";
  public static final String ALLOW_DELETE_DESC =
      "Permits delete operations for topics and configs.";

  public static final String QUITE_OPTION = "quite";
  public static final String QUITE_DESC = "Print minimum status update";

  public static final String HELP_OPTION = "help";
  public static final String HELP_DESC = "Prints usage information.";

  public static final String VERSION_OPTION = "version";
  public static final String VERSION_DESC = "Prints useful version information.";

  public static final String APP_NAME = "kafka-topology-builder";

  public static Options buildOptions() {

    final Option topologyFileOption =
        Option.builder().longOpt(TOPOLOGY_OPTION).hasArg().desc(TOPOLOGY_DESC).required().build();

    final Option extractTopologyFileOption =
        Option.builder()
            .longOpt(EXTRACT_TOPOLOGY_OPTION)
            .hasArg()
            .desc(EXTRACT_TOPOLOGY_DESC)
            .required(false)
            .build();

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

    final Option quiteOption =
        Option.builder()
            .longOpt(QUITE_OPTION)
            .hasArg(false)
            .desc(QUITE_DESC)
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
    options.addOption(extractTopologyFileOption);
    options.addOption(brokersListOption);
    options.addOption(adminClientConfigFileOption);

    options.addOption(allowDeleteOption);
    options.addOption(quiteOption);
    options.addOption(versionOption);
    options.addOption(helpOption);

    return options;
  }

  public static void main(String[] args) throws IOException {

    Options options = buildOptions();
    HelpFormatter formatter = new HelpFormatter();
    CommandLineParser parser = new DefaultParser();

    printHelpOrVersion(parser, options, args, formatter);

    CommandLine cmd = parseArgsOrExit(parser, options, args, formatter);

    String topology = cmd.getOptionValue(TOPOLOGY_OPTION);
    String extractTopology = cmd.getOptionValue(EXTRACT_TOPOLOGY_OPTION);

    String brokersList = cmd.getOptionValue(BROKERS_OPTION);
    boolean allowDelete = cmd.hasOption(ALLOW_DELETE_OPTION);
    boolean quite = cmd.hasOption(QUITE_OPTION);
    String adminClientConfigFile = cmd.getOptionValue(ADMIN_CLIENT_CONFIG_OPTION);

    Map<String, String> config = new HashMap<>();
    config.put(BROKERS_OPTION, brokersList);
    config.put(ALLOW_DELETE_OPTION, String.valueOf(allowDelete));
    config.put(QUITE_OPTION, String.valueOf(quite));
    config.put(ADMIN_CLIENT_CONFIG_OPTION, adminClientConfigFile);

    List<String> listOfArgs = Arrays.asList(args);
    if (listOfArgs.contains("--" + EXTRACT_TOPOLOGY_OPTION)) {
      extractTopology(topology, config);
      System.out.println("Kafka Topology extracted");
    } else {
      processTopology(topology, config);
      System.out.println("Kafka Topology updated");
    }

    exit(0);
  }

  private static void printHelpOrVersion(
      CommandLineParser parser, Options options, String[] args, HelpFormatter formatter) {

    List<String> listOfArgs = Arrays.asList(args);

    if (listOfArgs.contains("--" + HELP_OPTION)) {
      formatter.printHelp(APP_NAME, options);
      exit(0);
    } else if (listOfArgs.contains("--" + VERSION_OPTION)) {
      System.out.println(KafkaTopologyBuilder.getVersion());
      exit(0);
    }
  }

  private static CommandLine parseArgsOrExit(
      CommandLineParser parser, Options options, String[] args, HelpFormatter formatter) {
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

  private static void processTopology(String topologyFile, Map<String, String> config)
      throws IOException {
    verifyRequiredParameters(topologyFile, config);

    TopologyBuilderConfig builderConfig = new TopologyBuilderConfig(config);
    TopologyBuilderAdminClient adminClient =
        new TopologyBuilderAdminClientBuilder(builderConfig).build();
    AccessControlProviderFactory accessControlProviderFactory =
        new AccessControlProviderFactory(
            builderConfig, adminClient, new MDSApiClientBuilder(builderConfig));

    KafkaTopologyBuilder builder =
        new KafkaTopologyBuilder(
            topologyFile, builderConfig, adminClient, accessControlProviderFactory.get());

    try {
      builder.run();
    } finally {
      adminClient.close();
    }
  }

  private static void extractTopology(String topologyFile, Map<String, String> config)
      throws IOException {
    verifyRequiredParameters(topologyFile, config);

    TopologyBuilderConfig builderConfig = new TopologyBuilderConfig(config);
    TopologyBuilderAdminClient adminClient =
        new TopologyBuilderAdminClientBuilder(builderConfig).build();
    AccessControlProviderFactory accessControlProviderFactory =
        new AccessControlProviderFactory(
            builderConfig, adminClient, new MDSApiClientBuilder(builderConfig));

    KafkaTopologyBuilder builder =
        new KafkaTopologyBuilder(
            topologyFile, builderConfig, adminClient, accessControlProviderFactory.get());

    try {
      System.out.println("Kafka Topology extraction YET TO BE IMPLEMENTED!");

      // TODO
      // builder.extract();

    } finally {
      adminClient.close();
    }
  }

  private static void verifyRequiredParameters(String topologyFile, Map<String, String> config)
      throws IOException {
    if (!Files.exists(Paths.get(topologyFile))) {
      throw new IOException("Topology file does not exist");
    }

    String configFilePath = config.get(BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION);

    if (!Files.exists(Paths.get(configFilePath))) {
      throw new IOException("AdminClient config file does not exist");
    }
  }
}
