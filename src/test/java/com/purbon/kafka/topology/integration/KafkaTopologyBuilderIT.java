package com.purbon.kafka.topology.integration;

import static com.purbon.kafka.topology.BuilderCLI.*;

import com.purbon.kafka.topology.KafkaTopologyBuilder;
import com.purbon.kafka.topology.integration.containerutils.SaslPlaintextKafkaContainer;
import com.purbon.kafka.topology.utils.TestUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class KafkaTopologyBuilderIT {

  private static SaslPlaintextKafkaContainer container;

  @BeforeClass
  public static void setup() {
    container = new SaslPlaintextKafkaContainer();
    container.start();
  }

  @AfterClass
  public static void teardown() {
    container.stop();
  }

  @Test(expected = IOException.class)
  public void testSetupKafkaTopologyBuilderWithWrongCredentialsHC() throws Exception {

    String fileOrDirPath = TestUtils.getResourceFilename("/descriptor.yaml");
    String clientConfigFile = TestUtils.getResourceFilename("/wrong-client-config.properties");

    Map<String, String> config = new HashMap<>();
    config.put(BROKERS_OPTION, container.getBootstrapServers());
    config.put(ALLOW_DELETE_OPTION, "false");
    config.put(DRY_RUN_OPTION, "false");
    config.put(QUIET_OPTION, "true");
    config.put(ADMIN_CLIENT_CONFIG_OPTION, clientConfigFile);

    KafkaTopologyBuilder.build(fileOrDirPath, config);
  }

  @Test
  public void testSetupKafkaTopologyBuilderWithGoodCredentialsHC() throws Exception {

    String fileOrDirPath = TestUtils.getResourceFilename("/descriptor.yaml");
    String clientConfigFile = TestUtils.getResourceFilename("/client-config.properties");

    Map<String, String> config = new HashMap<>();
    config.put(BROKERS_OPTION, container.getBootstrapServers());
    config.put(ALLOW_DELETE_OPTION, "false");
    config.put(DRY_RUN_OPTION, "false");
    config.put(QUIET_OPTION, "true");
    config.put(ADMIN_CLIENT_CONFIG_OPTION, clientConfigFile);

    KafkaTopologyBuilder.build(fileOrDirPath, config);
  }
}
