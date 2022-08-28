package com.purbon.kafka.topology.integration;

import static com.purbon.kafka.topology.CommandLineInterface.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.purbon.kafka.topology.JulieOps;
import com.purbon.kafka.topology.integration.containerutils.ContainerFactory;
import com.purbon.kafka.topology.integration.containerutils.SaslPlaintextKafkaContainer;
import com.purbon.kafka.topology.utils.TestUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JulieOpsIT {

  private static SaslPlaintextKafkaContainer container;

  @BeforeAll
  public static void setup() {
    container = ContainerFactory.fetchSaslKafkaContainer(System.getProperty("cp.version"));
    container.start();
  }

  @AfterAll
  public static void teardown() {
    container.stop();
  }

  @Test
  void testSetupKafkaTopologyBuilderWithWrongCredentialsHC() throws Exception {
    assertThrows(IOException.class, () -> {

      String fileOrDirPath = TestUtils.getResourceFilename("/descriptor.yaml");
      String clientConfigFile = TestUtils.getResourceFilename("/wrong-client-config.properties");

      Map<String, String> config = new HashMap<>();
      config.put(BROKERS_OPTION, container.getBootstrapServers());
      config.put(DRY_RUN_OPTION, "false");
      config.put(QUIET_OPTION, "true");
      config.put(CLIENT_CONFIG_OPTION, clientConfigFile);

      JulieOps.build(fileOrDirPath, config);
    });
  }

  @Test
  void testSetupKafkaTopologyBuilderWithGoodCredentialsHC() throws Exception {

    String fileOrDirPath = TestUtils.getResourceFilename("/descriptor.yaml");
    String clientConfigFile = TestUtils.getResourceFilename("/client-config.properties");

    Map<String, String> config = new HashMap<>();
    config.put(BROKERS_OPTION, container.getBootstrapServers());
    config.put(DRY_RUN_OPTION, "false");
    config.put(QUIET_OPTION, "true");
    config.put(CLIENT_CONFIG_OPTION, clientConfigFile);

    JulieOps.build(fileOrDirPath, config);
  }
}
