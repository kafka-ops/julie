package com.purbon.kafka.topology.integration;

import static org.junit.Assert.assertTrue;

import com.purbon.kafka.topology.TopicManager;
import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.TopologyImpl;
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
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.testcontainers.containers.KafkaContainer;

public class TopicManagerIT {

  private static KafkaContainer container;
  private TopicManager topicManager;
  private AdminClient kafkaAdminClient;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private TopologyBuilderConfig config;

  @BeforeClass
  public static void setup() {
    container = new KafkaContainer("5.3.1");
    container.start();
  }

  @AfterClass
  public static void teardown() {
    container.stop();
  }

  @Before
  public void before() {
    kafkaAdminClient = AdminClient.create(config());
    TopologyBuilderAdminClient adminClient =
        new TopologyBuilderAdminClient(kafkaAdminClient, config);

    topicManager = new TopicManager(adminClient);
  }

  @Test
  public void testTopicCreation() throws ExecutionException, InterruptedException, IOException {
    HashMap<String, String> config = new HashMap<>();
    config.put(TopicManager.NUM_PARTITIONS, "1");
    config.put(TopicManager.REPLICATION_FACTOR, "1");

    Project project = new Project("project");
    Topic topicA = new Topic("topicA");
    topicA.setConfig(config);
    project.addTopic(topicA);

    config = new HashMap<>();
    config.put(TopicManager.NUM_PARTITIONS, "1");
    config.put(TopicManager.REPLICATION_FACTOR, "1");

    Topic topicB = new Topic("topicB");
    topicB.setConfig(config);
    project.addTopic(topicB);

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    topicManager.sync(topology);

    verifyTopics(Arrays.asList(topicA.toString(), topicB.toString()));
  }

  @Test
  public void testTopicDelete() throws ExecutionException, InterruptedException, IOException {

    Project project = new Project("project");
    Topic topicA = new Topic("topicA");
    topicA.setConfig(buildDummyTopicConfig());
    project.addTopic(topicA);

    Topic topicB = new Topic("topicB");
    topicB.setConfig(buildDummyTopicConfig());
    project.addTopic(topicB);

    String internalTopic = createInternalTopic();

    Topology topology = new TopologyImpl();
    topology.setTeam("testTopicDelete-test");
    topology.addProject(project);

    topicManager.sync(topology);

    Topic topicC = new Topic("topicC");
    topicC.setConfig(buildDummyTopicConfig());

    topology = new TopologyImpl();
    topology.setTeam("testTopicDelete-test");

    project = new Project("project");
    project.addTopic(topicA);
    project.addTopic(topicC);

    topology.addProject(project);

    topicManager.sync(topology);

    verifyTopics(Arrays.asList(topicA.toString(), internalTopic, topicC.toString()));
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
    Project project = new Project("project");
    Topic topicA = new Topic("topicA");
    topicA.setConfig(config);
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.setTeam("testTopicCreationWithConfig-test");
    topology.addProject(project);

    topicManager.sync(topology);

    verifyTopicConfiguration(topicA.toString(), config);
  }

  @Test
  public void testTopicConfigUpdate() throws ExecutionException, InterruptedException, IOException {

    HashMap<String, String> config = buildDummyTopicConfig();
    config.put("retention.bytes", "104857600"); // set the retention.bytes per partition to 100mb
    config.put("segment.bytes", "104857600");

    Project project = new Project("project");
    Topic topicA = new Topic("topicA");
    topicA.setConfig(config);
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.setTeam("testTopicConfigUpdate-test");
    topology.addProject(project);

    topicManager.sync(topology);
    verifyTopicConfiguration(topicA.toString(), config);

    config.put("retention.bytes", "104");
    config.remove("segment.bytes");

    topicA.setConfig(config);
    project.setTopics(Collections.singletonList(topicA));
    topology.setTeam("testTopicConfigUpdate-test");
    topology.setProjects(Collections.singletonList(project));

    topicManager.sync(topology);

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
    assertTrue("Internal topics not found", isInternal);
  }

  private Properties config() {
    Properties props = new Properties();
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers());
    props.put(AdminClientConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
    return props;
  }
}
