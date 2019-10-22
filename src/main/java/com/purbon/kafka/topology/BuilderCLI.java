package com.purbon.kafka.topology;

import static java.lang.System.exit;

import com.purbon.kafka.topology.model.Topology;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;

public class BuilderCLI {

  public static final String BROKERS_OPTION = "brokers";
  public static final String TOPOLOGY_OPTION = "topology";
  public static final String DESTROY_OPTION = "destroy";
  public static final String ALLOW_DELETE_CONFIG = "allow.delete";

  public static Options buildOptions() {

    Option topologyFileOption = Option.builder("topologyFile")
        .argName(TOPOLOGY_OPTION)
        .desc("use the given topology file")
        .required()
        .build();

    Option brokersListOption = OptionBuilder.withArgName(BROKERS_OPTION)
        .hasArg()
        .withDescription(  "use the given Apache Kafka brokers list" )
        .create(BROKERS_OPTION);
    brokersListOption.setRequired(true);

    Option destroyOption = new Option(DESTROY_OPTION, "Allow delete operations for topics and configs");

    Option help = new Option( "help", "print this message" );


    Options options = new Options();
    options.addOption(topologyFileOption);
    options.addOption(brokersListOption);
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
      String [] brokersList = cmd.getOptionValues(BROKERS_OPTION);
      boolean allowDelete = cmd.hasOption(DESTROY_OPTION);

      Map<String, String> config = new HashMap<>();
      config.put(BROKERS_OPTION, StringUtils.join(brokersList, ",") );
      config.put(ALLOW_DELETE_CONFIG, String.valueOf(allowDelete));
      processTopology(topology, config);

    }

  }

  private static void processTopology(String topologyFile, Map<String, String> config)
      throws IOException {

    TopologySerdes parser = new TopologySerdes();
    Topology topology = parser.deserialise(new File(topologyFile));

    AdminClient kafkaAdminClient = null;

    try {
      kafkaAdminClient = buildKafkaAdminClient(config);
      TopologyBuilderAdminClient builderAdminClient = new TopologyBuilderAdminClient(
          kafkaAdminClient);
      TopicManager topicManager = new TopicManager(builderAdminClient);
      AclsManager aclsManager = new AclsManager(builderAdminClient);

      topicManager.syncTopics(topology);
      aclsManager.syncAcls(topology);
    }
    finally {
      if (kafkaAdminClient != null) {
        kafkaAdminClient.close();
      }
    }
  }

  private static AdminClient buildKafkaAdminClient(Map<String, String> topologyConfig) {
    return AdminClient.create(config(topologyConfig));
  }

  private static Properties config(Map<String, String> config) {
    final Properties props = new Properties();
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, config.get(BROKERS_OPTION));
    props.put(AdminClientConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
    return props;
  }


}
