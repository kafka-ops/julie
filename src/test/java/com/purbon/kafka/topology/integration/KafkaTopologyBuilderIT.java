package com.purbon.kafka.topology.integration;

import static com.purbon.kafka.topology.BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.ALLOW_DELETE_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.BROKERS_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.DRY_RUN_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.QUIET_OPTION;

import com.purbon.kafka.topology.KafkaTopologyBuilder;
import com.purbon.kafka.topology.integration.containerutils.SaslPlaintextKafkaContainer;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
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

    URL dirOfDescriptors = getClass().getResource("/descriptor.yaml");
    String fileOrDirPath = Paths.get(dirOfDescriptors.toURI()).toFile().toString();

    URL clientConfigURL = getClass().getResource("/wrong-client-config.properties");
    String clientConfigFile = Paths.get(clientConfigURL.toURI()).toFile().toString();

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

    URL dirOfDescriptors = getClass().getResource("/descriptor.yaml");
    String fileOrDirPath = Paths.get(dirOfDescriptors.toURI()).toFile().toString();

    URL clientConfigURL = getClass().getResource("/client-config.properties");
    String clientConfigFile = Paths.get(clientConfigURL.toURI()).toFile().toString();

    Map<String, String> config = new HashMap<>();
    config.put(BROKERS_OPTION, container.getBootstrapServers());
    config.put(ALLOW_DELETE_OPTION, "false");
    config.put(DRY_RUN_OPTION, "false");
    config.put(QUIET_OPTION, "true");
    config.put(ADMIN_CLIENT_CONFIG_OPTION, clientConfigFile);

    KafkaTopologyBuilder.build(fileOrDirPath, config);
  }
}
