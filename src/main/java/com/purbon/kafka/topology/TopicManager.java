package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.TopologyBuilderConfig.KAFKA_INTERNAL_TOPIC_PREFIXES;
import static com.purbon.kafka.topology.TopologyBuilderConfig.KAFKA_INTERNAL_TOPIC_PREFIXES_DEFAULT;

import com.purbon.kafka.topology.actions.Action;
import com.purbon.kafka.topology.actions.DeleteTopics;
import com.purbon.kafka.topology.actions.SyncTopicAction;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopicManager {

  private static final Logger LOGGER = LogManager.getLogger(TopicManager.class);

  public static final String NUM_PARTITIONS = "num.partitions";
  public static final String REPLICATION_FACTOR = "replication.factor";

  private final SchemaRegistryManager schemaRegistryManager;
  private final TopologyBuilderAdminClient adminClient;
  private final TopologyBuilderConfig config;
  private final Boolean allowDelete;
  private final List<String> internalTopicPrefixes;
  private boolean dryRun;
  private PrintStream outputStream;

  public TopicManager(
      TopologyBuilderAdminClient adminClient, SchemaRegistryManager schemaRegistryManager) {
    this(adminClient, schemaRegistryManager, new TopologyBuilderConfig());
  }

  public TopicManager(
      TopologyBuilderAdminClient adminClient,
      SchemaRegistryManager schemaRegistryManager,
      TopologyBuilderConfig config) {

    this.adminClient = adminClient;
    this.schemaRegistryManager = schemaRegistryManager;
    this.config = config;
    this.allowDelete = config.allowDeletes();
    this.dryRun = config.isDryRun();
    this.outputStream = System.out;
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
              + StringUtils.join(new ArrayList<>(listOfTopics), ","));

    Set<String> updatedListOfTopics = new HashSet<>();
    // Foreach topic in the topology, sync it's content
    // if topics does not exist already it's created
    List<Action> actionPlan = new ArrayList<>();

    for (Project project : topology.getProjects()) {
      for (Topic topic : project.getTopics()) {
        String fullTopicName = topic.toString();
        actionPlan.add(
            new SyncTopicAction(
                adminClient, schemaRegistryManager, topic, fullTopicName, listOfTopics));
        updatedListOfTopics.add(fullTopicName);
      }
    }

    if (allowDelete) {
      // Handle topic delete: Topics in the initial list, but not present anymore after a
      // full topic sync should be deleted
      List<String> topicsToBeDeleted =
          listOfTopics.stream()
              .filter(topic -> !updatedListOfTopics.contains(topic) && !isAnInternalTopics(topic))
              .collect(Collectors.toList());

      if (topicsToBeDeleted.size() > 0) {
        LOGGER.debug("Topic to be deleted: " + StringUtils.join(topicsToBeDeleted, ","));
        actionPlan.add(new DeleteTopics(adminClient, topicsToBeDeleted));
      }
    }
    for (Action action : actionPlan) {
      if (dryRun) {
        outputStream.println(action);
      } else {
        action.run();
      }
    }
  }

  private boolean isAnInternalTopics(String topic) {
    return internalTopicPrefixes.stream()
            .anyMatch(topic::startsWith);
  }

  public void printCurrentState(PrintStream os) throws IOException {
    os.println("List of Topics:");
    adminClient.listTopics().forEach(os::println);
  }

  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }

  public void setOutputStream(PrintStream os) {
    this.outputStream = os;
  }

  public void close() {
    adminClient.close();
  }
}
