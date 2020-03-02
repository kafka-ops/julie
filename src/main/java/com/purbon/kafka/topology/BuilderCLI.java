package com.purbon.kafka.topology;

import static java.lang.System.exit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class BuilderCLI {

  public static final String BROKERS_OPTION = "brokers";
  public static final String TOPOLOGY_OPTION = "topology";
  public static final String ADMIN_CLIENT_CONFIG_OPTION = "clientConfig";
  public static final String DESTROY_OPTION = "destroy";
  public static final String ALLOW_DELETE_CONFIG = "allowDelete";

  public static Options buildOptions() {

    Option topologyFileOption = OptionBuilder.withArgName(TOPOLOGY_OPTION)
        .hasArg()
        .withDescription(  "topology file" )
        .create(TOPOLOGY_OPTION);
    topologyFileOption.setRequired(true);

    Option brokersListOption = OptionBuilder.withArgName(BROKERS_OPTION)
        .hasArg()
        .withDescription(  "use the given Apache Kafka brokers list" )
        .create(BROKERS_OPTION);
    brokersListOption.setRequired(true);

    Option adminClientConfigFileOption = OptionBuilder.withArgName(ADMIN_CLIENT_CONFIG_OPTION)
        .hasArg()
        .withDescription( "AdminClient configuration file" )
        .create(ADMIN_CLIENT_CONFIG_OPTION);
    brokersListOption.setRequired(true);

    Option destroyOption = new Option(DESTROY_OPTION, "Allow delete operations for topics and configs");

    Option help = new Option( "help", "print this message" );

    Options options = new Options();
    options.addOption(topologyFileOption);
    options.addOption(brokersListOption);
    options.addOption(adminClientConfigFileOption);
    options.addOption(destroyOption);
    options.addOption(help);

    return options;
  }

  public static void main(String [] args) throws IOException {

    HelpFormatter formatter = new HelpFormatter();

    Options options = buildOptions();

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse( options, args);
    } catch (ParseException e) {
      System.out.println( "Parsing failed.  Reason: " + e.getMessage() );
      formatter.printHelp( "cli", options );
      exit(1);
    }

    if (cmd.hasOption("help")) {
      formatter.printHelp( "kafka-topology-builder", options );
    } else {
      String topology = cmd.getOptionValue(TOPOLOGY_OPTION);
      String brokersList = cmd.getOptionValue(BROKERS_OPTION);
      boolean allowDelete = cmd.hasOption(DESTROY_OPTION);
      String adminClientConfigFile = cmd.getOptionValue(ADMIN_CLIENT_CONFIG_OPTION);

      Map<String, String> config = new HashMap<>();
      config.put(BROKERS_OPTION, brokersList );
      config.put(ALLOW_DELETE_CONFIG, String.valueOf(allowDelete));
      config.put(ADMIN_CLIENT_CONFIG_OPTION, adminClientConfigFile);
      processTopology(topology, config);
      System.out.println("Kafka Topology updated");

    }

  }

  private static void processTopology(String topologyFile, Map<String, String> config)
      throws IOException {
    verifyRequiredParameters(topologyFile, config);
    KafkaTopologyBuilder builder = new KafkaTopologyBuilder(topologyFile, config);
    builder.run();

  }


  private static void verifyRequiredParameters(String topologyFile, Map<String, String> config)
      throws IOException {
    if (!Files.exists(Paths.get(topologyFile))) {
      throw new IOException("Topology file does not exist");
    }

    String configFilePath = config.get(BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION);
    if (!Files.exists(Paths.get(configFilePath))) {
      throw new IOException("App Config file does not exist");
    }
  }

}
