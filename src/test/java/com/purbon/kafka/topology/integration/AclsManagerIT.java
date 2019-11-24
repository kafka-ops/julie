package com.purbon.kafka.topology.integration;

import com.purbon.kafka.topology.AclsManager;
import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
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

public class AclsManagerIT {

  private static AdminClient kafkaAdminClient;
  private AclsManager aclsManager;

  @Before
  public void before() {
    kafkaAdminClient = AdminClient.create(config());
    TopologyBuilderAdminClient adminClient = new TopologyBuilderAdminClient(kafkaAdminClient);
    aclsManager = new AclsManager(adminClient);
  }


  @Test
  public void consumerAclsCreation() throws ExecutionException, InterruptedException {

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("app1"));

    Project project = new Project("project");
    project.setConsumers(consumers);
    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("testConsumerAclsCreation");
    topology.addProject(project);

    aclsManager.sync(topology);

    verifyConsumerAcls(consumers, topicA.buildTopicName(topology, project));
  }

  @Test
  public void producerAclsCreation() throws ExecutionException, InterruptedException {

    List<Producer> producers = new ArrayList<>();
    producers.add(new Producer("app1"));

    Project project = new Project("project");
    project.setProducers(producers);
    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("producerAclsCreation");
    topology.addProject(project);

    aclsManager.sync(topology);

    verifyProducerAcls(producers, topicA.buildTopicName(topology, project));
  }

  @Test
  public void kstreamsAclsCreation() throws ExecutionException, InterruptedException {
    Project project = new Project();

    KStream app = new KStream();
    app.setPrincipal("App0");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    topics.put(KStream.WRITE_TOPICS, Arrays.asList("topicC", "topicD"));
    app.setTopics(topics);
    project.setStreams(Collections.singletonList(app));

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("kstreamsAclsCreation");
    topology.addProject(project);

    aclsManager.sync(topology);

    verifyKStreamsAcls(app);
  }

  @Test
  public void connectAclsCreation() throws ExecutionException, InterruptedException {
    Project project = new Project();

    Connector connector = new Connector();
    connector.setPrincipal("Connect");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    connector.setTopics(topics);
    project.setConnectors(Collections.singletonList(connector));

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("connectAclsCreation");
    topology.addProject(project);

    aclsManager.sync(topology);

    verifyConnectAcls(connector);

  }

  private void verifyConnectAcls(Connector connector) throws ExecutionException, InterruptedException {

    ResourcePatternFilter resourceFilter = new ResourcePatternFilter(ResourceType.TOPIC, null, PatternType.ANY);

    AccessControlEntryFilter entryFilter = new AccessControlEntryFilter(connector.getPrincipal(),
        null, AclOperation.READ, AclPermissionType.ALLOW);

    AclBindingFilter filter = new AclBindingFilter(resourceFilter, entryFilter);

    Collection<AclBinding> acls = kafkaAdminClient
        .describeAcls(filter)
        .values()
        .get();

    Assert.assertEquals(5, acls.size());

    entryFilter = new AccessControlEntryFilter(connector.getPrincipal(), null, AclOperation.WRITE, AclPermissionType.ALLOW);
    filter = new AclBindingFilter(resourceFilter, entryFilter);
    acls = kafkaAdminClient
        .describeAcls(filter)
        .values()
        .get();

    Assert.assertEquals(3, acls.size());

    resourceFilter = new ResourcePatternFilter(ResourceType.GROUP, null, PatternType.ANY);
    entryFilter = new AccessControlEntryFilter(connector.getPrincipal(), null, AclOperation.READ, AclPermissionType.ALLOW);
    filter = new AclBindingFilter(resourceFilter, entryFilter);
    acls = kafkaAdminClient
        .describeAcls(filter)
        .values()
        .get();

    Assert.assertEquals(1, acls.size());

  }

  private void verifyKStreamsAcls(KStream app) throws ExecutionException, InterruptedException {
    ResourcePatternFilter resourceFilter = ResourcePatternFilter.ANY;

    AccessControlEntryFilter entryFilter = new AccessControlEntryFilter(app.getPrincipal(),
        null, AclOperation.WRITE, AclPermissionType.ALLOW);

    AclBindingFilter filter = new AclBindingFilter(resourceFilter, entryFilter);

    Collection<AclBinding> acls = kafkaAdminClient
        .describeAcls(filter)
        .values()
        .get();

    // two acls created for the write topics
    Assert.assertEquals(2, acls.size());

    entryFilter = new AccessControlEntryFilter(app.getPrincipal(), null,
        AclOperation.READ, AclPermissionType.ALLOW);

    filter = new AclBindingFilter(resourceFilter, entryFilter);

    acls = kafkaAdminClient
        .describeAcls(filter)
        .values()
        .get();

    // two acls created for the read topics
    Assert.assertEquals(2, acls.size());

    entryFilter = new AccessControlEntryFilter(app.getPrincipal(), null,
        AclOperation.ALL, AclPermissionType.ALLOW);

    filter = new AclBindingFilter(resourceFilter, entryFilter);

    acls = kafkaAdminClient
        .describeAcls(filter)
        .values()
        .get();

    // 1 acls created for the prefix internal topics
    Assert.assertEquals(1, acls.size());



  }

  private void verifyProducerAcls(List<Producer> producers, String topic) throws InterruptedException, ExecutionException {

    for (Producer producer : producers) {
      ResourcePatternFilter resourceFilter = ResourcePatternFilter.ANY;
      AccessControlEntryFilter entryFilter = new AccessControlEntryFilter(producer.getPrincipal(),
          null, AclOperation.WRITE, AclPermissionType.ALLOW);

      AclBindingFilter filter = new AclBindingFilter(resourceFilter, entryFilter);
      Collection<AclBinding> acls = kafkaAdminClient
          .describeAcls(filter)
          .values()
          .get();

      Assert.assertEquals(1, acls.size());

      List<ResourceType> types = acls
          .stream()
          .map(aclBinding -> aclBinding.pattern().resourceType())
          .collect(Collectors.toList());

      Assert.assertTrue(types.contains(ResourceType.TOPIC));
    }
  }

  private void verifyConsumerAcls(List<Consumer> consumers, String topic) throws InterruptedException, ExecutionException {

    for(Consumer consumer : consumers) {
      ResourcePatternFilter resourceFilter = ResourcePatternFilter.ANY;
      AccessControlEntryFilter entryFilter = new AccessControlEntryFilter(consumer.getPrincipal(), null, AclOperation.READ, AclPermissionType.ALLOW);

      AclBindingFilter filter = new AclBindingFilter(resourceFilter, entryFilter);
      Collection<AclBinding> acls = kafkaAdminClient
          .describeAcls(filter)
          .values()
          .get();

        Assert.assertEquals(2, acls.size());

        List<ResourceType> types = acls
            .stream()
            .map(aclBinding -> aclBinding.pattern().resourceType())
            .collect(Collectors.toList());

        Assert.assertTrue(types.contains(ResourceType.GROUP));
        Assert.assertTrue(types.contains(ResourceType.TOPIC));
    }

  }


  private Properties config() {
    Properties props = new Properties();

    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    props.put(AdminClientConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
    props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
    props.put("sasl.mechanism", "PLAIN");

    props.put("sasl.jaas.config","org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kafka\" password=\"kafka\";");

    return props;
  }
}
