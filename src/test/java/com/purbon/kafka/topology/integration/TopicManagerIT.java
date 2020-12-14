package com.purbon.kafka.topology.integration;

import static com.purbon.kafka.topology.BuilderCLI.ALLOW_DELETE_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.BROKERS_OPTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.purbon.kafka.topology.BackendController;
import com.purbon.kafka.topology.ExecutionPlan;
import com.purbon.kafka.topology.TopicManager;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.integration.containerutils.ContainerTestUtils;
import com.purbon.kafka.topology.integration.containerutils.SaslPlaintextKafkaContainer;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.ConfigResource.Type;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TopicManagerIT {

  private static SaslPlaintextKafkaContainer container;
  private TopicManager topicManager;
  private AdminClient kafkaAdminClient;

  private ExecutionPlan plan;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @BeforeClass
  public static void setup() {
    container = new SaslPlaintextKafkaContainer();
    container.start();
  }

  @AfterClass
  public static void teardown() {
    container.stop();
  }

  @Before
  public void before() throws IOException {
    kafkaAdminClient = ContainerTestUtils.getSaslAdminClient(container);
    TopologyBuilderAdminClient adminClient = new TopologyBuilderAdminClient(kafkaAdminClient);

    final SchemaRegistryClient schemaRegistryClient = new MockSchemaRegistryClient();
    final SchemaRegistryManager schemaRegistryManager =
        new SchemaRegistryManager(schemaRegistryClient, System.getProperty("user.dir"));
    this.plan = ExecutionPlan.init(new BackendController(), System.out);

    Properties props = new Properties();

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(ALLOW_DELETE_OPTION, "true");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    this.topicManager = new TopicManager(adminClient, schemaRegistryManager, config);
  }

  @Test
  public void testTopicCreation() throws ExecutionException, InterruptedException, IOException {
    HashMap<String, String> config = new HashMap<>();
    config.put(TopicManager.NUM_PARTITIONS, "1");
    config.put(TopicManager.REPLICATION_FACTOR, "1");

    Project project = new ProjectImpl("project");
    Topic topicA = new TopicImpl("topicA", config);
    project.addTopic(topicA);

    config = new HashMap<>();
    config.put(TopicManager.NUM_PARTITIONS, "1");
    config.put(TopicManager.REPLICATION_FACTOR, "1");

    Topic topicB = new TopicImpl("topicB", config);
    project.addTopic(topicB);

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    topicManager.apply(topology, plan);
    plan.run();

    verifyTopics(Arrays.asList(topicA.toString(), topicB.toString()));
  }

  @Test(expected = IOException.class)
  public void testTopicCreationWithFalseConfig() throws IOException {
    HashMap<String, String> config = new HashMap<>();
    config.put("num.partitions", "1");
    config.put("replication.factor", "1");
    config.put("banana", "bar");

    Project project = new ProjectImpl("project");
    Topic topicA = new TopicImpl("topicA", config);
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    topicManager.apply(topology, plan);
    plan.run();
  }

  @Test
  public void testTopicCreationWithChangedTopology()
      throws ExecutionException, InterruptedException, IOException {
    HashMap<String, String> config = new HashMap<>();
    config.put(TopicManager.NUM_PARTITIONS, "1");
    config.put(TopicManager.REPLICATION_FACTOR, "1");

    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl("project");
    topology.addProject(project);

    Topic topicA = new TopicImpl("topicA", config);
    project.addTopic(topicA);

    config = new HashMap<>();
    config.put(TopicManager.NUM_PARTITIONS, "1");
    config.put(TopicManager.REPLICATION_FACTOR, "1");

    Topic topicB = new TopicImpl("topicB", config);
    project.addTopic(topicB);

    topicManager.apply(topology, plan);
    plan.run();

    verifyTopics(Arrays.asList(topicA.toString(), topicB.toString()));

    Topology upTopology = new TopologyImpl();
    upTopology.setContext("foo");
    Project upProject = new ProjectImpl("bar");
    upTopology.addProject(upProject);

    config = new HashMap<>();
    config.put(TopicManager.NUM_PARTITIONS, "1");
    config.put(TopicManager.REPLICATION_FACTOR, "1");

    topicA = new TopicImpl("topicA", config);
    upProject.addTopic(topicA);

    config = new HashMap<>();
    config.put(TopicManager.NUM_PARTITIONS, "1");
    config.put(TopicManager.REPLICATION_FACTOR, "1");

    topicB = new TopicImpl("topicB", config);
    upProject.addTopic(topicB);

    plan.getActions().clear();
    topicManager.apply(upTopology, plan);
    plan.run();

    verifyTopics(Arrays.asList(topicA.toString(), topicB.toString()));
  }

  @Test
  public void testTopicDelete() throws ExecutionException, InterruptedException, IOException {

    Project project = new ProjectImpl("project");
    Topic topicA = new TopicImpl("topicA", buildDummyTopicConfig());
    project.addTopic(topicA);

    Topic topicB = new TopicImpl("topicB", buildDummyTopicConfig());
    project.addTopic(topicB);

    String internalTopic = createInternalTopic();

    Topology topology = new TopologyImpl();
    topology.setContext("testTopicDelete-test");
    topology.addProject(project);

    topicManager.apply(topology, plan);
    plan.run();

    Topic topicC = new TopicImpl("topicC", buildDummyTopicConfig());

    topology = new TopologyImpl();
    topology.setContext("testTopicDelete-test");

    project = new ProjectImpl("project");
    project.addTopic(topicA);
    project.addTopic(topicC);

    topology.addProject(project);

    plan.getActions().clear();
    topicManager.apply(topology, plan);
    plan.run();

    verifyTopics(Arrays.asList(topicA.toString(), internalTopic, topicC.toString()), 2);
  }

  private String createInternalTopic() {

    String topic = "_internal-topic";
    NewTopic newTopic = new NewTopic(topic, 1, (short) 1);

    try {
      kafkaAdminClient.createTopics(Collections.singleton(newTopic)).all().get();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return topic;
  }

  @Test
  public void testTopicCreationWithConfig()
      throws ExecutionException, InterruptedException, IOException {

    HashMap<String, String> config = buildDummyTopicConfig();
    config.put("retention.bytes", "104857600"); // set the retention.bytes per partition to 100mb
    Project project = new ProjectImpl("project");
    Topic topicA = new TopicImpl("topicA", config);
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.setContext("testTopicCreationWithConfig-test");
    topology.addProject(project);

    topicManager.apply(topology, plan);
    plan.run();

    verifyTopicConfiguration(topicA.toString(), config);
  }

  @Test
  public void testTopicConfigUpdate() throws ExecutionException, InterruptedException, IOException {

    HashMap<String, String> config = buildDummyTopicConfig();
    config.put("retention.bytes", "104857600"); // set the retention.bytes per partition to 100mb
    config.put("segment.bytes", "104857600");

    Project project = new ProjectImpl("project");
    Topic topicA = new TopicImpl("topicA", config);
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.setContext("testTopicConfigUpdate-test");
    topology.addProject(project);

    topicManager.apply(topology, plan);
    plan.run();

    verifyTopicConfiguration(topicA.toString(), config);

    config = buildDummyTopicConfig();
    config.put("retention.bytes", "104");
    topicA = new TopicImpl("topicA", config);
    project.setTopics(Collections.singletonList(topicA));
    topology.setContext("testTopicConfigUpdate-test");
    topology.setProjects(Collections.singletonList(project));

    plan.getActions().clear();
    topicManager.apply(topology, plan);
    plan.run();

    verifyTopicConfiguration(topicA.toString(), config, Collections.singletonList("segment.bytes"));
  }

  private void verifyTopicConfiguration(String topic, HashMap<String, String> config)
      throws ExecutionException, InterruptedException {
    verifyTopicConfiguration(topic, config, new ArrayList<>());
  }

  private void verifyTopicConfiguration(
      String topic, HashMap<String, String> config, List<String> removedConfigs)
      throws ExecutionException, InterruptedException {

    ConfigResource resource = new ConfigResource(Type.TOPIC, topic);
    Collection<ConfigResource> resources = Collections.singletonList(resource);

    Map<ConfigResource, Config> configs = kafkaAdminClient.describeConfigs(resources).all().get();

    Config topicConfig = configs.get(resource);
    Assert.assertNotNull(topicConfig);

    topicConfig
        .entries()
        .forEach(
            entry -> {
              if (!entry.isDefault()) {
                if (config.get(entry.name()) != null)
                  Assert.assertEquals(config.get(entry.name()), entry.value());
                Assert.assertFalse(removedConfigs.contains(entry.name()));
              }
            });
  }

  private HashMap<String, String> buildDummyTopicConfig() {
    HashMap<String, String> config = new HashMap<>();
    config.put(TopicManager.NUM_PARTITIONS, "1");
    config.put(TopicManager.REPLICATION_FACTOR, "1");
    return config;
  }

  private void verifyTopics(List<String> topics) throws ExecutionException, InterruptedException {
    verifyTopics(topics, topics.size());
  }

  private void verifyTopics(List<String> topics, int topicsCount)
      throws ExecutionException, InterruptedException {

    Set<String> topicNames = kafkaAdminClient.listTopics().names().get();
    topics.forEach(
        topic -> assertTrue("Topic " + topic + " not found", topicNames.contains(topic)));
    boolean isInternal = false;
    for (String topic : topicNames) {
      if (topic.startsWith("_")) {
        isInternal = true;
        break;
      }
    }
    Set<String> nonInternalTopics =
        topicNames.stream().filter(topic -> !topic.startsWith("_")).collect(Collectors.toSet());

    assertEquals(topicsCount, nonInternalTopics.size());

    assertTrue("Internal topics not found", isInternal);
  }
}
