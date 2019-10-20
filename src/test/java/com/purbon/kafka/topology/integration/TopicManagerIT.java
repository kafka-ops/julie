package com.purbon.kafka.topology.integration;

import com.purbon.kafka.topology.TopicManager;
import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
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

    topicManager.syncTopics(topology);

    verifyTopics(Arrays.asList(topicA.composeTopicName(topology, project.getName()),
        topicB.composeTopicName(topology, project.getName())));
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
