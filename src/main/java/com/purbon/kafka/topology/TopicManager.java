package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.ALLOW_DELETE_OPTION;

import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopicManager {

  private static final Logger LOGGER = LogManager.getLogger(TopicManager.class);

  public static final String NUM_PARTITIONS = "num.partitions";
  public static final String REPLICATION_FACTOR = "replication.factor";

  private final TopologyBuilderAdminClient adminClient;
  private final Map<String, String> cliParams;
  private final Boolean allowDelete;

  public TopicManager(TopologyBuilderAdminClient adminClient) {
    this(adminClient, new HashMap<>());
  }

  public TopicManager(TopologyBuilderAdminClient adminClient, Map<String, String> cliParams) {
    this.adminClient = adminClient;
    this.cliParams = cliParams;
    this.allowDelete = Boolean.valueOf(cliParams.getOrDefault(ALLOW_DELETE_OPTION, "true"));
  }

  public void sync(Topology topology) throws IOException {

    // List all topics existing in the cluster
    Set<String> listOfTopics = adminClient.listTopics();

    Set<String> updatedListOfTopics = new HashSet<>();
    // Foreach topic in the topology, sync it's content
    // if topics does not exist already it's created
    topology.getProjects().stream()
        .forEach(
            project ->
                project
                    .getTopics()
                    .forEach(
                        topic -> {
                          String fullTopicName = topic.toString();
                          try {
                            syncTopic(topic, fullTopicName, listOfTopics);
                            updatedListOfTopics.add(fullTopicName);
                          } catch (IOException e) {
                            LOGGER.error(e);
                          }
                        }));

    if (allowDelete) {
      // Handle topic delete: Topics in the initial list, but not present anymore after a
      // full topic sync should be deleted
      List<String> topicsToBeDeleted = new ArrayList<>();
      listOfTopics.stream()
          .forEach(
              originalTopic -> {
                if (!updatedListOfTopics.contains(originalTopic)) {
                  topicsToBeDeleted.add(originalTopic);
                }
              });
      adminClient.deleteTopics(topicsToBeDeleted);
    }
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
