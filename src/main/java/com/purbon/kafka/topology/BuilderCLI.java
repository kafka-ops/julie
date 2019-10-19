package com.purbon.kafka.topology;

import static java.lang.System.exit;

import java.nio.file.Watchable;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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

    Option help = new Option( "help", "print this message" );


    Options options = new Options();
    options.addOption(topologyFileOption);
    options.addOption(brokersListOption);
    options.addOption(help);

    return options;
  }

  public static void main(String [] args) {

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

    }

  }
}
