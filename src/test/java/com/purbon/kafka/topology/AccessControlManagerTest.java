package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.ALLOW_DELETE_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.BROKERS_OPTION;
import static com.purbon.kafka.topology.TopologyBuilderConfig.ALLOW_DELETE_BINDINGS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.ALLOW_DELETE_TOPICS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.KAFKA_INTERNAL_TOPIC_PREFIXES;
import static com.purbon.kafka.topology.TopologyBuilderConfig.OPTIMIZED_ACLS_CONFIG;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.*;

import com.purbon.kafka.topology.actions.Action;
import com.purbon.kafka.topology.api.adminclient.AclBuilder;
import com.purbon.kafka.topology.model.*;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.platform.*;
import com.purbon.kafka.topology.roles.SimpleAclsProvider;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.roles.acls.AclsBindingsBuilder;
import com.purbon.kafka.topology.utils.TestUtils;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class AccessControlManagerTest {

  @Mock SimpleAclsProvider aclsProvider;
  @Mock AclsBindingsBuilder aclsBuilder;

  @Mock BackendController backendController;

  @Mock PrintStream mockPrintStream;

  ExecutionPlan plan;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private AccessControlManager accessControlManager;

  @Before
  public void setup() throws IOException {
    TestUtils.deleteStateFile();
    plan = ExecutionPlan.init(backendController, mockPrintStream);
    accessControlManager = new AccessControlManager(aclsProvider, aclsBuilder);
    doNothing().when(backendController).add(Matchers.anyList());
    doNothing().when(backendController).flushAndClose();
  }

  @Test
  public void newConsumerACLsCreation() {

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));
    Project project = new ProjectImpl();
    project.setConsumers(consumers);

    Topic topicA = new TopicImpl("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    List<Consumer> users = asList(new Consumer("User:app1"));

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .buildBindingsForConsumers(users, topicA.toString(), false);
    accessControlManager.apply(topology, plan);
    verify(aclsBuilder, times(1))
        .buildBindingsForConsumers(eq(users), eq(topicA.toString()), eq(false));
  }

  @Test
  public void newConsumerOptimisedACLsCreation() {

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    Properties props = new Properties();
    props.put(OPTIMIZED_ACLS_CONFIG, true);

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);
    accessControlManager = new AccessControlManager(aclsProvider, aclsBuilder, config);

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));
    Project project = new ProjectImpl();
    project.setConsumers(consumers);

    Topic topicA = new TopicImpl("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    List<Consumer> users = asList(new Consumer("User:app1"));

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .buildBindingsForConsumers(users, project.namePrefix(), true);
    accessControlManager.apply(topology, plan);
    verify(aclsBuilder, times(1))
        .buildBindingsForConsumers(eq(users), eq(project.namePrefix()), eq(true));
  }

  @Test
  public void testConsumerAclsAtTopicLevel() {

    Consumer projectConsumer = new Consumer("project-consumer");
    Consumer topicConsumer = new Consumer("topic-consumer");

    List<Consumer> projectConsumers = singletonList(projectConsumer);

    Topology topology = new TopologyImpl();
    topology.setContext("testConsumerAclsAtTopicLevel");

    Project project = new ProjectImpl("project");
    project.setConsumers(projectConsumers);
    Topic topic = new TopicImpl("foo");

    List<Consumer> topicConsumers = Arrays.asList(projectConsumer, topicConsumer);
    topic.setConsumers(topicConsumers);

    project.addTopic(topic);
    topology.addProject(project);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .buildBindingsForConsumers(any(), eq(topic.toString()), eq(false));

    accessControlManager.apply(topology, plan);

    ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);

    verify(aclsBuilder, times(1))
        .buildBindingsForConsumers(argumentCaptor.capture(), eq(topic.toString()), eq(false));

    List<Consumer> capturedList = argumentCaptor.getValue();
    assertThat(capturedList, hasItem(projectConsumer));
    assertThat(capturedList, hasItem(topicConsumer));
    assertThat(capturedList, hasSize(2));
  }

  @Test
  public void newProducerACLsCreation() {

    List<Producer> producers = new ArrayList<>();
    producers.add(new Producer("User:app1"));
    Project project = new ProjectImpl();
    project.setProducers(producers);

    Topic topicA = new TopicImpl("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .buildBindingsForProducers(producers, topicA.toString(), false);

    accessControlManager.apply(topology, plan);
    verify(aclsBuilder, times(1))
        .buildBindingsForProducers(eq(producers), eq(topicA.toString()), eq(false));
  }

  @Test
  public void newProducerOptimizedACLsCreation() {
    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    Properties props = new Properties();
    props.put(OPTIMIZED_ACLS_CONFIG, true);

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);
    accessControlManager = new AccessControlManager(aclsProvider, aclsBuilder, config);

    List<Producer> producers = new ArrayList<>();
    producers.add(new Producer("User:app1"));
    Project project = new ProjectImpl();
    project.setProducers(producers);

    Topic topicA = new TopicImpl("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .buildBindingsForProducers(producers, project.namePrefix(), true);
    accessControlManager.apply(topology, plan);
    verify(aclsBuilder, times(1))
        .buildBindingsForProducers(eq(producers), eq(project.namePrefix()), eq(true));
  }

  @Test
  public void testProducerAclsAtTopicLevel() {

    Producer projectProducer = new Producer("project-producer");
    Producer topicProducer = new Producer("topic-producer");

    List<Producer> projectProducers = singletonList(projectProducer);

    Topology topology = new TopologyImpl();
    topology.setContext("testProducerAclsAtTopicLevel");

    Project project = new ProjectImpl("project");
    project.setProducers(projectProducers);
    Topic topic = new TopicImpl("foo");

    List<Producer> topicProducers = Arrays.asList(projectProducer, topicProducer);
    topic.setProducers(topicProducers);

    project.addTopic(topic);
    topology.addProject(project);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .buildBindingsForProducers(any(), eq(topic.toString()), eq(false));

    accessControlManager.apply(topology, plan);

    ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);

    verify(aclsBuilder, times(1))
        .buildBindingsForProducers(argumentCaptor.capture(), eq(topic.toString()), eq(false));

    List<Producer> capturedList = argumentCaptor.getValue();
    assertThat(capturedList, hasItem(projectProducer));
    assertThat(capturedList, hasItem(topicProducer));
    assertThat(capturedList, hasSize(2));
  }

  @Test
  public void newKafkaStreamsAppACLsCreation() {

    Project project = new ProjectImpl();

    KStream app = new KStream();
    app.setPrincipal("User:App0");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, asList("topicA", "topicB"));
    topics.put(KStream.WRITE_TOPICS, asList("topicC", "topicD"));
    app.setTopics(topics);
    project.setStreams(singletonList(app));

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    accessControlManager.apply(topology, plan);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .buildBindingsForStreamsApp(
            "User:App0",
            project.namePrefix(),
            topics.get(KStream.READ_TOPICS),
            topics.get(KStream.WRITE_TOPICS));
    verify(aclsBuilder, times(1))
        .buildBindingsForStreamsApp(
            eq("User:App0"),
            eq(project.namePrefix()),
            eq(topics.get(KStream.READ_TOPICS)),
            eq(topics.get(KStream.WRITE_TOPICS)));
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
    sr.setInstances(singletonList(instance));

    Map<String, List<User>> rbac = new HashMap<>();
    rbac.put("SecurityAdmin", singletonList(new User("User:foo")));
    rbac.put("ClusterAdmin", singletonList(new User("User:bar")));
    sr.setRbac(Optional.of(rbac));

    platform.setSchemaRegistry(sr);
    topology.setPlatform(platform);

    accessControlManager.apply(topology, plan);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .buildBindingsForSchemaRegistry(instance);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .setClusterLevelRole(anyString(), anyString(), eq(Component.SCHEMA_REGISTRY));

    verify(aclsBuilder, times(1)).buildBindingsForSchemaRegistry(instance);
    verify(aclsBuilder, times(1))
        .setClusterLevelRole("SecurityAdmin", "User:foo", Component.SCHEMA_REGISTRY);
    verify(aclsBuilder, times(1))
        .setClusterLevelRole("ClusterAdmin", "User:bar", Component.SCHEMA_REGISTRY);
  }

  @Test
  public void newControlCenterACLCreation() {

    Project project = new ProjectImpl();
    Topology topology = new TopologyImpl();
    topology.addProject(project);

    Platform platform = new Platform();
    ControlCenter c3 = new ControlCenter();
    ControlCenterInstance instance = new ControlCenterInstance();
    instance.setPrincipal("User:foo");
    instance.setAppId("appid");
    c3.setInstances(singletonList(instance));
    platform.setControlCenter(c3);
    topology.setPlatform(platform);

    accessControlManager.apply(topology, plan);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .buildBindingsForControlCenter("User:foo", "appid");

    verify(aclsBuilder, times(1)).buildBindingsForControlCenter("User:foo", "appid");
  }

  @Test
  public void newKafkaClusterRBACCreation() throws IOException {
    Project project = new ProjectImpl();
    Topology topology = new TopologyImpl();
    topology.addProject(project);

    Platform platform = new Platform();
    Kafka kafka = new Kafka();
    Map<String, List<User>> rbac = new HashMap<>();
    rbac.put("Operator", singletonList(new User("User:foo")));
    rbac.put("ClusterAdmin", singletonList(new User("User:bar")));
    kafka.setRbac(Optional.of(rbac));
    platform.setKafka(kafka);
    topology.setPlatform(platform);

    accessControlManager.apply(topology, plan);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .setClusterLevelRole(anyString(), anyString(), eq(Component.KAFKA));

    verify(aclsBuilder, times(1)).setClusterLevelRole("Operator", "User:foo", Component.KAFKA);
    verify(aclsBuilder, times(1)).setClusterLevelRole("ClusterAdmin", "User:bar", Component.KAFKA);
  }

  @Test
  public void newKafkaConnectClusterRBACCreation() throws IOException {
    Project project = new ProjectImpl();
    Topology topology = new TopologyImpl();
    topology.addProject(project);

    Platform platform = new Platform();
    KafkaConnect connect = new KafkaConnect();
    Map<String, List<User>> rbac = new HashMap<>();
    rbac.put("Operator", singletonList(new User("User:foo")));
    rbac.put("ClusterAdmin", singletonList(new User("User:bar")));
    connect.setRbac(Optional.of(rbac));
    platform.setKafkaConnect(connect);
    topology.setPlatform(platform);

    accessControlManager.apply(topology, plan);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .setClusterLevelRole(anyString(), anyString(), eq(Component.KAFKA_CONNECT));

    verify(aclsBuilder, times(1))
        .setClusterLevelRole("Operator", "User:foo", Component.KAFKA_CONNECT);
    verify(aclsBuilder, times(1))
        .setClusterLevelRole("ClusterAdmin", "User:bar", Component.KAFKA_CONNECT);
  }

  @Test
  public void newKafkaConnectACLsCreation() {
    Project project = new ProjectImpl();

    Connector connector1 = new Connector();
    connector1.setPrincipal("User:Connect1");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(Connector.READ_TOPICS, asList("topicA", "topicB"));
    connector1.setTopics(topics);

    project.setConnectors(asList(connector1));

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    accessControlManager.apply(topology, plan);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .buildBindingsForConnect(connector1, project.namePrefix());

    verify(aclsBuilder, times(1)).buildBindingsForConnect(eq(connector1), eq(project.namePrefix()));
  }

  @Test
  public void testDryRunMode() throws IOException {

    plan = ExecutionPlan.init(backendController, mockPrintStream);
    Topology topology = new TopologyImpl();
    topology.setContext("foo");

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));

    Project project = new ProjectImpl("project");
    project.setConsumers(consumers);
    topology.addProject(project);

    Topic topicA = new TopicImpl("topicA");
    project.addTopic(topicA);

    List<Consumer> users = asList(new Consumer("User:app1"));

    doReturn(singletonList(new TopologyAclBinding()))
        .when(aclsBuilder)
        .buildBindingsForConsumers(users, topicA.toString(), false);

    accessControlManager.apply(topology, plan);

    plan.run(true);

    verify(mockPrintStream, times(1)).println(any(Action.class));
  }

  @Test
  public void testAclDeleteLogic() throws IOException {

    BackendController backendController = new BackendController();
    backendController.load();
    backendController.reset();

    Properties props = new Properties();
    props.put(KAFKA_INTERNAL_TOPIC_PREFIXES, Arrays.asList("foo.", "_"));

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(ALLOW_DELETE_OPTION, "true");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    plan = ExecutionPlan.init(backendController, mockPrintStream);
    accessControlManager = new AccessControlManager(aclsProvider, aclsBuilder, config);

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));
    consumers.add(new Consumer("User:app2"));

    Topic topicA = new TopicImpl("topicA");

    Topology topology = buildTopology(consumers, asList(topicA));

    List<TopologyAclBinding> bindings = returnAclsForConsumers(consumers, topicA.getName());
    doReturn(bindings)
        .when(aclsBuilder)
        .buildBindingsForConsumers(any(), eq(topicA.toString()), eq(false));

    accessControlManager.apply(topology, plan);
    plan.run();

    verify(aclsBuilder, times(1))
        .buildBindingsForConsumers(refEq(consumers), eq(topicA.toString()), eq(false));

    consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));

    bindings = returnAclsForConsumers(consumers, topicA.getName());
    doReturn(bindings)
        .when(aclsBuilder)
        .buildBindingsForConsumers(any(), eq(topicA.toString()), eq(false));

    Topology newTopology = buildTopology(consumers, asList(topicA));

    accessControlManager.apply(newTopology, plan);
    plan.run();

    List<TopologyAclBinding> bindingsToDelete =
        returnAclsForConsumers(asList(new Consumer("User:app2")), topicA.getName());

    verify(aclsProvider, times(1)).clearBindings(new HashSet<>(bindingsToDelete));
  }

  @Test
  public void testBindingsDeleteWithAllOptionsDisabled() throws IOException {

    BackendController backendController = new BackendController();
    backendController.load();
    backendController.reset();
    plan = ExecutionPlan.init(backendController, mockPrintStream);

    Properties props = new Properties();
    props.put(KAFKA_INTERNAL_TOPIC_PREFIXES, Arrays.asList("foo.", "_"));
    props.put(ALLOW_DELETE_TOPICS, "false");

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(ALLOW_DELETE_OPTION, "false");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    accessControlManager = new AccessControlManager(aclsProvider, aclsBuilder, config);

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));
    consumers.add(new Consumer("User:app2"));

    Topic topicA = new TopicImpl("topicA");

    Topology topology = buildTopology(consumers, singletonList(topicA));

    List<TopologyAclBinding> bindings = returnAclsForConsumers(consumers, topicA.getName());
    doReturn(bindings)
        .when(aclsBuilder)
        .buildBindingsForConsumers(any(), eq(topicA.toString()), eq(false));

    accessControlManager.apply(topology, plan);
    plan.run();

    verify(aclsBuilder, times(1))
        .buildBindingsForConsumers(refEq(consumers), eq(topicA.toString()), eq(false));

    consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));

    bindings = returnAclsForConsumers(consumers, topicA.getName());
    doReturn(bindings)
        .when(aclsBuilder)
        .buildBindingsForConsumers(any(), eq(topicA.toString()), eq(false));

    Topology newTopology = buildTopology(consumers, singletonList(topicA));

    accessControlManager.apply(newTopology, plan);
    plan.run();

    List<TopologyAclBinding> bindingsToDelete =
        returnAclsForConsumers(singletonList(new Consumer("User:app2")), topicA.getName());

    verify(aclsProvider, times(0)).clearBindings(new HashSet<>(bindingsToDelete));
  }

  @Test
  public void testBindingsDeleteWithSpecificConfigEnabled() throws IOException {

    BackendController backendController = new BackendController();
    backendController.load();
    backendController.reset();
    plan = ExecutionPlan.init(backendController, mockPrintStream);

    Properties props = new Properties();
    props.put(KAFKA_INTERNAL_TOPIC_PREFIXES, Arrays.asList("foo.", "_"));
    props.put(ALLOW_DELETE_BINDINGS, "true");

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(ALLOW_DELETE_OPTION, "false");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    accessControlManager = new AccessControlManager(aclsProvider, aclsBuilder, config);

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));
    consumers.add(new Consumer("User:app2"));

    Topic topicA = new TopicImpl("topicA");

    Topology topology = buildTopology(consumers, singletonList(topicA));

    List<TopologyAclBinding> bindings = returnAclsForConsumers(consumers, topicA.getName());
    doReturn(bindings)
        .when(aclsBuilder)
        .buildBindingsForConsumers(any(), eq(topicA.toString()), eq(false));

    accessControlManager.apply(topology, plan);
    plan.run();

    verify(aclsBuilder, times(1))
        .buildBindingsForConsumers(refEq(consumers), eq(topicA.toString()), eq(false));

    consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));

    bindings = returnAclsForConsumers(consumers, topicA.getName());
    doReturn(bindings)
        .when(aclsBuilder)
        .buildBindingsForConsumers(any(), eq(topicA.toString()), eq(false));

    Topology newTopology = buildTopology(consumers, singletonList(topicA));

    accessControlManager.apply(newTopology, plan);
    plan.run();

    List<TopologyAclBinding> bindingsToDelete =
        returnAclsForConsumers(singletonList(new Consumer("User:app2")), topicA.getName());

    verify(aclsProvider, times(1)).clearBindings(new HashSet<>(bindingsToDelete));
  }

  private Topology buildTopology(List<Consumer> consumers, List<Topic> topics) {

    Project project = new ProjectImpl();
    project.setConsumers(consumers);

    for (Topic topic : topics) {
      project.addTopic(topic);
    }

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    return topology;
  }

  private List<TopologyAclBinding> returnAclsForConsumers(List<Consumer> consumers, String topic) {

    List<AclBinding> acls = new ArrayList<>();

    for (Consumer consumer : consumers) {
      acls.add(
          buildTopicLevelAcl(
              consumer.getPrincipal(), topic, PatternType.LITERAL, AclOperation.DESCRIBE));
      acls.add(
          buildTopicLevelAcl(
              consumer.getPrincipal(), topic, PatternType.LITERAL, AclOperation.READ));
      acls.add(
          buildGroupLevelAcl(
              consumer.getPrincipal(),
              consumer.groupString(),
              PatternType.LITERAL,
              AclOperation.READ));
    }

    return acls.stream()
        .map(aclBinding -> new TopologyAclBinding(aclBinding))
        .collect(Collectors.toList());
  }

  private AclBinding buildTopicLevelAcl(
      String principal, String topic, PatternType patternType, AclOperation op) {
    return new AclBuilder(principal)
        .addResource(ResourceType.TOPIC, topic, patternType)
        .addControlEntry("*", op, AclPermissionType.ALLOW)
        .build();
  }

  private AclBinding buildGroupLevelAcl(
      String principal, String group, PatternType patternType, AclOperation op) {
    return new AclBuilder(principal)
        .addResource(ResourceType.GROUP, group, patternType)
        .addControlEntry("*", op, AclPermissionType.ALLOW)
        .build();
  }
}
