package com.purbon.kafka.topology;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.exit;

public class BuilderCLI {

  public static final String TOPOLOGY_OPTION = "topology";
  public static final String TOPOLOGY_DESC = "Topology config yaml file.";

  public static final String BROKERS_OPTION = "brokers";
  public static final String BROKERS_DESC = "The Apache Kafka server(s) to connect to.";

  public static final String ADMIN_CLIENT_CONFIG_OPTION = "clientConfig";
  public static final String ADMIN_CLIENT_CONFIG_DESC = "The AdminClient configuration file.";

  public static final String ALLOW_DELETE_OPTION = "allowDelete";
  public static final String ALLOW_DELETE_DESC = "Permits delete operations for topics and configs.";

  public static final String HELP_OPTION = "help";
  public static final String HELP_DESC = "Prints usage information.";

  public static Options buildOptions() {

    final Option topologyFileOption = Option.builder()
        .longOpt(TOPOLOGY_OPTION).hasArg().desc(TOPOLOGY_DESC).required().build();

    final Option brokersListOption = Option.builder()
        .longOpt(BROKERS_OPTION).hasArg().desc(BROKERS_DESC).required().build();

    final Option adminClientConfigFileOption = Option.builder()
        .longOpt(ADMIN_CLIENT_CONFIG_OPTION).hasArg().desc(ADMIN_CLIENT_CONFIG_DESC).required().build();

    final Option allowDeleteOption = Option.builder()
        .longOpt(ALLOW_DELETE_OPTION).hasArg(false).desc(ALLOW_DELETE_DESC).required(false).build();

    final Option helpOption = Option.builder()
        .longOpt(HELP_OPTION).hasArg(false).desc(HELP_DESC).required(false).build();

    final Options options = new Options();
    options.addOption(topologyFileOption);
    options.addOption(brokersListOption);
    options.addOption(adminClientConfigFileOption);
    options.addOption(allowDeleteOption);
    options.addOption(helpOption);

    return options;
  }

  public static void main(String[] args) throws IOException {

    Options options = buildOptions();
    HelpFormatter formatter = new HelpFormatter();
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parseArgsOrExit(parser, options, args, formatter);

    if (cmd.hasOption("help")) {
      formatter.printHelp("kafka-topology-builder", options);
    } else {
      String topology = cmd.getOptionValue(TOPOLOGY_OPTION);
      String brokersList = cmd.getOptionValue(BROKERS_OPTION);
      boolean allowDelete = cmd.hasOption(ALLOW_DELETE_OPTION);
      String adminClientConfigFile = cmd.getOptionValue(ADMIN_CLIENT_CONFIG_OPTION);

      Map<String, String> config = new HashMap<>();
      config.put(BROKERS_OPTION, brokersList);
      config.put(ALLOW_DELETE_OPTION, String.valueOf(allowDelete));
      config.put(ADMIN_CLIENT_CONFIG_OPTION, adminClientConfigFile);
      processTopology(topology, config);
      System.out.println("Kafka Topology updated");
    }
  }

  private static CommandLine parseArgsOrExit(CommandLineParser parser, Options options, String[] args, HelpFormatter formatter) {
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

  private static void processTopology(String topologyFile, Map<String, String> config) throws IOException {
    verifyRequiredParameters(topologyFile, config);
    KafkaTopologyBuilder builder = new KafkaTopologyBuilder(topologyFile, config);
    builder.run();
  }

  private static void verifyRequiredParameters(String topologyFile, Map<String, String> config) throws IOException {
    if (!Files.exists(Paths.get(topologyFile))) {
      throw new IOException("Topology file does not exist");
    }

    String configFilePath = config.get(BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION);

    if (!Files.exists(Paths.get(configFilePath))) {
      throw new IOException("AdminClient config file does not exist");
    }
  }
}
