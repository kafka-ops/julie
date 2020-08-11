package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.TopicSchemas;
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
    this.allowDelete = config.allowDeleteTopics();
    this.internalTopicPrefixes =
        config
            .getStringList(
                TopologyBuilderConfig.KAFKA_INTERNAL_TOPIC_PREFIXES,
                TopologyBuilderConfig.KAFKA_INTERNAL_TOPIC_PREFIXES_DEFAULT)
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
        LOGGER.debug("Topic to be deleted: " + StringUtils.join(topicsToBeDeleted, ","));
      adminClient.deleteTopics(topicsToBeDeleted);
    }
  }

  private boolean isAnInternalTopics(String topic) {
    return internalTopicPrefixes.stream()
        .map(prefix -> topic.startsWith(prefix))
        .collect(Collectors.reducing((a, b) -> a || b))
        .get();
  }

  public void syncTopic(Topic topic, Set<String> listOfTopics) throws IOException {
    String fullTopicName = topic.toString();
    syncTopic(topic, fullTopicName, listOfTopics);
  }

  public void syncTopic(Topic topic, String fullTopicName, Set<String> listOfTopics)
      throws IOException {
    if (existTopic(fullTopicName, listOfTopics)) {
      adminClient.updateTopicConfig(topic, fullTopicName);
    } else {
      adminClient.createTopic(topic, fullTopicName);
    }

    if (topic.getSchemas() != null) {
      final TopicSchemas schemas = topic.getSchemas();

      if (StringUtils.isNotBlank(schemas.getKeySchemaFile())) {
        schemaRegistryManager.register(fullTopicName, schemas.getKeySchemaFile());
      }

      if (StringUtils.isNotBlank(schemas.getValueSchemaFile())) {
        schemaRegistryManager.register(fullTopicName, schemas.getValueSchemaFile());
      }
    }
  }

  private boolean existTopic(String topic, Set<String> listOfTopics) {
    return listOfTopics.contains(topic);
  }

  public void printCurrentState(PrintStream os) throws IOException {
    os.println("List of Topics:");
    adminClient.listTopics().forEach(os::println);
  }
}
