package com.purbon.kafka.topology;

import org.apache.commons.cli.CommandLine;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.purbon.kafka.topology.BuilderCLI.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class CLITest {

  @Mock public KafkaTopologyBuilder kafkaTopologyBuilder;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private BuilderCLI cli;

  @Before
  public void setup() {
    cli = Mockito.spy(new BuilderCLI());
  }

  @Test(expected = IOException.class)
  public void testParamPassing() throws IOException {
    String[] args =
        new String[] {
          "--brokers", "localhost:9092",
          "--topology", "descriptor.yaml",
          "--clientConfig", "topology-builder-sasl-plain.properties"
        };

    doNothing().when(cli).processTopology(eq("descriptor.yaml"), anyMap(), eq(new Properties()));

    Map<String, String> config = new HashMap<>();
    config.put(BROKERS_OPTION, "localhost:9092");
    config.put(ALLOW_DELETE_OPTION, "false");
    config.put(DRY_RUN_OPTION, "false");
    config.put(QUIET_OPTION, "false");
    config.put(ADMIN_CLIENT_CONFIG_OPTION, "topology-builder-sasl-plain.properties");
    cli.run(args);

    verify(cli, times(1)).processTopology(eq("descriptor.yaml"), eq(config), eq(new Properties()));
  }

  @Test
  public void testDryRun() throws IOException {
    String[] args =
        new String[] {
          "--brokers", "localhost:9092", "--topology", "example/descriptor.yaml", "--dryRun"
        };

    doNothing().when(cli).processTopology(eq("descriptor.yaml"), anyMap(), eq(new Properties()));

    Map<String, String> config = new HashMap<>();
    config.put(ALLOW_DELETE_OPTION, "false");
    config.put(DRY_RUN_OPTION, "true");
    config.put(QUIET_OPTION, "false");
    cli.run(args);

    final Properties props = new Properties();
    props.put("bootstrap.servers", "localhost:9092");

    verify(cli, times(1)).processTopology(eq("example/descriptor.yaml"), eq(config), eq(props));
  }

  @Test
  public void adminPropertiesAreLoaded() throws IOException {
    final String[] args =
        new String[] {
          "--topology", "example/descriptor.yaml",
          "--clientConfig", "example/topology-builder-sasl-plain.properties"
        };

    final BuilderCLI cli = new BuilderCLI();
    final CommandLine commandLine = cli.parseArgsOrExit(args);
    final Properties properties = cli.parseProperties(commandLine, Collections.EMPTY_MAP);

    assertEquals("PLAIN", properties.get("sasl.mechanism"));
  }

  @Test(expected = FileNotFoundException.class)
  public void nonExistingConfigFileGivesException() throws IOException {
    final String[] args =
        new String[] {
          "--topology", "example/descriptor.yaml",
          "--clientConfig", "youwillneverfindme"
        };
    final BuilderCLI cli = new BuilderCLI();
    final CommandLine commandLine = cli.parseArgsOrExit(args);
    cli.parseProperties(commandLine, Collections.EMPTY_MAP); // should throw
  }

  @Test
  public void envVarShouldBeRenamedAndAdded() throws IOException {
    final String[] args =
        new String[] {
          "--topology", "example/descriptor.yaml",
          "--envVarPrefix", "KTB"
        };

    final Map<String, String> environment = new HashMap<>();
    environment.put("KTB_PROP_A", "prop_a_val");
    environment.put("KTB_PROP___B", "prop_b_val");
    environment.put("OTHER_PROP", "other_val");

    final BuilderCLI cli = new BuilderCLI();
    final CommandLine commandLine = cli.parseArgsOrExit(args);
    final Properties properties = cli.parseProperties(commandLine, environment);
    assertEquals("exactly 2 configs should be picked up", 2, properties.size());
    assertEquals("single underscore parsed correctly", "prop_a_val", properties.get("prop.a"));
    assertEquals("triple underscore parsed correctly", "prop_b_val", properties.get("prop_b"));
  }

  @Test
  public void commandLineBrokerShouldOverridePropertyVal() throws IOException {
    final String[] args =
        new String[] {
          "--topology", "example/descriptor.yaml",
          "--broker", "strange-override:9092",
          "--clientConfig", "example/topology-builder-only-bootstrap.properties"
        };

    final BuilderCLI cli = new BuilderCLI();
    final CommandLine commandLine = cli.parseArgsOrExit(args);
    final Properties properties = cli.parseProperties(commandLine, Collections.EMPTY_MAP);
    assertEquals(
        "command-line broker setting overrides bootstrap.servers property",
        "strange-override:9092",
        properties.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG));
  }

  @Test
  public void envVarShouldOverrideConfigFile() throws IOException {
    final String[] args =
        new String[] {
          "--topology", "example/descriptor.yaml",
          "--clientConfig", "example/topology-builder-sasl-plain.properties",
          "--envVarPrefix", "KTB"
        };

    final Map<String, String> environment =
        Collections.singletonMap("KTB_SASL_MECHANISM", "plain-no-more");

    final BuilderCLI cli = new BuilderCLI();
    final CommandLine commandLine = cli.parseArgsOrExit(args);
    final Properties properties = cli.parseProperties(commandLine, environment);
    assertEquals("plain-no-more", properties.get("sasl.mechanism"));
  }

  @Test
  public void envVarShouldOverrideCommandLineForBroker() throws IOException {
    final String[] args =
        new String[] {
          "--topology", "example/descriptor.yaml",
          "--broker", "original-val:9092",
          "--envVarPrefix", "KTB"
        };

    final Map<String, String> environment =
        Collections.singletonMap("KTB_BOOTSTRAP_SERVERS", "new:9092");

    final BuilderCLI cli = new BuilderCLI();
    final CommandLine commandLine = cli.parseArgsOrExit(args);
    final Properties properties = cli.parseProperties(commandLine, environment);
    assertEquals("new:9092", properties.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG));
  }
}
