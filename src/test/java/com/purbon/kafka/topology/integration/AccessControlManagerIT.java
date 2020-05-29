package com.purbon.kafka.topology.integration;

import com.purbon.kafka.topology.AccessControlManager;
import com.purbon.kafka.topology.ClusterState;
import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.Platform;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.SchemaRegistry;
import com.purbon.kafka.topology.roles.SimpleAclsProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import org.junit.Test;

public class AccessControlManagerIT {

  private static AdminClient kafkaAdminClient;
  private AccessControlManager accessControlManager;
  private ClusterState cs;
  private SimpleAclsProvider aclsProvider;

  @Before
  public void before() {
    kafkaAdminClient = AdminClient.create(config());
    TopologyBuilderAdminClient adminClient = new TopologyBuilderAdminClient(kafkaAdminClient);
    adminClient.clearAcls();

    cs = new ClusterState();
    aclsProvider = new SimpleAclsProvider(adminClient);
    accessControlManager = new AccessControlManager(aclsProvider, cs);
  }

  @Test
  public void testAclsCleanup() throws ExecutionException, InterruptedException {

    // Crate an ACL outside of the control of the state manager.
    aclsProvider.setAclsForProducers(Collections.singleton("User:foo"), "bar");

    // Add a collection of ACLs using the control manager, so keeping everything
    // in the loop of the cluster state
    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:testAclsCleanupApp1"));

    Project project = new Project("project");
    project.setConsumers(consumers);
    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("testAclsCleanup");
    topology.addProject(project);

    accessControlManager.sync(topology);

    // Verify the cluster state, only has acls created using the access control manager.
    Assert.assertEquals(3, cs.size());
    // there should be 5 acls currently in the cluster, 3 for the consumer and 2 for the producer
    verifyAclsOfSize(5);
    // clear all acls within the control of the manager.
    accessControlManager.clearAcls();
    // in the cluster should be only 2 acl staying, the one we created outside the CS
    verifyAclsOfSize(2);
  }

  @Test
  public void consumerAclsCreation() throws ExecutionException, InterruptedException {

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));

    Project project = new Project("project");
    project.setConsumers(consumers);
    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("testConsumerAclsCreation");
    topology.addProject(project);

    accessControlManager.sync(topology);

    verifyConsumerAcls(consumers, topicA.toString());
  }

  @Test
  public void producerAclsCreation() throws ExecutionException, InterruptedException {

    List<Producer> producers = new ArrayList<>();
    producers.add(new Producer("User:Producer1"));

    Project project = new Project("project");
    project.setProducers(producers);
    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("producerAclsCreation");
    topology.addProject(project);

    accessControlManager.sync(topology);

    verifyProducerAcls(producers, topicA.toString());
  }

  @Test
  public void kstreamsAclsCreation() throws ExecutionException, InterruptedException {
    Project project = new Project();

    KStream app = new KStream();
    app.setPrincipal("User:App0");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    topics.put(KStream.WRITE_TOPICS, Arrays.asList("topicC", "topicD"));
    app.setTopics(topics);
    project.setStreams(Collections.singletonList(app));

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("kstreamsAclsCreation");
    topology.addProject(project);

    accessControlManager.sync(topology);

    verifyKStreamsAcls(app);
  }

  @Test
  public void schemaRegistryAclsCreation() throws ExecutionException, InterruptedException {
    Project project = new Project();

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("schemaRegistryAclsCreation");
    topology.addProject(project);

    Platform platform = new Platform();
    SchemaRegistry sr = new SchemaRegistry();
    sr.setPrincipal("User:foo");
    platform.addSchemaRegistry(sr);

    SchemaRegistry sr2 = new SchemaRegistry();
    sr2.setPrincipal("User:banana");
    platform.addSchemaRegistry(sr2);

    topology.setPlatform(platform);

    accessControlManager.sync(topology);

    verifySchemaRegistryAcls(platform);
  }

  @Test
  public void connectAclsCreation() throws ExecutionException, InterruptedException {
    Project project = new Project();

    Connector connector = new Connector();
    connector.setPrincipal("User:Connect");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    connector.setTopics(topics);
    project.setConnectors(Collections.singletonList(connector));

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("connectAclsCreation");
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

    List<SchemaRegistry> srs = platform.getSchemaRegistry();

    for (SchemaRegistry sr : srs) {

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
