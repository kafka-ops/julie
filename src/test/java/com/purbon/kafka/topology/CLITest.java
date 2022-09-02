package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.CommandLineInterface.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CLITest {

  private CommandLineInterface cli;

  @BeforeEach
  void setup() {
    cli = Mockito.spy(new CommandLineInterface());
  }

  @Test
  void paramPassing() throws Exception {
    String[] args =
        new String[]{
            "--brokers", "localhost:9092",
            "--topology", "descriptor.yaml",
            "--clientConfig", "topology-builder-sasl-plain.properties"
        };

    doNothing().when(cli).processTopology(eq("descriptor.yaml"), eq("default"), anyMap());

    Map<String, String> config = new HashMap<>();
    config.put(BROKERS_OPTION, "localhost:9092");
    config.put(DRY_RUN_OPTION, "false");
    config.put(QUIET_OPTION, "false");
    config.put(VALIDATE_OPTION, "false");
    config.put(CLIENT_CONFIG_OPTION, "topology-builder-sasl-plain.properties");
    config.put(OVERRIDING_CLIENT_CONFIG_OPTION, null);
    cli.run(args);

    verify(cli, times(1)).processTopology(eq("descriptor.yaml"), eq("default"), eq(config));
  }

  @Test
  void dryRun() throws Exception {
    String[] args =
        new String[]{
            "--brokers", "localhost:9092",
            "--topology", "descriptor.yaml",
            "--clientConfig", "topology-builder-sasl-plain.properties",
            "--dryRun"
        };

    doNothing().when(cli).processTopology(eq("descriptor.yaml"), eq("default"), anyMap());

    Map<String, String> config = new HashMap<>();
    config.put(BROKERS_OPTION, "localhost:9092");
    config.put(DRY_RUN_OPTION, "true");
    config.put(QUIET_OPTION, "false");
    config.put(VALIDATE_OPTION, "false");
    config.put(CLIENT_CONFIG_OPTION, "topology-builder-sasl-plain.properties");
    config.put(OVERRIDING_CLIENT_CONFIG_OPTION, null);
    cli.run(args);

    verify(cli, times(1)).processTopology(eq("descriptor.yaml"), eq("default"), eq(config));
  }
}
