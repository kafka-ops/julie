package com.purbon.kafka.topology.integration;

import com.purbon.kafka.topology.TopicManager;
import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.ConfigResource.Type;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;

public class TopicManagerIT {

  private static KafkaContainer container;
  private TopicManager topicManager;
  private AdminClient kafkaAdminClient;
  @BeforeClass
  public static void setup() {
   container =  new KafkaContainer("5.3.1");
   container.start();
  }

  @AfterClass
  public static void teardown() {
    container.stop();
  }

  @Before
  public void before() {
    kafkaAdminClient = AdminClient.create(config());
    TopologyBuilderAdminClient adminClient = new TopologyBuilderAdminClient(kafkaAdminClient);

    topicManager = new TopicManager(adminClient);
  }

  @Test
  public void testTopicCreation() throws ExecutionException, InterruptedException {
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

    Topology topology = new Topology();
    topology.addProject(project);

    topicManager.sync(topology);

    verifyTopics(Arrays.asList(topicA.toString(),
        topicB.toString()));
  }

  @Test
  public void testTopicDelete() throws ExecutionException, InterruptedException {


    Project project = new Project("project");
    Topic topicA = new Topic("topicA");
    topicA.setConfig(buildDummyTopicConfig());
    project.addTopic(topicA);

    Topic topicB = new Topic("topicB");
    topicB.setConfig(buildDummyTopicConfig());
    project.addTopic(topicB);

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("testTopicDelete");
    topology.addProject(project);

    topicManager.sync(topology);

    Topic topicC = new Topic("topicC");
    topicC.setConfig(buildDummyTopicConfig());
    project.addTopic(topicB);

    topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("testTopicDelete");

    project = new Project("project");
    project.addTopic(topicA);
    project.addTopic(topicC);

    topology.addProject(project);

    topicManager.sync(topology);

    verifyTopics(Arrays.asList(topicA.toString(),
        topicC.toString()));
  }

  @Test
  public void testTopicCreationWithConfig() throws ExecutionException, InterruptedException {

    HashMap<String, String> config = buildDummyTopicConfig();
    config.put("retention.bytes", "104857600"); // set the retention.bytes per partition to 100mb
    Project project = new Project("project");
    Topic topicA = new Topic("topicA");
    topicA.setConfig(config);
    project.addTopic(topicA);

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("testTopicCreationWithConfig");
    topology.addProject(project);

    topicManager.sync(topology);

    verifyTopicConfiguration(topicA.toString(), config);
  }

  @Test
  public void testTopicConfigUpdate() throws ExecutionException, InterruptedException {

    HashMap<String, String> config = buildDummyTopicConfig();
    config.put("retention.bytes", "104857600"); // set the retention.bytes per partition to 100mb
    config.put("segment.bytes" ,"104857600");

    Project project = new Project("project");
    Topic topicA = new Topic("topicA");
    topicA.setConfig(config);
    project.addTopic(topicA);

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("testTopicConfigUpdate");
    topology.addProject(project);

    topicManager.sync(topology);
    verifyTopicConfiguration(topicA.toString(), config);

    config.put("retention.bytes", "104");
    config.remove("segment.bytes");

    topicA.setConfig(config);
    project.setTopics(Collections.singletonList(topicA));
    topology.setTeam("integration-test");
    topology.setSource("testTopicConfigUpdate");
    topology.setProjects(Collections.singletonList(project));

    topicManager.sync(topology);

    verifyTopicConfiguration(topicA.toString(), config, Collections.singletonList("segment.bytes"));
  }

  private void verifyTopicConfiguration(String topic, HashMap<String, String> config)
      throws ExecutionException, InterruptedException {
    verifyTopicConfiguration(topic, config, new ArrayList<>());
  }
  private void verifyTopicConfiguration(String topic, HashMap<String, String> config, List<String> removedConfigs)
      throws ExecutionException, InterruptedException {

    ConfigResource resource = new ConfigResource(Type.TOPIC, topic);
    Collection<ConfigResource> resources = Collections.singletonList(resource);

    Map<ConfigResource, Config> configs = kafkaAdminClient
        .describeConfigs(resources)
        .all()
        .get();

    Config topicConfig = configs.get(resource);
    Assert.assertNotNull(topicConfig);

    topicConfig
        .entries()
        .forEach(entry -> {
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

    Set<String> topicNames = kafkaAdminClient
        .listTopics()
        .names()
        .get();

    topics.forEach(topic -> Assert.assertTrue(topicNames.contains(topic)));
  }

  private Properties config() {
    Properties props = new Properties();
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers());
    props.put(AdminClientConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
    return props;
  }
}
