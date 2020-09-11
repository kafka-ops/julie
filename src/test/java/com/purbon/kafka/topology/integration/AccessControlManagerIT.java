package com.purbon.kafka.topology.integration;

import static org.mockito.Mockito.when;

import com.purbon.kafka.topology.AccessControlManager;
import com.purbon.kafka.topology.ClusterState;
import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Platform;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.platform.ControlCenter;
import com.purbon.kafka.topology.model.users.platform.ControlCenterInstance;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistry;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import com.purbon.kafka.topology.roles.SimpleAclsProvider;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class AccessControlManagerIT {

  private static AdminClient kafkaAdminClient;
  private AccessControlManager accessControlManager;
  private ClusterState cs;
  private SimpleAclsProvider aclsProvider;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private TopologyBuilderConfig config;

  @Before
  public void before() throws IOException {
    kafkaAdminClient = AdminClient.create(config());
    TopologyBuilderAdminClient adminClient =
        new TopologyBuilderAdminClient(kafkaAdminClient, config);
    adminClient.clearAcls();

    cs = new ClusterState();
    aclsProvider = new SimpleAclsProvider(adminClient);
    accessControlManager = new AccessControlManager(aclsProvider, cs);
  }

  @Test
  public void aclsRemoval() throws ExecutionException, InterruptedException, IOException {

    // Crate an ACL outside of the control of the state manager.
    List<TopologyAclBinding> bindings =
        aclsProvider.setAclsForProducers(Collections.singleton("User:foo"), "bar");
    aclsProvider.createBindings(new HashSet<>(bindings));

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:testAclsRemovalUser1"));
    consumers.add(new Consumer("User:testAclsRemovalUser2"));

    Project project = new ProjectImpl("project");
    project.setConsumers(consumers);
    Topic topicA = new TopicImpl("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.setContext("integration-test");
    topology.addOther("source", "testAclsRemoval");
    topology.addProject(project);

    accessControlManager.sync(topology);

    Assert.assertEquals(6, cs.size());
    verifyAclsOfSize(8); // Total of 3 acls per consumer + 2 for the producer

    consumers.remove(1);
    project.setConsumers(consumers);

    accessControlManager.sync(topology);

    Assert.assertEquals(3, cs.size());
    verifyAclsOfSize(5);
  }

  @Test
  public void consumerAclsCreation() throws ExecutionException, InterruptedException, IOException {

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));

    Project project = new ProjectImpl("project");
    project.setConsumers(consumers);
    Topic topicA = new TopicImpl("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.setContext("integration-test");
    topology.addOther("source", "testConsumerAclsCreation");
    topology.addProject(project);

    accessControlManager.sync(topology);

    verifyConsumerAcls(consumers, topicA.toString());
  }

  @Test
  public void producerAclsCreation() throws ExecutionException, InterruptedException, IOException {

    List<Producer> producers = new ArrayList<>();
    producers.add(new Producer("User:Producer1"));

    Project project = new ProjectImpl("project");
    project.setProducers(producers);
    Topic topicA = new TopicImpl("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.setContext("integration-test");
    topology.addOther("source", "producerAclsCreation");
    topology.addProject(project);

    accessControlManager.sync(topology);

    verifyProducerAcls(producers, topicA.toString());
  }

  @Test
  public void kstreamsAclsCreation() throws ExecutionException, InterruptedException, IOException {
    Project project = new ProjectImpl();

    KStream app = new KStream();
    app.setPrincipal("User:App0");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    topics.put(KStream.WRITE_TOPICS, Arrays.asList("topicC", "topicD"));
    app.setTopics(topics);
    project.setStreams(Collections.singletonList(app));

    Topology topology = new TopologyImpl();
    topology.setContext("integration-test");
    topology.addOther("source", "kstreamsAclsCreation");
    topology.addProject(project);

    accessControlManager.sync(topology);

    verifyKStreamsAcls(app);
  }

  @Test
  public void schemaRegistryAclsCreation()
      throws ExecutionException, InterruptedException, IOException {
    Project project = new ProjectImpl();

    Topology topology = new TopologyImpl();
    topology.setContext("integration-test");
    topology.addOther("source", "schemaRegistryAclsCreation");
    topology.addProject(project);

    Platform platform = new Platform();
    SchemaRegistry sr = new SchemaRegistry();
    SchemaRegistryInstance instance = new SchemaRegistryInstance();
    instance.setPrincipal("User:foo");

    SchemaRegistryInstance instance2 = new SchemaRegistryInstance();
    instance2.setPrincipal("User:banana");

    sr.setInstances(Arrays.asList(instance, instance2));
    platform.setSchemaRegistry(sr);

    topology.setPlatform(platform);

    accessControlManager.sync(topology);

    verifySchemaRegistryAcls(platform);
  }

  @Test
  public void controlcenterAclsCreation()
      throws ExecutionException, InterruptedException, IOException {

    when(config.getConfluentCommandTopic()).thenReturn("foo");
    when(config.getConfluentMetricsTopic()).thenReturn("bar");
    when(config.getConfluentMonitoringTopic()).thenReturn("zet");

    Project project = new ProjectImpl();

    Topology topology = new TopologyImpl();
    topology.setContext("integration-test");
    topology.addOther("source", "controlcenterAclsCreation");
    topology.addProject(project);

    Platform platform = new Platform();
    ControlCenter c3 = new ControlCenter();
    ControlCenterInstance instance = new ControlCenterInstance();
    instance.setPrincipal("User:foo");
    instance.setAppId("appid");
    c3.setInstances(Collections.singletonList(instance));
    platform.setControlCenter(c3);

    topology.setPlatform(platform);

    accessControlManager.sync(topology);

    verifyControlCenterAcls(platform);
  }

  @Test
  public void connectAclsCreation() throws ExecutionException, InterruptedException, IOException {
    Project project = new ProjectImpl();

    Connector connector = new Connector();
    connector.setPrincipal("User:Connect");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    connector.setTopics(topics);
    project.setConnectors(Collections.singletonList(connector));

    Topology topology = new TopologyImpl();
    topology.setContext("integration-test");
    topology.addOther("source", "connectAclsCreation");
    topology.addProject(project);

    accessControlManager.sync(topology);

    verifyConnectAcls(connector);
  }

  private void verifyAclsOfSize(int size) throws ExecutionException, InterruptedException {

    Collection<AclBinding> acls =
        kafkaAdminClient.describeAcls(AclBindingFilter.ANY).values().get();

    Assert.assertEquals(size, acls.size());
  }

  private void verifyConnectAcls(Connector connector)
      throws ExecutionException, InterruptedException {

    ResourcePatternFilter resourceFilter =
        new ResourcePatternFilter(ResourceType.TOPIC, null, PatternType.ANY);

    AccessControlEntryFilter entryFilter =
        new AccessControlEntryFilter(
            connector.getPrincipal(), null, AclOperation.READ, AclPermissionType.ALLOW);

    AclBindingFilter filter = new AclBindingFilter(resourceFilter, entryFilter);

    Collection<AclBinding> acls = kafkaAdminClient.describeAcls(filter).values().get();

    Assert.assertEquals(5, acls.size());

    entryFilter =
        new AccessControlEntryFilter(
            connector.getPrincipal(), null, AclOperation.WRITE, AclPermissionType.ALLOW);
    filter = new AclBindingFilter(resourceFilter, entryFilter);
    acls = kafkaAdminClient.describeAcls(filter).values().get();

    Assert.assertEquals(3, acls.size());

    resourceFilter = new ResourcePatternFilter(ResourceType.GROUP, null, PatternType.ANY);
    entryFilter =
        new AccessControlEntryFilter(
            connector.getPrincipal(), null, AclOperation.READ, AclPermissionType.ALLOW);
    filter = new AclBindingFilter(resourceFilter, entryFilter);
    acls = kafkaAdminClient.describeAcls(filter).values().get();

    Assert.assertEquals(1, acls.size());
  }

  private void verifySchemaRegistryAcls(Platform platform)
      throws ExecutionException, InterruptedException {

    List<SchemaRegistryInstance> srs = platform.getSchemaRegistry().getInstances();

    for (SchemaRegistryInstance sr : srs) {
      ResourcePatternFilter resourceFilter =
          new ResourcePatternFilter(ResourceType.TOPIC, null, PatternType.ANY);

      AccessControlEntryFilter entryFilter =
          new AccessControlEntryFilter(
              sr.getPrincipal(), null, AclOperation.ANY, AclPermissionType.ALLOW);

      AclBindingFilter filter = new AclBindingFilter(resourceFilter, entryFilter);

      Collection<AclBinding> acls = kafkaAdminClient.describeAcls(filter).values().get();

      Assert.assertEquals(3, acls.size());
    }
  }

  private void verifyControlCenterAcls(Platform platform)
      throws ExecutionException, InterruptedException {

    List<ControlCenterInstance> c3List = platform.getControlCenter().getInstances();

    for (ControlCenterInstance c3 : c3List) {
      ResourcePatternFilter resourceFilter =
          new ResourcePatternFilter(ResourceType.TOPIC, null, PatternType.ANY);

      AccessControlEntryFilter entryFilter =
          new AccessControlEntryFilter(
              c3.getPrincipal(), null, AclOperation.ANY, AclPermissionType.ALLOW);

      AclBindingFilter filter = new AclBindingFilter(resourceFilter, entryFilter);

      Collection<AclBinding> acls = kafkaAdminClient.describeAcls(filter).values().get();

      Assert.assertEquals(16, acls.size());
    }
  }

  private void verifyKStreamsAcls(KStream app) throws ExecutionException, InterruptedException {
    ResourcePatternFilter resourceFilter = ResourcePatternFilter.ANY;

    AccessControlEntryFilter entryFilter =
        new AccessControlEntryFilter(
            app.getPrincipal(), null, AclOperation.WRITE, AclPermissionType.ALLOW);

    AclBindingFilter filter = new AclBindingFilter(resourceFilter, entryFilter);

    Collection<AclBinding> acls = kafkaAdminClient.describeAcls(filter).values().get();

    // two acls created for the write topics
    Assert.assertEquals(2, acls.size());

    entryFilter =
        new AccessControlEntryFilter(
            app.getPrincipal(), null, AclOperation.READ, AclPermissionType.ALLOW);

    filter = new AclBindingFilter(resourceFilter, entryFilter);

    acls = kafkaAdminClient.describeAcls(filter).values().get();

    // two acls created for the read topics
    Assert.assertEquals(2, acls.size());

    entryFilter =
        new AccessControlEntryFilter(
            app.getPrincipal(), null, AclOperation.ALL, AclPermissionType.ALLOW);

    filter = new AclBindingFilter(resourceFilter, entryFilter);

    acls = kafkaAdminClient.describeAcls(filter).values().get();

    // 1 acls created for the prefix internal topics
    Assert.assertEquals(1, acls.size());
  }

  private void verifyProducerAcls(List<Producer> producers, String topic)
      throws InterruptedException, ExecutionException {

    for (Producer producer : producers) {
      ResourcePatternFilter resourceFilter = ResourcePatternFilter.ANY;
      AccessControlEntryFilter entryFilter =
          new AccessControlEntryFilter(
              producer.getPrincipal(), null, AclOperation.ANY, AclPermissionType.ALLOW);

      AclBindingFilter filter = new AclBindingFilter(resourceFilter, entryFilter);
      Collection<AclBinding> acls = kafkaAdminClient.describeAcls(filter).values().get();

      Assert.assertEquals(2, acls.size());

      List<ResourceType> types =
          acls.stream()
              .map(aclBinding -> aclBinding.pattern().resourceType())
              .collect(Collectors.toList());

      Assert.assertTrue(types.contains(ResourceType.TOPIC));

      List<AclOperation> ops =
          acls.stream()
              .map(aclsBinding -> aclsBinding.entry().operation())
              .collect(Collectors.toList());

      Assert.assertTrue(ops.contains(AclOperation.DESCRIBE));
      Assert.assertTrue(ops.contains(AclOperation.WRITE));
    }
  }

  private void verifyConsumerAcls(List<Consumer> consumers, String topic)
      throws InterruptedException, ExecutionException {

    for (Consumer consumer : consumers) {
      ResourcePatternFilter resourceFilter = ResourcePatternFilter.ANY;
      AccessControlEntryFilter entryFilter =
          new AccessControlEntryFilter(
              consumer.getPrincipal(), null, AclOperation.ANY, AclPermissionType.ALLOW);

      AclBindingFilter filter = new AclBindingFilter(resourceFilter, entryFilter);
      Collection<AclBinding> acls = kafkaAdminClient.describeAcls(filter).values().get();

      Assert.assertEquals(3, acls.size());

      List<ResourceType> types =
          acls.stream()
              .map(aclBinding -> aclBinding.pattern().resourceType())
              .collect(Collectors.toList());

      Assert.assertTrue(types.contains(ResourceType.GROUP));
      Assert.assertTrue(types.contains(ResourceType.TOPIC));

      List<AclOperation> ops =
          acls.stream()
              .map(aclsBinding -> aclsBinding.entry().operation())
              .collect(Collectors.toList());

      Assert.assertTrue(ops.contains(AclOperation.DESCRIBE));
      Assert.assertTrue(ops.contains(AclOperation.READ));
    }
  }

  private Properties config() {
    Properties props = new Properties();

    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    props.put(AdminClientConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
    props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
    props.put("sasl.mechanism", "PLAIN");

    props.put(
        "sasl.jaas.config",
        "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kafka\" password=\"kafka\";");

    return props;
  }
}
