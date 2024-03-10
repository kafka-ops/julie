package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.CommandLineInterface.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class CLITest {

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private CommandLineInterface cli;

  @Before
  public void setup() {
    cli = Mockito.spy(new CommandLineInterface());
  }

  @Test
  public void testParamPassing() throws Exception {
    String[] args =
        new String[] {
          "--brokers", "localhost:9092",
          "--topology", "descriptor.yaml",
          "--clientConfig", "topology-builder-sasl-plain.properties"
        };

    doNothing().when(cli).processTopology(eq("descriptor.yaml"), eq("default"), anyMap());

    Map<String, String> config = getDefaultMap();
    config.put(BROKERS_OPTION, "localhost:9092");
    config.put(CLIENT_CONFIG_OPTION, "topology-builder-sasl-plain.properties");
    config.put(OVERRIDING_CLIENT_CONFIG_OPTION, null);
    cli.run(args);

    verify(cli, times(1)).processTopology(eq("descriptor.yaml"), eq("default"), eq(config));
  }

  @Test
  public void testDryRun() throws Exception {
    String[] args =
        new String[] {
          "--brokers", "localhost:9092",
          "--topology", "descriptor.yaml",
          "--clientConfig", "topology-builder-sasl-plain.properties",
          "--dryRun"
        };

    doNothing().when(cli).processTopology(eq("descriptor.yaml"), eq("default"), anyMap());

    Map<String, String> config = getDefaultMap();
    config.put(BROKERS_OPTION, "localhost:9092");
    config.put(DRY_RUN_OPTION, "true");
    config.put(CLIENT_CONFIG_OPTION, "topology-builder-sasl-plain.properties");
    config.put(OVERRIDING_CLIENT_CONFIG_OPTION, null);
    cli.run(args);

    verify(cli, times(1)).processTopology(eq("descriptor.yaml"), eq("default"), eq(config));
  }

  private Map<String, String> getDefaultMap() {
    Map<String, String> map = new HashMap<>();
    map.put(DRY_RUN_OPTION, "false");
    map.put(RECURSIVE_OPTION, "false");
    map.put(QUIET_OPTION, "false");
    map.put(VALIDATE_OPTION, "false");
    map.put(DONT_WARN_FOR_READ_ONLY_STREAMS_OPTION, "false");
    map.put(DONT_WARN_FOR_PROJECTS_WITHOUT_TOPICS_OPTION, "false");
    return map;
  }
}
