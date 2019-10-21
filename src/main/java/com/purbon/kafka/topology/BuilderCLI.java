package com.purbon.kafka.topology;

import static java.lang.System.exit;

import com.purbon.kafka.topology.model.Topology;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
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
import org.apache.kafka.clients.admin.KafkaAdminClient;

public class BuilderCLI {

  public static Options buildOptions() {

    Option topologyFileOption = OptionBuilder.withArgName( "topology" )
        .hasArg()
        .withDescription(  "use the given topology file" )
        .create( "topologyFile");
    topologyFileOption.setRequired(true);

    Option brokersListOption = OptionBuilder.withArgName( "brokers" )
        .hasArg()
        .withDescription(  "use the given Apache Kafka brokers list" )
        .create( "brokers");
    brokersListOption.setRequired(true);

    Option destroyOption = new Option( "destroy", "Allow delete operations for topics and configs");

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
      String topology = cmd.getOptionValue("topology");
      String [] brokersList = cmd.getOptionValues("brokers");
      boolean allowDelete = cmd.hasOption("destroy");

      Map<String, String> config = new HashMap<>();
      config.put("brokers", StringUtils.join(brokersList, ",") );
      config.put("allow.delete", String.valueOf(allowDelete));
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
      ACLManager aclManager = new ACLManager(builderAdminClient);

      topicManager.syncTopics(topology);
      aclManager.syncAcls(topology);
    }
    finally {
      if (kafkaAdminClient != null)
          kafkaAdminClient.close();
    }
  }

  private static AdminClient buildKafkaAdminClient(Map<String, String> topologyConfig) {
    return AdminClient.create(config(topologyConfig));
  }

  private static Properties config(Map<String, String> config) {
    Properties props = new Properties();
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, config.get("brokers"));
    props.put(AdminClientConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
    return props;
  }


}
