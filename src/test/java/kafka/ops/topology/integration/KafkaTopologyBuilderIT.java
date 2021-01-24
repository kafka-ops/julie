package kafka.ops.topology.integration;

import kafka.ops.topology.KafkaTopologyBuilder;
import kafka.ops.topology.integration.containerutils.ContainerFactory;
import kafka.ops.topology.integration.containerutils.SaslPlaintextKafkaContainer;
import kafka.ops.topology.utils.TestUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import kafka.ops.topology.BuilderCLI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class KafkaTopologyBuilderIT {

  private static SaslPlaintextKafkaContainer container;

  @BeforeClass
  public static void setup() {
    container = ContainerFactory.fetchSaslKafkaContainer(System.getProperty("cp.version"));
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
    config.put(BuilderCLI.BROKERS_OPTION, container.getBootstrapServers());
    config.put(BuilderCLI.ALLOW_DELETE_OPTION, "false");
    config.put(BuilderCLI.DRY_RUN_OPTION, "false");
    config.put(BuilderCLI.QUIET_OPTION, "true");
    config.put(BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION, clientConfigFile);

    KafkaTopologyBuilder.build(fileOrDirPath, config);
  }

  @Test
  public void testSetupKafkaTopologyBuilderWithGoodCredentialsHC() throws Exception {

    String fileOrDirPath = TestUtils.getResourceFilename("/descriptor.yaml");
    String clientConfigFile = TestUtils.getResourceFilename("/client-config.properties");

    Map<String, String> config = new HashMap<>();
    config.put(BuilderCLI.BROKERS_OPTION, container.getBootstrapServers());
    config.put(BuilderCLI.ALLOW_DELETE_OPTION, "false");
    config.put(BuilderCLI.DRY_RUN_OPTION, "false");
    config.put(BuilderCLI.QUIET_OPTION, "true");
    config.put(BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION, clientConfigFile);

    KafkaTopologyBuilder.build(fileOrDirPath, config);
  }
}
