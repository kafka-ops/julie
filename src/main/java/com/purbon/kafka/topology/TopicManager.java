package com.purbon.kafka.topology;

import com.purbon.kafka.topology.actions.Action;
import com.purbon.kafka.topology.actions.topics.CreateTopicAction;
import com.purbon.kafka.topology.actions.topics.DeleteTopics;
import com.purbon.kafka.topology.actions.topics.RegisterSchemaAction;
import com.purbon.kafka.topology.actions.topics.TopicConfigUpdatePlan;
import com.purbon.kafka.topology.actions.topics.UpdateTopicConfigAction;
import com.purbon.kafka.topology.actions.topics.builders.TopicConfigUpdatePlanBuilder;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopicManager implements ManagerOfThings {

  private static final Logger LOGGER = LogManager.getLogger(TopicManager.class);

  public static final String NUM_PARTITIONS = "num.partitions";
  public static final String REPLICATION_FACTOR = "replication.factor";

  private final SchemaRegistryManager schemaRegistryManager;
  private final TopologyBuilderAdminClient adminClient;
  private final Configuration config;
  private final List<String> internalTopicPrefixes;
  private final List<String> managedPrefixes;

  public TopicManager(
      TopologyBuilderAdminClient adminClient, SchemaRegistryManager schemaRegistryManager) {
    this(adminClient, schemaRegistryManager, new Configuration());
  }

  public TopicManager(
      TopologyBuilderAdminClient adminClient,
      SchemaRegistryManager schemaRegistryManager,
      Configuration config) {
    this.adminClient = adminClient;
    this.schemaRegistryManager = schemaRegistryManager;
    this.config = config;
    this.internalTopicPrefixes = config.getKafkaInternalTopicPrefixes();
    this.managedPrefixes = config.getTopicManagedPrefixes();
  }

  public void apply(Topology topology, ExecutionPlan plan) throws IOException {

    Set<String> currentTopics = loadActualClusterStateIfAvailable(plan);
    // Foreach topic in the topology, sync it's content
    // if topics does not exist already it's created

    Map<String, Topic> topics = parseMapOfTopics(topology);

    Set<Action> createTopicActions = new HashSet<>();
    Set<Action> updateTopicConfigActions = new HashSet<>();
    topics.forEach(
        (topicName, topic) -> {
          if (currentTopics.contains(topicName)) {
            TopicConfigUpdatePlanBuilder builder = new TopicConfigUpdatePlanBuilder(adminClient);
            TopicConfigUpdatePlan topicConfigUpdatePlan =
                builder.createTopicConfigUpdatePlan(topic, topicName);
            updateTopicConfigActions.add(
                new UpdateTopicConfigAction(adminClient, topicConfigUpdatePlan));
          } else {
            createTopicActions.add(new CreateTopicAction(adminClient, topic, topicName));
          }
        });

    createTopicActions.forEach(plan::add); // Do createActions before update actions
    updateTopicConfigActions.forEach(plan::add);

    topics.forEach(
        (topicName, topic) -> {
          plan.add(new RegisterSchemaAction(schemaRegistryManager, topic, topicName));
        });

    if (config.isAllowDeleteTopics()) {
      // Handle topic delete: Topics in the initial list, but not present anymore after a
      // full topic sync should be deleted
      List<String> topicsToBeDeleted =
          currentTopics.stream()
              .filter(topic -> !topics.containsKey(topic) && !isAnInternalTopics(topic))
              .collect(Collectors.toList());

      if (topicsToBeDeleted.size() > 0) {
        LOGGER.debug("Topic to be deleted: " + StringUtils.join(topicsToBeDeleted, ","));
        plan.add(new DeleteTopics(adminClient, topicsToBeDeleted));
      }
    }
  }

  private Map<String, Topic> parseMapOfTopics(Topology topology) {
    return topology.getProjects().stream()
        .flatMap(project -> project.getTopics().stream())
        .filter(this::matchesPrefixList)
        .collect(Collectors.toMap(Topic::toString, topic -> topic));
  }

  private boolean isAnInternalTopics(String topic) {
    return internalTopicPrefixes.stream().anyMatch(topic::startsWith);
  }

  private Set<String> loadActualClusterStateIfAvailable(ExecutionPlan plan) throws IOException {
    Set<String> listOfTopics =
        config.fetchTopicStateFromTheCluster()
            ? adminClient.listApplicationTopics()
            : plan.getTopics();

    listOfTopics =
        listOfTopics.stream().filter(this::matchesPrefixList).collect(Collectors.toSet());
    if (listOfTopics.size() > 0)
      LOGGER.debug(
          "Full list of managed topics in the cluster: "
              + StringUtils.join(new ArrayList<>(listOfTopics), ","));
    return listOfTopics;
  }

  private boolean matchesPrefixList(Topic topic) {
    return matchesPrefixList(topic.toString());
  }

  private boolean matchesPrefixList(String topic) {
    boolean matches =
        managedPrefixes.size() == 0 || managedPrefixes.stream().anyMatch(topic::startsWith);
    LOGGER.debug(String.format("Topic %s matches %s with $s", topic, matches, managedPrefixes));
    return matches;
  }

  public void printCurrentState(PrintStream os) throws IOException {
    os.println("List of Topics:");
    adminClient.listTopics().forEach(os::println);
  }

  public void close() {
    adminClient.close();
  }
}
