package com.purbon.kafka.topology;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Platform;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.platform.ControlCenter;
import com.purbon.kafka.topology.model.users.platform.ControlCenterInstance;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistry;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import com.purbon.kafka.topology.roles.SimpleAclsProvider;
import com.purbon.kafka.topology.roles.acls.AclsBindingsBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TopologyBuilderAdminClientTest {

  @Mock CreateAclsResult createAclsResult;
  @Mock KafkaFuture<Void> kafkaFuture;
  @Mock AdminClient kafkaAdminClient;
  @Mock Configuration config;

  TopologyBuilderAdminClient adminClient;

  private SimpleAclsProvider aclsProvider;
  private AclsBindingsBuilder bindingsBuilder;
  private ExecutionPlan plan;

  @Mock BackendController backendController;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private AccessControlManager accessControlManager;

  @Before
  public void setup() throws ExecutionException, InterruptedException, IOException {
    adminClient = new TopologyBuilderAdminClient(kafkaAdminClient, config);
    aclsProvider = new SimpleAclsProvider(adminClient);
    bindingsBuilder = new AclsBindingsBuilder(config);
    accessControlManager = new AccessControlManager(aclsProvider, bindingsBuilder);

    plan = ExecutionPlan.init(backendController, System.out);

    doNothing().when(backendController).addBindings(Matchers.anyList());
    doNothing().when(backendController).flushAndClose();

    doReturn("foo").when(config).getConfluentCommandTopic();
    doReturn("foo").when(config).getConfluentMetricsTopic();
    doReturn("foo").when(config).getConfluentMonitoringTopic();

    doReturn(new Object()).when(kafkaFuture).get();
    doReturn(kafkaFuture).when(createAclsResult).all();
    doReturn(createAclsResult).when(kafkaAdminClient).createAcls(anyCollection());
  }

  @Test
  public void newConsumerACLsCreation() throws IOException {

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));
    Project project = new ProjectImpl();
    project.setConsumers(consumers);

    Topic topicA = new TopicImpl("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    accessControlManager.apply(topology, plan);
    plan.run();

    verify(kafkaAdminClient, times(1)).createAcls(anyCollection());
  }

  @Test
  public void newProducerACLsCreation() throws IOException {

    List<Producer> producers = new ArrayList<>();
    producers.add(new Producer("User:app1"));
    Project project = new ProjectImpl();
    project.setProducers(producers);

    Topic topicA = new TopicImpl("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    accessControlManager.apply(topology, plan);
    plan.run();

    verify(kafkaAdminClient, times(1)).createAcls(anyCollection());
  }

  @Test
  public void newKafkaStreamsAppACLsCreation() throws IOException {

    Project project = new ProjectImpl();

    KStream app = new KStream();
    app.setPrincipal("User:App0");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    topics.put(KStream.WRITE_TOPICS, Arrays.asList("topicC", "topicD"));
    app.setTopics(topics);
    project.setStreams(Collections.singletonList(app));

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    accessControlManager.apply(topology, plan);
    plan.run();

    verify(kafkaAdminClient, times(1)).createAcls(anyCollection());
  }

  @Test
  public void newSchemaRegistryACLCreation() throws IOException {

    Project project = new ProjectImpl();
    Topology topology = new TopologyImpl();
    topology.addProject(project);

    Platform platform = new Platform();
    SchemaRegistry sr = new SchemaRegistry();

    SchemaRegistryInstance instance = new SchemaRegistryInstance();
    instance.setPrincipal("User:foo");
    sr.setInstances(Collections.singletonList(instance));

    Map<String, List<User>> rbac = new HashMap<>();
    rbac.put("SecurityAdmin", Collections.singletonList(new User("User:foo")));
    rbac.put("ClusterAdmin", Collections.singletonList(new User("User:bar")));
    sr.setRbac(Optional.of(rbac));

    platform.setSchemaRegistry(sr);
    topology.setPlatform(platform);

    accessControlManager.apply(topology, plan);
    plan.run();

    verify(kafkaAdminClient, times(1)).createAcls(anyCollection());
  }

  @Test
  public void newControlCenterACLCreation() throws IOException {

    Project project = new ProjectImpl();
    Topology topology = new TopologyImpl();
    topology.addProject(project);

    Platform platform = new Platform();
    ControlCenter c3 = new ControlCenter();
    ControlCenterInstance instance = new ControlCenterInstance();
    instance.setPrincipal("User:foo");
    instance.setAppId("appid");
    c3.setInstances(Collections.singletonList(instance));
    platform.setControlCenter(c3);
    topology.setPlatform(platform);

    accessControlManager.apply(topology, plan);
    plan.run();

    verify(kafkaAdminClient, times(1)).createAcls(anyCollection());
  }

  @Test
  public void newKafkaConnectACLsCreation() throws IOException {

    Project project = new ProjectImpl();

    Connector connector1 = new Connector();
    connector1.setPrincipal("User:Connect1");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(Connector.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    connector1.setTopics(topics);

    project.setConnectors(Arrays.asList(connector1));

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    accessControlManager.apply(topology, plan);
    plan.run();

    verify(kafkaAdminClient, times(1)).createAcls(anyCollection());
  }

  @Test
  public void listApplicationTopicsForDefaultOption() throws IOException {
    String topicName = "TopicA";

    ArgumentCaptor<ListTopicsOptions> captor = ArgumentCaptor.forClass(ListTopicsOptions.class);
    ListTopicsResult listTopicsResults = mock(ListTopicsResult.class);

    when(listTopicsResults.names()).thenReturn(getTopicsKafkaFuture(topicName));
    when(kafkaAdminClient.listTopics(captor.capture())).thenReturn(listTopicsResults);
    adminClient.listTopics();

    assertFalse(captor.getValue().shouldListInternal());
  }

  @Test
  public void resolveConfigForNonExistingTopic() throws IOException {
    // new config
    String topicNameA = "topicA";
    Map<String, String> configTopicA = new HashMap<>();
    configTopicA.put("config.key1", "value1");
    Topic topicA = new TopicImpl(topicNameA, "json", configTopicA);

    // which does not exist on the cluster
    when(kafkaAdminClient.describeConfigs(any())).thenThrow(new UnknownTopicOrPartitionException());
    AlterConfigsResult altersConfigResult = getAltersConfigResult();
    when(kafkaAdminClient.incrementalAlterConfigs(any())).thenReturn(altersConfigResult);
    adminClient.updateTopicConfig(topicA, topicNameA, false);
  }

  @Test
  public void resolveConfigForAllTopicsForOverwrite() throws IOException {
    // new config
    String topicNameA = "topicA";
    Map<String, String> configTopicA = new HashMap<>();
    configTopicA.put("config.key1", "value1");
    configTopicA.put("config.key2", "value2-changed");
    Topic topicA = new TopicImpl(topicNameA, "json", configTopicA);

    // existing config from kafka server
    DescribeConfigsResult describeConfigResultMock = mock(DescribeConfigsResult.class);
    ArrayList<ConfigEntry> configEntries = new ArrayList<>();
    configEntries.add(new ConfigEntry("config.key1", "value1"));
    configEntries.add(new ConfigEntry("config.key2", "value2"));
    configEntries.add(new ConfigEntry("config.key3", "willbeunset"));
    when(describeConfigResultMock.all())
        .thenReturn(getConfigResultFuture(configEntries, topicNameA));
    when(kafkaAdminClient.describeConfigs(any())).thenReturn(describeConfigResultMock);
    ArgumentCaptor<Map<ConfigResource, Collection<AlterConfigOp>>> incrementalArgumentCaptor =
        ArgumentCaptor.forClass(Map.class);

    AlterConfigsResult altersConfigResult = getAltersConfigResult();
    when(kafkaAdminClient.incrementalAlterConfigs(incrementalArgumentCaptor.capture()))
        .thenReturn(altersConfigResult);

    Map<String, String> changedConfig = adminClient.updateTopicConfig(topicA, topicNameA, false);

    assertThat(
        "expecting config key to be changed",
        changedConfig.keySet(),
        hasItems("config.key3", "config.key2"));
    assertThat(changedConfig.get("config.key3").trim(), is("[Unset] willbeunset -> ".trim()));
    assertThat(
        changedConfig.get("config.key2").trim(), is("[Update] value2 -> value2-changed".trim()));
  }

  private AlterConfigsResult getAltersConfigResult() {
    AlterConfigsResult alterConfigsResult = mock(AlterConfigsResult.class);
    KafkaFutureImpl<Void> voidKafkaFuture = new KafkaFutureImpl<>();
    voidKafkaFuture.complete(null);
    when(alterConfigsResult.all()).thenReturn(voidKafkaFuture);
    return alterConfigsResult;
  }

  @NotNull
  private KafkaFutureImpl<Set<String>> getTopicsKafkaFuture(String... topicNames) {
    KafkaFutureImpl<Set<String>> kafkaFuture = new KafkaFutureImpl<>();
    kafkaFuture.complete(new HashSet<>(Arrays.asList(topicNames)));
    return kafkaFuture;
  }

  private KafkaFuture<Map<ConfigResource, Config>> getConfigResultFuture(
      List<ConfigEntry> configEntries, String... topicNames) {
    KafkaFutureImpl<Map<ConfigResource, Config>> completedFuture = new KafkaFutureImpl<>();

    Map<ConfigResource, Config> configResourceMap = new HashMap<>();
    Arrays.stream(topicNames)
        .forEach(
            t ->
                configResourceMap.put(
                    new ConfigResource(ConfigResource.Type.TOPIC, t), new Config(configEntries)));
    completedFuture.complete(configResourceMap);

    return completedFuture;
  }

  private KafkaFuture<Map<String, TopicDescription>> getTopicsResultFuture(String topicName) {

    KafkaFutureImpl<Map<String, TopicDescription>> completedFuture = new KafkaFutureImpl<>();
    Map<String, TopicDescription> topicsMap = new HashMap<>();
    topicsMap.put(topicName, new TopicDescription(topicName, false, new ArrayList<>()));

    completedFuture.complete(topicsMap);

    return completedFuture;
  }
}
