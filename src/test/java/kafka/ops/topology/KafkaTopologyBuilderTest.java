package kafka.ops.topology;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import kafka.ops.topology.api.adminclient.TopologyBuilderAdminClient;
import kafka.ops.topology.backend.RedisBackend;
import kafka.ops.topology.exceptions.TopologyParsingException;
import kafka.ops.topology.utils.TestUtils;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class KafkaTopologyBuilderTest {

  @Mock TopologyBuilderAdminClient topologyAdminClient;

  @Mock AccessControlProvider accessControlProvider;

  @Mock BindingsBuilderProvider bindingsBuilderProvider;

  @Mock TopicManager topicManager;

  @Mock AccessControlManager accessControlManager;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private Map<String, String> cliOps;
  private Properties props;

  @Before
  public void before() {
    cliOps = new HashMap<>();
    cliOps.put(BuilderCli.BROKERS_OPTION, "");
    cliOps.put(BuilderCli.ADMIN_CLIENT_CONFIG_OPTION, "/fooBar");

    props = new Properties();
    props.put(TopologyBuilderConfig.CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG, "http://foo:8082");
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "");
    props.put(AdminClientConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
  }

  @Test
  public void closeAdminClientTest() throws Exception {
    String fileOrDirPath = TestUtils.getResourceFilename("/descriptor.yaml");

    TopologyBuilderConfig builderConfig = new TopologyBuilderConfig(cliOps, props);

    KafkaTopologyBuilder builder =
        KafkaTopologyBuilder.build(
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
    TopologyBuilderConfig builderConfig = new TopologyBuilderConfig(cliOps, props);

    KafkaTopologyBuilder builder =
        KafkaTopologyBuilder.build(
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

    TopologyBuilderConfig builderConfig = new TopologyBuilderConfig(cliOps, props);
    KafkaTopologyBuilder builder =
        KafkaTopologyBuilder.build(
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

    cliOps.put(BuilderCli.ADMIN_CLIENT_CONFIG_OPTION, clientConfigFile);

    TopologyBuilderConfig builderConfig = new TopologyBuilderConfig(cliOps, props);
    KafkaTopologyBuilder builder =
        KafkaTopologyBuilder.build(
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
    config.put(BuilderCli.BROKERS_OPTION, "localhost:9092");
    config.put(BuilderCli.ALLOW_DELETE_OPTION, "false");
    config.put(BuilderCli.DRY_RUN_OPTION, "true");
    config.put(BuilderCli.QUIET_OPTION, "false");
    config.put(BuilderCli.ADMIN_CLIENT_CONFIG_OPTION, clientConfigFile);

    KafkaTopologyBuilder builder = KafkaTopologyBuilder.build(fileOrDirPath, config);

    builder.setTopicManager(topicManager);
    builder.setAccessControlManager(accessControlManager);

    doNothing().when(topicManager).apply(anyObject(), anyObject());

    doNothing().when(accessControlManager).apply(anyObject(), anyObject());

    builder.run();
    builder.close();

    verify(topicManager, times(1)).apply(anyObject(), anyObject());
    verify(accessControlManager, times(1)).apply(anyObject(), anyObject());
  }

  @Mock RedisBackend stateProcessor;

  @Test
  public void builderRunTestAsFromCLIWithARedisBackend() throws Exception {
    String fileOrDirPath = TestUtils.getResourceFilename("/descriptor.yaml");
    String clientConfigFile = TestUtils.getResourceFilename("/client-config-redis.properties");

    Map<String, String> config = new HashMap<>();
    config.put(BuilderCli.BROKERS_OPTION, "localhost:9092");
    config.put(BuilderCli.ALLOW_DELETE_OPTION, "false");
    config.put(BuilderCli.DRY_RUN_OPTION, "true");
    config.put(BuilderCli.QUIET_OPTION, "false");
    config.put(BuilderCli.ADMIN_CLIENT_CONFIG_OPTION, clientConfigFile);

    KafkaTopologyBuilder builder = KafkaTopologyBuilder.build(fileOrDirPath, config);

    builder.setTopicManager(topicManager);
    builder.setAccessControlManager(accessControlManager);

    doNothing().when(topicManager).apply(anyObject(), anyObject());

    doNothing().when(accessControlManager).apply(anyObject(), anyObject());

    BackendController cs = new BackendController(stateProcessor);
    ExecutionPlan plan = ExecutionPlan.init(cs, System.out);

    builder.run(plan);
    builder.close();

    verify(stateProcessor, times(1)).createOrOpen();
    verify(stateProcessor, times(1)).createOrOpen(BackendController.Mode.TRUNCATE);
    verify(topicManager, times(1)).apply(anyObject(), anyObject());
    verify(accessControlManager, times(1)).apply(anyObject(), anyObject());
  }

  @Test
  public void buiderRunTest() throws Exception {
    String fileOrDirPath = TestUtils.getResourceFilename("/descriptor.yaml");

    TopologyBuilderConfig builderConfig = new TopologyBuilderConfig(cliOps, props);

    KafkaTopologyBuilder builder =
        KafkaTopologyBuilder.build(
            fileOrDirPath,
            builderConfig,
            topologyAdminClient,
            accessControlProvider,
            bindingsBuilderProvider);

    builder.setTopicManager(topicManager);
    builder.setAccessControlManager(accessControlManager);

    doNothing().when(topicManager).apply(anyObject(), anyObject());

    doNothing().when(accessControlManager).apply(anyObject(), anyObject());

    builder.run();
    builder.close();

    verify(topicManager, times(1)).apply(anyObject(), anyObject());
    verify(accessControlManager, times(1)).apply(anyObject(), anyObject());
  }
}
