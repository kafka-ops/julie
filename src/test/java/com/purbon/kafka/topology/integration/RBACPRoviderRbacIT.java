package com.purbon.kafka.topology.integration;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.Constants.*;
import static com.purbon.kafka.topology.roles.rbac.RBACPredefinedRoles.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.purbon.kafka.topology.AccessControlManager;
import com.purbon.kafka.topology.BackendController;
import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.ExecutionPlan;
import com.purbon.kafka.topology.TestTopologyBuilder;
import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.api.mds.RbacResourceType;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Platform;
import com.purbon.kafka.topology.model.PlatformSystem;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KSqlApp;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.Schemas;
import com.purbon.kafka.topology.model.users.platform.ControlCenter;
import com.purbon.kafka.topology.model.users.platform.ControlCenterInstance;
import com.purbon.kafka.topology.model.users.platform.Kafka;
import com.purbon.kafka.topology.model.users.platform.KafkaConnect;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistry;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import com.purbon.kafka.topology.roles.RBACProvider;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.roles.rbac.RBACBindingsBuilder;
import com.purbon.kafka.topology.utils.BasicAuth;
import com.purbon.kafka.topology.utils.TestUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.kafka.common.resource.PatternType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RBACPRoviderRbacIT extends MDSBaseTest {

  private String mdsServer = "http://localhost:8090";
  private String mdsUser = "professor";
  private String mdsPassword = "professor";

  private MDSApiClient apiClient;
  @Mock private BackendController cs;
  private ExecutionPlan plan;

  private AccessControlManager defaultAccessControlManager;
  private AccessControlManager optimizedAclsAccessControlManager;

  @Before
  public void before() throws IOException, InterruptedException {
    super.beforeEach();
    TestUtils.deleteStateFile();

    apiClient = new MDSApiClient(mdsServer);
    apiClient.setBasicAuth(new BasicAuth(mdsUser, mdsPassword));
    apiClient.authenticate();
    apiClient.setKafkaClusterId(getKafkaClusterID());
    apiClient.setSchemaRegistryClusterID(getSchemaRegistryClusterID());
    apiClient.setConnectClusterID(getKafkaConnectClusterID());
    apiClient.setKSqlClusterID(getKSqlClusterID());

    plan = ExecutionPlan.init(cs, System.out);
    RBACProvider rbacProvider = new RBACProvider(apiClient);
    RBACBindingsBuilder bindingsBuilder = new RBACBindingsBuilder(apiClient);

    defaultAccessControlManager = buildDefaultAccessControlManager(rbacProvider, bindingsBuilder);
    optimizedAclsAccessControlManager = buildOptimizedAclsAccessControlManager(rbacProvider, bindingsBuilder);
  }

  private AccessControlManager buildDefaultAccessControlManager(
    RBACProvider rbacProvider,
    BindingsBuilderProvider bindingsBuilder
  ) {
    Properties props = new Properties();
    props.put(ALLOW_DELETE_BINDINGS, true);

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Configuration configuration = new Configuration(cliOps, props);

    return new AccessControlManager(rbacProvider, bindingsBuilder, configuration);
  }

  private AccessControlManager buildOptimizedAclsAccessControlManager(
    RBACProvider rbacProvider,
    BindingsBuilderProvider bindingsBuilder
  ) {
    Properties props = new Properties();
    props.put(ALLOW_DELETE_BINDINGS, true);
    props.put(OPTIMIZED_ACLS_CONFIG, true);

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Configuration configuration = new Configuration(cliOps, props);

    return new AccessControlManager(rbacProvider, bindingsBuilder, configuration);
  }

  @Test
  public void consumerAclsCreation() throws IOException {

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));

    Project project = new ProjectImpl("project");
    project.setConsumers(consumers);
    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.setContext("testConsumerAclsCreation-test");
    topology.addProject(project);

    defaultAccessControlManager.updatePlan(topology, plan);
    plan.run();

    // this method is call twice, once for consumers and one for producers
    verify(cs, times(1)).addBindings(anyList());
    verify(cs, times(1)).flushAndClose();
    verifyConsumerAcls(consumers, topicA.toString());
  }

  @Test
  public void producerAclsCreation() throws IOException {

    List<Producer> producers = new ArrayList<>();
    producers.add(new Producer("User:app2", null, true));

    Project project = new ProjectImpl("project");
    project.setProducers(producers);
    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.setContext("producerAclsCreation-test");
    topology.addProject(project);

    defaultAccessControlManager.updatePlan(topology, plan);
    plan.run();

    // this method is call twice, once for consumers and one for consumers
    verify(cs, times(1)).addBindings(anyList());
    verify(cs, times(1)).flushAndClose();
    verifyProducerAcls(producers, topicA.toString(), 2);
  }

  @Test
  public void producerAclsWithExtraPropertiesShouldNotBreak() throws IOException {

    List<Producer> producers = new ArrayList<>();
    var producer = new Producer("User:app3");
    producer.setTransactionId(Optional.of("12345"));
    producers.add(producer);

    Project project = new ProjectImpl("project");
    project.setProducers(producers);
    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.setContext("producerAclsWithExtraPropertiesShouldNotBreak-test");
    topology.addProject(project);

    defaultAccessControlManager.updatePlan(topology, plan);
    plan.run();

    // this method is call twice, once for consumers and one for consumers
    verify(cs, times(1)).addBindings(anyList());
    verify(cs, times(1)).flushAndClose();
    verifyProducerAcls(producers, topicA.toString(), 2);
  }

  @Test
  public void kstreamsAclsCreation() throws IOException {
    Project project = new ProjectImpl();

    KStream app = new KStream();
    app.setPrincipal("User:App3");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    topics.put(KStream.WRITE_TOPICS, Arrays.asList("topicC", "topicD"));
    app.setTopics(topics);
    project.setStreams(Collections.singletonList(app));

    Topology topology = new TopologyImpl();
    topology.setContext("kstreamsAclsCreation-test");
    topology.addProject(project);

    defaultAccessControlManager.updatePlan(topology, plan);
    plan.run();

    verify(cs, times(1)).addBindings(anyList());
    verify(cs, times(1)).flushAndClose();
    verifyKStreamsAcls(app);
  }

  @Test
  public void ksqlAppAclsCreation() throws IOException {
    Project project = new ProjectImpl();

    KSqlApp app = new KSqlApp();
    app.setKsqlDbId("ksql-id");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, asList("topicA", "topicB"));
    topics.put(KStream.WRITE_TOPICS, asList("topicC", "topicD"));
    app.setTopics(topics);
    app.setPrincipal("User:foo");
    project.setKSqls(singletonList(app));

    Topology topology = new TopologyImpl();
    topology.setContext("integration-test");
    topology.addOther("source", "ksqlAppAclsCreation-test");
    topology.addProject(project);

    defaultAccessControlManager.updatePlan(topology, plan);
    plan.run();

    verify(cs, times(1)).addBindings(anyList());
    verify(cs, times(1)).flushAndClose();

    verifyKSqlAppAcls(app);
  }

  @Test
  public void connectorsRbacCreation() throws IOException {
    var names = asList("jdbc-sink", "jdbc-source");

    Connector con = new Connector();
    con.setPrincipal("User:Connect");
    con.setConnectors(Optional.of(names));

    PlatformSystem<Connector> connectors = new PlatformSystem<>(singletonList(con));

    Project project =
        new ProjectImpl(
            "name",
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(connectors),
            Optional.empty(),
            Optional.empty(),
            Collections.emptyMap(),
            Collections.emptyList(),
            new Configuration());

    Topology topology = new TopologyImpl();
    topology.setContext("connectorsRbacCreation-test");
    topology.addProject(project);

    defaultAccessControlManager.updatePlan(topology, plan);
    plan.run();

    verify(cs, times(1)).addBindings(anyList());
    verify(cs, times(1)).flushAndClose();

    var resources = apiClient.lookupResourcesForConnect(con.getPrincipal(), RESOURCE_OWNER);
    for (RbacResourceType resource : resources) {
      assertThat(names).contains(resource.getName());
      assertThat(resource.getResourceType()).isEqualTo("Connector");
    }
  }

  @Test
  public void connectAclsCreation() throws IOException {
    Project project = new ProjectImpl();

    Connector connector = new Connector();
    connector.setPrincipal("User:Connect");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    connector.setTopics(topics);
    project.setConnectors(Collections.singletonList(connector));

    Topology topology = new TopologyImpl();
    topology.setContext("connectAclsCreation-test");
    topology.addProject(project);

    defaultAccessControlManager.updatePlan(topology, plan);
    plan.run();

    verify(cs, times(1)).addBindings(anyList());
    verify(cs, times(1)).flushAndClose();
    verifyConnectAcls(connector);
  }

  @Test
  public void schemasRbacCreation() throws IOException {
    var names = asList("foo", "bar");

    Schemas schema = new Schemas();
    schema.setPrincipal("User:Schemas");
    schema.setSubjects(names);

    PlatformSystem<Schemas> schemas = new PlatformSystem<>(singletonList(schema));

    Project project =
        new ProjectImpl(
            "name",
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(schemas),
            Optional.empty(),
            Collections.emptyMap(),
            Collections.emptyList(),
            new Configuration());

    Topology topology = new TopologyImpl();
    topology.setContext("schemasRbacCreation-test");
    topology.addProject(project);

    defaultAccessControlManager.updatePlan(topology, plan);
    plan.run();

    verify(cs, times(1)).addBindings(anyList());
    verify(cs, times(1)).flushAndClose();

    var resources =
        apiClient.lookupResourcesForSchemaRegistry(schema.getPrincipal(), RESOURCE_OWNER);
    assertThat(resources.size()).isEqualTo(2);
    for (RbacResourceType resource : resources) {
      assertThat(names).contains(resource.getName());
      assertThat(resource.getResourceType()).isEqualTo("Subject");
      assertThat(resource.getPatternType()).isEqualTo(PatternType.LITERAL.name());
    }
    // remove the first and only schema and check if properly deleted.
    topology.getProjects().get(0).setSchemas(Collections.emptyList());

    defaultAccessControlManager.updatePlan(topology, plan);
    plan.run();

    resources = apiClient.lookupResourcesForSchemaRegistry(schema.getPrincipal(), RESOURCE_OWNER);
    assertThat(resources).isEmpty();

  }

  @Test
  public void schemasOptimizedRbacCreation() throws IOException {
    var names = asList("foo", "bar");

    Schemas schema = new Schemas();
    schema.setPrincipal("User:Schemas");
    schema.setSubjects(names);

    PlatformSystem<Schemas> schemas = new PlatformSystem<>(singletonList(schema));

    Project project =
      new ProjectImpl(
        "name",
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(schemas),
        Optional.empty(),
        Collections.emptyMap(),
        Collections.emptyList(),
        new Configuration());

    Topology topology = new TopologyImpl();
    topology.setContext("schemasOptimizedRbacCreation-test");
    topology.addProject(project);

    optimizedAclsAccessControlManager.updatePlan(topology, plan);
    plan.run();

    verify(cs, times(1)).addBindings(anyList());
    verify(cs, times(1)).flushAndClose();

    var resources =
      apiClient.lookupResourcesForSchemaRegistry(schema.getPrincipal(), RESOURCE_OWNER);

    assertThat(resources.size()).isEqualTo(1);
    for (RbacResourceType resource : resources) {
      assertThat(project.namePrefix()).contains(resource.getName());
      assertThat(resource.getResourceType()).isEqualTo("Subject");
      assertThat(resource.getPatternType()).isEqualTo(PatternType.PREFIXED.name());
    }
    // remove the first and only schema and check if properly deleted.
    topology.getProjects().get(0).setSchemas(Collections.emptyList());

    optimizedAclsAccessControlManager.updatePlan(topology, plan);
    plan.run();

    resources = apiClient.lookupResourcesForSchemaRegistry(schema.getPrincipal(), RESOURCE_OWNER);
    assertThat(resources).isEmpty();
  }

  @Test
  public void schemaRegistryAclsCreation() throws IOException {
    Project project = new ProjectImpl();

    Topology topology = new TopologyImpl();
    topology.setContext("schemaRegistryAclsCreation-test");
    topology.addProject(project);

    Platform platform = new Platform();
    SchemaRegistry sr = new SchemaRegistry();
    SchemaRegistryInstance instance = new SchemaRegistryInstance();
    instance.setPrincipal("User:foo");

    SchemaRegistryInstance instance2 = new SchemaRegistryInstance();
    instance2.setPrincipal("User:banana");

    sr.setInstances(Arrays.asList(instance, instance2));

    Map<String, List<User>> rbac = new HashMap<>();
    rbac.put("SecurityAdmin", Collections.singletonList(new User("User:foo")));
    rbac.put("ClusterAdmin", Collections.singletonList(new User("User:bar")));
    sr.setRbac(Optional.of(rbac));

    platform.setSchemaRegistry(sr);
    topology.setPlatform(platform);

    defaultAccessControlManager.updatePlan(topology, plan);
    plan.run();

    verify(cs, times(1)).addBindings(anyList());
    verify(cs, times(1)).flushAndClose();
    verifySchemaRegistryAcls(platform);
  }

  @Test
  public void controlcenterAclsCreation() throws IOException {
    Project project = new ProjectImpl();

    Topology topology = new TopologyImpl();
    topology.setContext("controlcenterAclsCreation-test");
    topology.addProject(project);

    Platform platform = new Platform();
    ControlCenter c3 = new ControlCenter();
    ControlCenterInstance instance = new ControlCenterInstance();
    instance.setPrincipal("User:foo");
    instance.setAppId("appid");
    c3.setInstances(Collections.singletonList(instance));
    platform.setControlCenter(c3);

    topology.setPlatform(platform);

    defaultAccessControlManager.updatePlan(topology, plan);
    plan.run();

    verify(cs, times(1)).addBindings(anyList());
    verify(cs, times(1)).flushAndClose();
    verifyControlCenterAcls(platform);
  }

  @Test
  public void kafkaClusterLevelAclCreation() throws IOException {
    Project project = new ProjectImpl();

    Topology topology = new TopologyImpl();
    topology.setContext("kafkaClusterLevelAclCreation-test");
    topology.addProject(project);

    Platform platform = new Platform();
    Kafka kafka = new Kafka();
    Map<String, List<User>> rbac = new HashMap<>();
    rbac.put("Operator", Collections.singletonList(new User("User:foo")));
    rbac.put("ClusterAdmin", Collections.singletonList(new User("User:bar")));
    kafka.setRbac(Optional.of(rbac));
    platform.setKafka(kafka);
    topology.setPlatform(platform);

    defaultAccessControlManager.updatePlan(topology, plan);
    plan.run();

    verify(cs, times(1)).addBindings(anyList());
    verify(cs, times(1)).flushAndClose();

    verifyKafkaClusterACLs(platform);
  }

  @Test
  public void connectClusterLevelAclCreation() throws IOException {
    Project project = new ProjectImpl();

    Topology topology = new TopologyImpl();
    topology.setContext("kafkaClusterLevelAclCreation-test");
    topology.addProject(project);

    Platform platform = new Platform();
    KafkaConnect connect = new KafkaConnect();
    Map<String, List<User>> rbac = new HashMap<>();
    rbac.put("Operator", Collections.singletonList(new User("User:foo")));
    rbac.put("ClusterAdmin", Collections.singletonList(new User("User:bar")));
    connect.setRbac(Optional.of(rbac));
    platform.setKafkaConnect(connect);
    topology.setPlatform(platform);

    defaultAccessControlManager.updatePlan(topology, plan);
    plan.run();

    verify(cs, times(1)).addBindings(anyList());
    verify(cs, times(1)).flushAndClose();

    verifyConnectClusterACLs(platform);
  }

  @Test
  public void testRoleDeleteFlow() throws IOException {

    BackendController cs = new BackendController();
    ExecutionPlan plan = ExecutionPlan.init(cs, System.out);
    RBACProvider rbacProvider = Mockito.spy(new RBACProvider(apiClient));
    RBACBindingsBuilder bindingsBuilder = new RBACBindingsBuilder(apiClient);

    Properties props = new Properties();
    props.put(TOPOLOGY_STATE_FROM_CLUSTER, true);
    props.put(ALLOW_DELETE_TOPICS, true);
    props.put(ALLOW_DELETE_BINDINGS, true);

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Configuration config = new Configuration(cliOps, props);

    defaultAccessControlManager = new AccessControlManager(rbacProvider, bindingsBuilder, config);

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1a"));
    consumers.add(new Consumer("User:app2a"));

    Topology topology = new TopologyImpl(config);
    topology.setContext("testRoleDeleteFlow-test");

    Project project = new ProjectImpl("project");
    project.setConsumers(consumers);
    topology.setProjects(Collections.singletonList(project));

    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    defaultAccessControlManager.updatePlan(topology, plan);
    plan.run();

    // two group and two topics as we have one topic and two principles
    List<TopologyAclBinding> bindings = getBindings(rbacProvider);
    assertThat(bindings).hasSize(4);
    consumers.remove(0); // remove the first consumer

    cs = new BackendController();
    plan = ExecutionPlan.init(cs, System.out);

    defaultAccessControlManager.updatePlan(topology, plan);
    plan.run();

    bindings = getBindings(rbacProvider);
    // only one group and one topic as we removed one of principles
    assertThat(bindings).hasSize(2);
  }

  @Test
  public void deleteRolesShouldBeSkippedIfPrincipalIsNotManaged() throws IOException {
    BackendController cs = new BackendController();
    ExecutionPlan plan = ExecutionPlan.init(cs, System.out);
    RBACProvider rbacProvider = Mockito.spy(new RBACProvider(apiClient));
    RBACBindingsBuilder bindingsBuilder = new RBACBindingsBuilder(apiClient);

    Properties props = new Properties();
    props.put(TOPOLOGY_STATE_FROM_CLUSTER, true);
    props.put(ALLOW_DELETE_TOPICS, true);
    props.put(ALLOW_DELETE_BINDINGS, true);
    props.put(SERVICE_ACCOUNT_MANAGED_PREFIXES + ".0", "User:app");

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Configuration config = new Configuration(cliOps, props);

    defaultAccessControlManager = new AccessControlManager(rbacProvider, bindingsBuilder, config);
    final List<String> principals = asList("User:Pere", "User:app1b", "User:app2b");

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1b"));
    consumers.add(new Consumer("User:app2b"));

    Topology topology = new TopologyImpl(config);
    var prefix = "deleteRolesShouldBeSkippedIfPrincipalIsNotManaged-test";
    topology.setContext(prefix);

    Project project = new ProjectImpl("project");
    project.setConsumers(consumers);
    topology.setProjects(Collections.singletonList(project));

    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    defaultAccessControlManager.updatePlan(topology, plan);
    plan.run();

    // should create a new principal outside of the managed ones, before triggering the deletion.
    var extraBinding = apiClient.bind("User:Pere", RESOURCE_OWNER, "topicA", "Topic", "LITERAL");
    rbacProvider.createBindings(singleton(extraBinding));

    // two group and three topics, as we have one topic and two principles and one unmanaged
    // resource binding
    List<TopologyAclBinding> bindings =
        getBindings(rbacProvider).stream()
            .filter(
                binding ->
                    principals.contains(binding.getPrincipal())
                        || binding.getResourceName().startsWith(prefix))
            .collect(Collectors.toList());
    assertThat(bindings).hasSize(5);
    consumers.remove(0); // remove the first consumer

    cs = new BackendController();
    plan = ExecutionPlan.init(cs, System.out);

    defaultAccessControlManager.updatePlan(topology, plan);
    plan.run();

    bindings =
        getBindings(rbacProvider).stream()
            .filter(
                binding ->
                    principals.contains(binding.getPrincipal())
                        || binding.getResourceName().startsWith(prefix))
            .collect(Collectors.toList());
    // only one group and one topic as we removed one of principles plus the extra binding
    assertThat(bindings).hasSize(3);
    List<String> finalPrincipals = asList("User:Pere", "User:app2b");
    for (TopologyAclBinding binding : bindings) {
      assertThat(finalPrincipals).contains(binding.getPrincipal());
    }
  }

  @Test
  public void testJulieRoleAclCreation() throws IOException {

    BackendController cs = new BackendController();
    ExecutionPlan plan = ExecutionPlan.init(cs, System.out);
    RBACProvider rbacProvider = Mockito.spy(new RBACProvider(apiClient));
    RBACBindingsBuilder bindingsBuilder = new RBACBindingsBuilder(apiClient);
    String principal = "User:app" + System.currentTimeMillis();

    Topology topology =
        TestTopologyBuilder.createProject()
            .addOther("app", principal, "foo", "subj", "con")
            .buildTopology();

    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Properties props = new Properties();
    props.put(JULIE_ROLES, TestUtils.getResourceFilename("/roles-rbac.yaml"));

    Configuration config = new Configuration(cliOps, props);

    defaultAccessControlManager =
        new AccessControlManager(rbacProvider, bindingsBuilder, config.getJulieRoles(), config);

    defaultAccessControlManager.updatePlan(topology, plan);

    plan.run();

    List<TopologyAclBinding> bindings =
        getBindings(rbacProvider).stream()
            .filter(binding -> binding.getPrincipal().equalsIgnoreCase(principal))
            .collect(Collectors.toList());

    assertThat(bindings).hasSize(4);

    List<String> roles = apiClient.lookupRoles(principal);
    assertTrue(roles.contains(DEVELOPER_READ));

    roles =
        apiClient.lookupRoles(
            principal, apiClient.withClusterIDs().forKafka().forKafkaConnect().asMap());
    assertTrue(roles.contains(SECURITY_ADMIN));

    var clusters = apiClient.withClusterIDs().forKafka().forKsql().asMap();
    roles = apiClient.lookupRoles(principal, clusters);
    assertTrue(roles.contains(RESOURCE_OWNER));
  }

  private List<TopologyAclBinding> getBindings(RBACProvider rbacProvider) {
    return rbacProvider.listAcls().values().stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private void verifyConnectClusterACLs(Platform platform) {
    Map<String, Map<String, String>> clusters =
        apiClient.withClusterIDs().forKafka().forKafkaConnect().asMap();

    Map<String, List<User>> rbac = platform.getKafkaConnect().getRbac().get();
    for (String role : rbac.keySet()) {
      User user = rbac.get(role).get(0);
      List<String> roles = apiClient.lookupRoles(user.getPrincipal(), clusters);
      assertTrue(roles.contains(role));
    }
  }

  private void verifyKafkaClusterACLs(Platform platform) {
    Map<String, List<User>> kafkaRbac = platform.getKafka().getRbac().get();
    for (String role : kafkaRbac.keySet()) {
      User user = kafkaRbac.get(role).get(0);
      List<String> roles = apiClient.lookupRoles(user.getPrincipal());
      assertTrue(roles.contains(role));
    }
  }

  private void verifyControlCenterAcls(Platform platform) {
    ControlCenterInstance c3 = platform.getControlCenter().getInstances().get(0);
    List<String> roles = apiClient.lookupRoles(c3.getPrincipal());
    assertTrue(roles.contains(SYSTEM_ADMIN));
  }

  private void verifySchemaRegistryAcls(Platform platform) {
    SchemaRegistryInstance sr = platform.getSchemaRegistry().getInstances().get(0);
    List<String> roles = apiClient.lookupRoles(sr.getPrincipal());
    assertTrue(roles.contains(RESOURCE_OWNER));

    Map<String, Map<String, String>> clusters =
        apiClient.withClusterIDs().forKafka().forSchemaRegistry().asMap();

    roles = apiClient.lookupRoles(sr.getPrincipal(), clusters);
    assertTrue(roles.contains(SECURITY_ADMIN));

    Map<String, List<User>> srRbac = platform.getSchemaRegistry().getRbac().get();
    for (String role : srRbac.keySet()) {
      User user = srRbac.get(role).get(0);
      roles = apiClient.lookupRoles(user.getPrincipal(), clusters);
      assertTrue(roles.contains(role));
    }
  }

  private void verifyConnectAcls(Connector app) {
    List<String> roles = apiClient.lookupRoles(app.getPrincipal());
    assertTrue(roles.contains(DEVELOPER_READ));
    assertTrue(roles.contains(RESOURCE_OWNER));

    Map<String, Map<String, String>> clusters =
        apiClient.withClusterIDs().forKafka().forKafkaConnect().asMap();

    roles = apiClient.lookupRoles(app.getPrincipal(), clusters);
    assertTrue(roles.contains(SECURITY_ADMIN));
  }

  private void verifyKStreamsAcls(KStream app) {
    List<String> roles = apiClient.lookupRoles(app.getPrincipal());
    assertTrue(roles.contains(DEVELOPER_READ));
    assertTrue(roles.contains(DEVELOPER_WRITE));
    assertTrue(roles.contains(RESOURCE_OWNER));
  }

  private void verifyKSqlAppAcls(KSqlApp app) {
    List<String> roles = apiClient.lookupRoles(app.getPrincipal());
    assertTrue(roles.contains(DEVELOPER_READ));
    assertTrue(roles.contains(DEVELOPER_WRITE));
    assertTrue(roles.contains(RESOURCE_OWNER));
  }

  private void verifyProducerAcls(List<Producer> producers, String topic) {
    verifyProducerAcls(producers, topic, 1);
  }

  private void verifyProducerAcls(List<Producer> producers, String topic, int resourcesCount) {
    producers.forEach(
        producer -> {
          List<String> roles = apiClient.lookupRoles(producer.getPrincipal());
          assertEquals(1, roles.size());
          assertTrue(roles.contains(DEVELOPER_WRITE));

          List<RbacResourceType> resources =
              apiClient.lookupResourcesForKafka(producer.getPrincipal(), DEVELOPER_WRITE);
          assertEquals(resourcesCount, resources.size());
        });
  }

  private void verifyConsumerAcls(List<Consumer> consumers, String topic) {
    consumers.forEach(
        consumer -> {
          List<String> roles = apiClient.lookupRoles(consumer.getPrincipal());
          assertEquals(2, roles.size());
          assertTrue(roles.contains(DEVELOPER_READ));
          assertTrue(roles.contains(RESOURCE_OWNER));
        });
  }
}
