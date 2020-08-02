package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.ALLOW_DELETE_OPTION;
import static com.purbon.kafka.topology.TopologyBuilderConfig.KAFKA_INTERNAL_TOPIC_PREFIXES;
import static com.purbon.kafka.topology.TopologyBuilderConfig.KAFKA_INTERNAL_TOPIC_PREFIXES_DEFAULT;

import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.common.StringUtils;

public class TopicManager {

  private static final Logger LOGGER = LogManager.getLogger(TopicManager.class);

  public static final String NUM_PARTITIONS = "num.partitions";
  public static final String REPLICATION_FACTOR = "replication.factor";

  private final TopologyBuilderAdminClient adminClient;
  private final TopologyBuilderConfig config;
  private final Boolean allowDelete;
  private final List<String> internalTopicPrefixes;

  public TopicManager(TopologyBuilderAdminClient adminClient) {
    this(adminClient, new TopologyBuilderConfig());
  }

  public TopicManager(TopologyBuilderAdminClient adminClient, TopologyBuilderConfig config) {
    this.adminClient = adminClient;
    this.config = config;
    this.allowDelete = Boolean.valueOf(config.params().getOrDefault(ALLOW_DELETE_OPTION, "true"));
    this.internalTopicPrefixes =
        config
            .getPropertyAsList(
                KAFKA_INTERNAL_TOPIC_PREFIXES, KAFKA_INTERNAL_TOPIC_PREFIXES_DEFAULT, ",")
            .stream()
            .map(s -> s.trim())
            .collect(Collectors.toList());
  }

  public void sync(Topology topology) throws IOException {

    // List all topics existing in the cluster, excluding internal topics
    Set<String> listOfTopics = adminClient.listApplicationTopics();
    if (listOfTopics.size() > 0)
      LOGGER.debug(
          "Full list of topics in the cluster: "
              + StringUtils.joinStrings(new ArrayList<>(listOfTopics), ","));

    Set<String> updatedListOfTopics = new HashSet<>();
    // Foreach topic in the topology, sync it's content
    // if topics does not exist already it's created

    for (Project project : topology.getProjects()) {
      for (Topic topic : project.getTopics()) {
        String fullTopicName = topic.toString();
        try {
          syncTopic(topic, fullTopicName, listOfTopics);
          updatedListOfTopics.add(fullTopicName);
        } catch (IOException e) {
          LOGGER.error(e);
          throw e;
        }
      }
    }

    if (allowDelete) {
      // Handle topic delete: Topics in the initial list, but not present anymore after a
      // full topic sync should be deleted
      List<String> topicsToBeDeleted = new ArrayList<>();
      listOfTopics.stream()
          .forEach(
              topic -> {
                if (!updatedListOfTopics.contains(topic) && !isAnInternalTopics(topic)) {
                  topicsToBeDeleted.add(topic);
                }
              });
      if (topicsToBeDeleted.size() > 0)
        LOGGER.debug("Topic to be deleted: " + StringUtils.joinStrings(topicsToBeDeleted, ","));
      adminClient.deleteTopics(topicsToBeDeleted);
    }
  }

  private boolean isAnInternalTopics(String topic) {
    return internalTopicPrefixes.stream()
        .map(prefix -> topic.startsWith(prefix))
        .collect(Collectors.reducing((a, b) -> a || b))
        .get();
  }

  public void syncTopic(Topic topic, String fullTopicName, Set<String> listOfTopics)
      throws IOException {
    if (existTopic(fullTopicName, listOfTopics)) {
      adminClient.updateTopicConfig(topic, fullTopicName);
    } else {
      adminClient.createTopic(topic, fullTopicName);
    }
  }

  public void syncTopic(Topic topic, Set<String> listOfTopics, Topology topology, Project project)
      throws IOException {
    String fullTopicName = topic.toString();
    syncTopic(topic, fullTopicName, listOfTopics);
  }

  private boolean existTopic(String topic, Set<String> listOfTopics) {
    return listOfTopics.contains(topic);
  }

  public void printCurrentState(PrintStream os) throws IOException {
    os.println("List of Topics:");
    adminClient
        .listTopics()
        .forEach(
            topic -> {
              os.println(topic);
            });
  }
}
