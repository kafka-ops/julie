package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.CommandLineInterface.*;
import static com.purbon.kafka.topology.Constants.*;
import static org.mockito.Mockito.*;

import com.purbon.kafka.topology.BackendController.Mode;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.backend.BackendState;
import com.purbon.kafka.topology.backend.RedisBackend;
import com.purbon.kafka.topology.exceptions.TopologyParsingException;
import com.purbon.kafka.topology.utils.TestUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class JulieOpsTest {

  @Mock TopologyBuilderAdminClient topologyAdminClient;

  @Mock AccessControlProvider accessControlProvider;

  @Mock BindingsBuilderProvider bindingsBuilderProvider;

  @Mock TopicManager topicManager;

  @Mock AccessControlManager accessControlManager;

  @Mock KafkaConnectArtefactManager connectorManager;

  @Mock KSqlArtefactManager ksqlArtefactManager;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock RedisBackend stateProcessor;

  private Map<String, String> cliOps;
  private Properties props;

  @Before
  public void before() throws IOException {
    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(CLIENT_CONFIG_OPTION, "/fooBar");

    props = new Properties();
    props.put(CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG, "http://foo:8082");
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "");
    props.put(AdminClientConfig.RETRIES_CONFIG, Integer.MAX_VALUE);

    when(stateProcessor.load()).thenReturn(new BackendState());
  }

  @Test
  public void closeAdminClientTest() throws Exception {
    String fileOrDirPath = TestUtils.getResourceFilename("/descriptor.yaml");

    Configuration builderConfig = new Configuration(cliOps, props);

    JulieOps builder =
        JulieOps.build(
            fileOrDirPath,
            builderConfig,
            topologyAdminClient,
            accessControlProvider,
            bindingsBuilderProvider);

    builder.close();

    verify(topologyAdminClient, times(1)).close();
  }

  @Test(expected = TopologyParsingException.class)
  public void verifyProblematicParametersTest() throws Exception {
    String file = "fileThatDoesNotExist.yaml";
    Configuration builderConfig = new Configuration(cliOps, props);

    JulieOps builder =
        JulieOps.build(
            file,
            builderConfig,
            topologyAdminClient,
            accessControlProvider,
            bindingsBuilderProvider);

    builder.verifyRequiredParameters(file, cliOps);
  }

  @Test(expected = IOException.class)
  public void verifyProblematicParametersTest2() throws Exception {
    String fileOrDirPath = TestUtils.getResourceFilename("/descriptor.yaml");

    Configuration builderConfig = new Configuration(cliOps, props);
    JulieOps builder =
        JulieOps.build(
            fileOrDirPath,
            builderConfig,
            topologyAdminClient,
            accessControlProvider,
            bindingsBuilderProvider);

    builder.verifyRequiredParameters(fileOrDirPath, cliOps);
  }

  @Test
  public void verifyProblematicParametersTestOK() throws Exception {
    String fileOrDirPath = TestUtils.getResourceFilename("/descriptor.yaml");
    String clientConfigFile = TestUtils.getResourceFilename("/client-config.properties");

    cliOps.put(CLIENT_CONFIG_OPTION, clientConfigFile);

    Configuration builderConfig = new Configuration(cliOps, props);
    JulieOps builder =
        JulieOps.build(
            fileOrDirPath,
            builderConfig,
            topologyAdminClient,
            accessControlProvider,
            bindingsBuilderProvider);

    builder.verifyRequiredParameters(fileOrDirPath, cliOps);
  }

  @Test
  public void builderRunTestAsFromCLI() throws Exception {
    String fileOrDirPath = TestUtils.getResourceFilename("/descriptor.yaml");
    String clientConfigFile = TestUtils.getResourceFilename("/client-config.properties");

    Map<String, String> config = new HashMap<>();
    config.put(BROKERS_OPTION, "localhost:9092");
    config.put(DRY_RUN_OPTION, "true");
    config.put(QUIET_OPTION, "false");
    config.put(CLIENT_CONFIG_OPTION, clientConfigFile);

    JulieOps builder = JulieOps.build(fileOrDirPath, config);

    builder.setTopicManager(topicManager);
    builder.setAccessControlManager(accessControlManager);
    builder.setConnectorManager(connectorManager);
    builder.setKsqlArtefactManager(ksqlArtefactManager);

    doNothing().when(topicManager).updatePlan(any(), any());

    doNothing().when(accessControlManager).updatePlan(any(), any());

    builder.run();
    builder.close();

    verify(topicManager, times(1)).updatePlan(any(), any());
    verify(accessControlManager, times(1)).updatePlan(any(), any());
    verify(connectorManager, times(1)).updatePlan(any(), any());
  }

  @Test
  public void builderRunTestAsFromCLIWithARedisBackend() throws Exception {
    String fileOrDirPath = TestUtils.getResourceFilename("/descriptor.yaml");
    String clientConfigFile = TestUtils.getResourceFilename("/client-config-redis.properties");

    Map<String, String> config = new HashMap<>();
    config.put(BROKERS_OPTION, "localhost:9092");
    config.put(DRY_RUN_OPTION, "true");
    config.put(QUIET_OPTION, "false");
    config.put(CLIENT_CONFIG_OPTION, clientConfigFile);

    JulieOps builder = JulieOps.build(fileOrDirPath, config);

    builder.setTopicManager(topicManager);
    builder.setAccessControlManager(accessControlManager);
    builder.setConnectorManager(connectorManager);
    builder.setKsqlArtefactManager(ksqlArtefactManager);

    doNothing().when(topicManager).updatePlan(any(), any());

    doNothing().when(accessControlManager).updatePlan(any(), any());

    builder.run(new BackendController(stateProcessor), System.out);
    builder.close();

    verify(stateProcessor, times(1)).createOrOpen();
    verify(stateProcessor, times(1)).createOrOpen(Mode.TRUNCATE);
    verify(topicManager, times(1)).updatePlan(any(), any());
    verify(accessControlManager, times(1)).updatePlan(any(), any());
  }

  @Test
  public void builderRunTest() throws Exception {
    String fileOrDirPath = TestUtils.getResourceFilename("/descriptor.yaml");

    Configuration builderConfig = new Configuration(cliOps, props);

    JulieOps builder =
        JulieOps.build(
            fileOrDirPath,
            builderConfig,
            topologyAdminClient,
            accessControlProvider,
            bindingsBuilderProvider);

    builder.setTopicManager(topicManager);
    builder.setAccessControlManager(accessControlManager);
    builder.setConnectorManager(connectorManager);
    builder.setKsqlArtefactManager(ksqlArtefactManager);

    doNothing().when(topicManager).updatePlan(any(), any());

    doNothing().when(accessControlManager).updatePlan(any(), any());

    builder.run();
    builder.close();

    verify(topicManager, times(1)).updatePlan(any(), any());
    verify(accessControlManager, times(1)).updatePlan(any(), any());
  }

  @Test
  public void builderRunTestAsFromDirectoryWithSchema() throws Exception {
    String fileOrDirPath = TestUtils.getResourceFilename("/dir_with_subdir");

    Configuration builderConfig = new Configuration(cliOps, props);

    JulieOps builder =
        JulieOps.build(
            fileOrDirPath,
            builderConfig,
            topologyAdminClient,
            accessControlProvider,
            bindingsBuilderProvider);

    builder.setTopicManager(topicManager);
    builder.setAccessControlManager(accessControlManager);
    builder.setKsqlArtefactManager(ksqlArtefactManager);

    doNothing().when(topicManager).updatePlan(any(), any());

    doNothing().when(accessControlManager).updatePlan(any(), any());

    builder.run();
    builder.close();

    verify(topicManager, times(1)).updatePlan(any(), any());
    verify(accessControlManager, times(1)).updatePlan(any(), any());
  }
}
