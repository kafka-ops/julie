package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.TopicSchemas;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopicManager {

  private static final Logger LOGGER = LogManager.getLogger(TopicManager.class);

  public static final String NUM_PARTITIONS = "num.partitions";
  public static final String REPLICATION_FACTOR = "replication.factor";

  private final TopologyBuilderAdminClient adminClient;
  private final SchemaRegistryManager schemaRegistryManager;

  public TopicManager(
      TopologyBuilderAdminClient adminClient, SchemaRegistryManager schemaRegistryManager) {
    this.adminClient = adminClient;
    this.schemaRegistryManager = schemaRegistryManager;
  }

  public void sync(Topology topology) {

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
                          syncTopic(topic, fullTopicName, listOfTopics);
                          updatedListOfTopics.add(fullTopicName);
                        }));

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

  public void syncTopic(Topic topic, String fullTopicName, Set<String> listOfTopics) {
    if (existTopic(fullTopicName, listOfTopics)) {
      adminClient.updateTopicConfig(topic, fullTopicName);
    } else {
      adminClient.createTopic(topic, fullTopicName);
    }

    if (topic.getSchemas() != null) {
      final TopicSchemas schemas = topic.getSchemas();

      if (StringUtils.isNotBlank(schemas.getKeySchemaType())
          && StringUtils.isNotBlank(schemas.getKeySchemaString())) {
        schemaRegistryManager.register(
            fullTopicName, schemas.getKeySchemaType(), schemas.getKeySchemaString());
      }

      if (StringUtils.isNotBlank(schemas.getValueSchemaType())
          && StringUtils.isNotBlank(schemas.getValueSchemaString())) {
        schemaRegistryManager.register(
            fullTopicName, schemas.getValueSchemaType(), schemas.getValueSchemaString());
      }
    }
  }

  private boolean existTopic(String topic, Set<String> listOfTopics) {
    return listOfTopics.contains(topic);
  }

  public void printCurrentState(PrintStream os) {
    os.println("List of Topics:");
    adminClient.listTopics().forEach(os::println);
  }
}
