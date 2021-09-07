package com.purbon.kafka.topology;

import static com.google.common.collect.Lists.newArrayList;
import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.Constants.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.purbon.kafka.topology.actions.Action;
import com.purbon.kafka.topology.api.adminclient.AclBuilder;
import com.purbon.kafka.topology.model.*;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.users.*;
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
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class AccessControlManagerTest {

  @Mock SimpleAclsProvider aclsProvider;
  @Mock AclsBindingsBuilder aclsBuilder;

  @Mock BackendController backendController;

  @Mock PrintStream mockPrintStream;
  @Mock Configuration config;

  ExecutionPlan plan;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private AccessControlManager accessControlManager;

  @Before
  public void setup() throws IOException {
    TestUtils.deleteStateFile();
    plan = ExecutionPlan.init(backendController, mockPrintStream);
    accessControlManager = new AccessControlManager(aclsProvider, aclsBuilder);
    doNothing().when(backendController).addBindings(ArgumentMatchers.anyList());
    doNothing().when(backendController).flushAndClose();
  }

  @Test
  public void newConsumerACLsCreation() throws IOException {
    Topic topicA = new TopicImpl("topicA");
    TestTopologyBuilder builder =
        TestTopologyBuilder.createProject().addTopic(topicA).addConsumer("User:app1");
    List<Consumer> users = newArrayList(builder.getConsumers());

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .buildBindingsForConsumers(users, topicA.toString(), false);
    accessControlManager.updatePlan(plan, builder.buildTopology());
    verify(aclsBuilder, times(1))
        .buildBindingsForConsumers(eq(users), eq(topicA.toString()), eq(false));
  }

  @Test
  public void newConsumerOptimisedACLsCreation() throws IOException {

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    Properties props = new Properties();
    props.put(OPTIMIZED_ACLS_CONFIG, true);

    Configuration config = new Configuration(cliOps, props);
    accessControlManager = new AccessControlManager(aclsProvider, aclsBuilder, config);

    TestTopologyBuilder builder =
        TestTopologyBuilder.createProject().addTopic("topicA").addConsumer("User:app1");
    List<Consumer> users = newArrayList(builder.getConsumers());

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .buildBindingsForConsumers(users, builder.getProject().namePrefix(), true);
    accessControlManager.updatePlan(plan, builder.buildTopology());
    verify(aclsBuilder, times(1))
        .buildBindingsForConsumers(eq(users), eq(builder.getProject().namePrefix()), eq(true));
  }

  @Test
  public void testConsumerAclsAtTopicLevel() throws IOException {

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

    accessControlManager.updatePlan(plan, topology);

    ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);

    verify(aclsBuilder, times(1))
        .buildBindingsForConsumers(argumentCaptor.capture(), eq(topic.toString()), eq(false));

    List<Consumer> capturedList = argumentCaptor.getValue();
    assertThat(capturedList, hasItem(projectConsumer));
    assertThat(capturedList, hasItem(topicConsumer));
    assertThat(capturedList, hasSize(2));
  }

  @Test
  public void newProducerACLsCreation() throws IOException {
    Topic topicA = new TopicImpl("topicA");
    TestTopologyBuilder builder =
        TestTopologyBuilder.createProject().addTopic(topicA).addProducer("User:app1");
    List<Producer> producers = newArrayList(builder.getProducers());

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .buildBindingsForProducers(producers, topicA.toString(), false);

    accessControlManager.updatePlan(plan, builder.buildTopology());
    verify(aclsBuilder, times(1))
        .buildBindingsForProducers(eq(producers), eq(topicA.toString()), eq(false));
  }

  @Test
  public void newProducerOptimizedACLsCreation() throws IOException {
    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    Properties props = new Properties();
    props.put(OPTIMIZED_ACLS_CONFIG, true);

    Configuration config = new Configuration(cliOps, props);
    accessControlManager = new AccessControlManager(aclsProvider, aclsBuilder, config);

    TestTopologyBuilder builder =
        TestTopologyBuilder.createProject().addTopic("topicA").addProducer("User:app1");
    List<Producer> producers = newArrayList(builder.getProducers());

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .buildBindingsForProducers(producers, builder.getProject().namePrefix(), true);
    accessControlManager.updatePlan(plan, builder.buildTopology());
    verify(aclsBuilder, times(1))
        .buildBindingsForProducers(eq(producers), eq(builder.getProject().namePrefix()), eq(true));
  }

  @Test
  public void testProducerAclsAtTopicLevel() throws IOException {

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

    accessControlManager.updatePlan(plan, topology);

    ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);

    verify(aclsBuilder, times(1))
        .buildBindingsForProducers(argumentCaptor.capture(), eq(topic.toString()), eq(false));

    List<Producer> capturedList = argumentCaptor.getValue();
    assertThat(capturedList, hasItem(projectProducer));
    assertThat(capturedList, hasItem(topicProducer));
    assertThat(capturedList, hasSize(2));
  }

  @Test
  public void newKafkaStreamsAppACLsCreation() throws IOException {

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

    accessControlManager.updatePlan(plan, topology);

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
  public void newKSqlApplicationCreation() throws IOException {
    Project project = new ProjectImpl();
    KSqlApp app = new KSqlApp();
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, asList("topicA", "topicB"));
    topics.put(KStream.WRITE_TOPICS, asList("topicC", "topicD"));
    app.setTopics(topics);
    app.setPrincipal("User:foo");
    project.setKSqls(singletonList(app));

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    accessControlManager.updatePlan(plan, topology);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .buildBindingsForKSqlApp(any(KSqlApp.class), anyString());

    verify(aclsBuilder, times(1)).buildBindingsForKSqlApp(app, "default.default");
  }

  @Test(expected = IOException.class)
  public void testkStreamAclsCreationWithMissingPrefixGroup() throws Exception {

    Properties props = new Properties();
    props.put(PROJECT_PREFIX_FORMAT_CONFIG, "");
    props.put(TOPOLOGY_STATE_FROM_CLUSTER, "true");
    props.put(ALLOW_DELETE_TOPICS, true);
    props.put(TOPIC_PREFIX_FORMAT_CONFIG, "{{topic}}");
    props.put(ALLOW_DELETE_BINDINGS, true);
    props.put(KAFKA_INTERNAL_TOPIC_PREFIXES, singletonList("_"));

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Configuration config = new Configuration(cliOps, props);

    AclsBindingsBuilder bindingsBuilder = new AclsBindingsBuilder(config);
    AccessControlManager accessControlManager =
        new AccessControlManager(aclsProvider, bindingsBuilder, config);

    Project project = new ProjectImpl("foo", config);

    KStream app = new KStream();
    app.setPrincipal("User:App0");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, Arrays.asList("topic-A", "topic-B"));
    topics.put(KStream.WRITE_TOPICS, Arrays.asList("topic-C", "topic-D"));
    app.setTopics(topics);
    project.setStreams(Collections.singletonList(app));

    Topology topology = new TopologyImpl();
    topology.setContext("integration-test");
    topology.addOther("source", "kstreamsAclsCreation");
    topology.addProject(project);

    accessControlManager.updatePlan(plan, topology);
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

    accessControlManager.updatePlan(plan, topology);

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
  public void newControlCenterACLCreation() throws IOException {

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

    accessControlManager.updatePlan(plan, topology);

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

    accessControlManager.updatePlan(plan, topology);

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

    accessControlManager.updatePlan(plan, topology);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .setClusterLevelRole(anyString(), anyString(), eq(Component.KAFKA_CONNECT));

    verify(aclsBuilder, times(1))
        .setClusterLevelRole("Operator", "User:foo", Component.KAFKA_CONNECT);
    verify(aclsBuilder, times(1))
        .setClusterLevelRole("ClusterAdmin", "User:bar", Component.KAFKA_CONNECT);
  }

  @Test
  public void newKafkaConnectACLsCreation() throws IOException {
    Project project = new ProjectImpl();

    Connector connector1 = new Connector();
    connector1.setPrincipal("User:Connect1");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(Connector.READ_TOPICS, asList("topicA", "topicB"));
    connector1.setTopics(topics);

    project.setConnectors(singletonList(connector1));

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    accessControlManager.updatePlan(plan, topology);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsBuilder)
        .buildBindingsForConnect(connector1, project.namePrefix());

    verify(aclsBuilder, times(1)).buildBindingsForConnect(eq(connector1), eq(project.namePrefix()));
  }

  @Test
  public void testDryRunMode() throws IOException {
    plan = ExecutionPlan.init(backendController, mockPrintStream);
    accessControlManager =
        new AccessControlManager(aclsProvider, new AclsBindingsBuilder(config), config);

    Topic topicA = new TopicImpl("topicA");
    TestTopologyBuilder builder =
        TestTopologyBuilder.createProject("foo", "project")
            .addTopic(topicA)
            .addConsumer("User:app1");
    List<Consumer> users = newArrayList(builder.getConsumers());

    doReturn(singletonList(new TopologyAclBinding()))
        .when(aclsBuilder)
        .buildBindingsForConsumers(users, topicA.toString(), false);

    accessControlManager.updatePlan(plan, builder.buildTopology());

    plan.run(true);

    verify(mockPrintStream, times(1)).println(any(Action.class));
  }

  @Test
  public void testAclDeleteWithDetailedOptionEnabled() throws IOException {
    doReturn(true).when(config).isAllowDeleteBindings();
    doReturn(false).when(config).isAllowDeleteTopics();

    testAclsDelete();
  }

  @Test
  public void testAclDeleteWithDetailedOptionDisabled() throws IOException {
    doReturn(false).when(config).isAllowDeleteBindings();
    doReturn(false).when(config).isAllowDeleteTopics();

    testNoAclsDeleted();
  }

  private void testNoAclsDeleted() throws IOException {
    BackendController backendController = initializeFileBackendController();
    plan = ExecutionPlan.init(backendController, mockPrintStream);
    accessControlManager =
        new AccessControlManager(aclsProvider, new AclsBindingsBuilder(config), config);

    TestTopologyBuilder builder =
        TestTopologyBuilder.createProject()
            .addTopic("topicA")
            .addConsumer("User:app1")
            .addConsumer("User:app2");

    accessControlManager.updatePlan(plan, builder.buildTopology());
    plan.run();

    verify(aclsProvider, times(1)).createBindings(any());

    builder.removeConsumer("User:app2");

    Mockito.reset(aclsProvider);
    plan = ExecutionPlan.init(backendController, mockPrintStream);
    accessControlManager.updatePlan(plan, builder.buildTopology());
    plan.run();

    verify(aclsProvider, times(0)).clearBindings(any());
  }

  private void testAclsDelete() throws IOException {
    BackendController backendController = initializeFileBackendController();
    plan = ExecutionPlan.init(backendController, mockPrintStream);
    accessControlManager =
        new AccessControlManager(aclsProvider, new AclsBindingsBuilder(config), config);

    TestTopologyBuilder builder =
        TestTopologyBuilder.createProject()
            .addTopic("topicA")
            .addConsumer("User:app1")
            .addConsumer("User:app2");

    accessControlManager.updatePlan(plan, builder.buildTopology());
    plan.run();

    verify(aclsProvider, times(1)).createBindings(any());

    builder.removeConsumer("User:app2");

    Mockito.reset(aclsProvider);
    plan = ExecutionPlan.init(backendController, mockPrintStream);
    Topology topology = builder.buildTopology();
    accessControlManager.updatePlan(plan, topology);
    plan.run();

    List<TopologyAclBinding> bindingsToDelete =
        returnAclsForConsumers(
            singletonList(new Consumer("User:app2")), builder.getTopic("topicA").toString());

    verify(aclsProvider, times(1)).clearBindings(new HashSet<>(bindingsToDelete));
  }

  private List<TopologyAclBinding> returnAclsForConsumers(List<Consumer> consumers, String topic) {

    List<AclBinding> acls = newArrayList();

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

    return acls.stream().map(TopologyAclBinding::new).collect(Collectors.toList());
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

  private BackendController initializeFileBackendController() throws IOException {
    BackendController backendController = new BackendController();
    backendController.load();
    backendController.reset();
    return backendController;
  }

  @Test
  public void testToProcessOnlySelectedTopics() throws IOException {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Properties props = new Properties();
    props.put(TOPIC_MANAGED_PREFIXES, Collections.singletonList("NamespaceA"));
    props.put(TOPIC_PREFIX_FORMAT_CONFIG, "{{topic}}");

    Configuration config = new Configuration(cliOps, props);
    accessControlManager =
        new AccessControlManager(aclsProvider, new AclsBindingsBuilder(config), config);

    TestTopologyBuilder builder =
        TestTopologyBuilder.createProject(config)
            .addTopic("topicA")
            .addTopic("NamespaceA_topicA")
            .addConsumer("User:app1");

    accessControlManager.updatePlan(plan, builder.buildTopology());

    // Check that we only have one action not 2
    assertEquals(1, plan.getActions().size());

    // Check that the action bindings are for the managed prefix topic, not the non-managed prefix.
    assertEquals(
        2,
        plan.getActions().get(0).getBindings().stream()
            .filter(
                b ->
                    b.getResourceType().equals(ResourceType.TOPIC.name())
                        && b.getResourceName().equals("NamespaceA_topicA"))
            .count());
    assertEquals(
        0,
        plan.getActions().get(0).getBindings().stream()
            .filter(
                b ->
                    b.getResourceType().equals(ResourceType.TOPIC.name())
                        && b.getResourceName().equals("topicA"))
            .count());
  }

  @Test
  public void testToProcessOnlySelectedGroupsOrWildcard() throws IOException {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Properties props = new Properties();
    props.put(GROUP_MANAGED_PREFIXES, Collections.singletonList("NamespaceA"));

    Configuration config = new Configuration(cliOps, props);
    accessControlManager =
        new AccessControlManager(aclsProvider, new AclsBindingsBuilder(config), config);

    TestTopologyBuilder builder =
        TestTopologyBuilder.createProject(config)
            .addTopic("topicA")
            .addConsumer("User:app1", "NamespaceA_ConsumerGroupA")
            .addConsumer("User:app2", "NamespaceB_ConsumerGroupB")
            .addConsumer("User:app3", "*");

    accessControlManager.updatePlan(plan, builder.buildTopology());

    // Check that we only have one action
    assertEquals(1, plan.getActions().size());

    // Check that the action bindings are for the managed prefix group, not the non-managed prefix.
    assertEquals(
        1,
        plan.getActions().get(0).getBindings().stream()
            .filter(
                b ->
                    b.getResourceType().equals(ResourceType.GROUP.name())
                        && b.getResourceName().equals("NamespaceA_ConsumerGroupA"))
            .count());
    assertEquals(
        0,
        plan.getActions().get(0).getBindings().stream()
            .filter(
                b ->
                    b.getResourceType().equals(ResourceType.GROUP.name())
                        && b.getResourceName().equals("NamespaceB_ConsumerGroupB"))
            .count());
    assertEquals(
        1,
        plan.getActions().get(0).getBindings().stream()
            .filter(
                b ->
                    b.getResourceType().equals(ResourceType.GROUP.name())
                        && b.getResourceName().equals("*"))
            .count());
  }

  @Test
  public void testToProcessWildcardGroupOnlySelectedServiceAccounts() throws IOException {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Properties props = new Properties();
    props.put(SERVICE_ACCOUNT_MANAGED_PREFIXES, Collections.singletonList("User:NamespaceA"));

    Configuration config = new Configuration(cliOps, props);
    accessControlManager =
        new AccessControlManager(aclsProvider, new AclsBindingsBuilder(config), config);

    TestTopologyBuilder builder =
        TestTopologyBuilder.createProject(config)
            .addTopic("topicA")
            .addConsumer("User:NamespaceA_app1", "*")
            .addConsumer("User:NamespaceB_app2", "*");

    accessControlManager.updatePlan(plan, builder.buildTopology());

    // Check that we only have one action
    assertEquals(1, plan.getActions().size());

    // Check that the action bindings are for the managed service group, not the non-managed prefix.
    assertEquals(
        1,
        plan.getActions().get(0).getBindings().stream()
            .filter(
                b ->
                    b.getResourceType().equals(ResourceType.GROUP.name())
                        && b.getResourceName().equals("*")
                        && b.getPrincipal().equals("User:NamespaceA_app1"))
            .count());
    assertEquals(
        0,
        plan.getActions().get(0).getBindings().stream()
            .filter(
                b ->
                    b.getResourceType().equals(ResourceType.GROUP.name())
                        && b.getResourceName().equals("*")
                        && b.getPrincipal().equals("User:NamespaceB_app2"))
            .count());
  }
}
