package com.purbon.kafka.topology;

import com.purbon.kafka.topology.actions.Action;
import com.purbon.kafka.topology.actions.topics.CreateTopicAction;
import com.purbon.kafka.topology.actions.topics.DeleteTopics;
import com.purbon.kafka.topology.actions.topics.RegisterSchemaAction;
import com.purbon.kafka.topology.actions.topics.TopicConfigUpdatePlan;
import com.purbon.kafka.topology.actions.topics.UpdateTopicConfigAction;
import com.purbon.kafka.topology.actions.topics.builders.TopicConfigUpdatePlanBuilder;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.exceptions.RemoteValidationException;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopicManager implements ExecutionPlanUpdater {

  private static final Logger LOGGER = LogManager.getLogger(TopicManager.class);

  public static final String NUM_PARTITIONS = "num.partitions";
  public static final String REPLICATION_FACTOR = "replication.factor";

  private final SchemaRegistryManager schemaRegistryManager;
  private final TopologyBuilderAdminClient adminClient;
  private final Configuration config;
  private List<String> internalTopicPrefixes;
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
    this.internalTopicPrefixes = new ArrayList<>();
    this.managedPrefixes = config.getTopicManagedPrefixes();
  }

  @Override
  public void updatePlan(ExecutionPlan plan, Map<String, Topology> topologies) throws IOException {

    internalTopicPrefixes = config.getKafkaInternalTopicPrefixes(topologies.values());
    Set<String> currentTopics = loadActualClusterStateIfAvailable(plan);
    Map<String, Topic> topics = new HashMap<>();

    Set<Action> createTopicActions = new HashSet<>();
    Set<Action> updateTopicConfigActions = new HashSet<>();
    for (Topology topology : topologies.values()) {
      Map<String, Topic> entryTopics = parseMapOfTopics(topology);
      entryTopics.forEach(
          (topicName, topic) -> {
            if (currentTopics.contains(topicName)) {
              TopicConfigUpdatePlanBuilder builder = new TopicConfigUpdatePlanBuilder(adminClient);
              TopicConfigUpdatePlan topicConfigUpdatePlan =
                  builder.createTopicConfigUpdatePlan(topic, topicName);
              if (topicConfigUpdatePlan.hasConfigChanges()) {
                updateTopicConfigActions.add(
                    new UpdateTopicConfigAction(adminClient, topicConfigUpdatePlan));
              }
            } else {
              createTopicActions.add(new CreateTopicAction(adminClient, topic, topicName));
            }
            topics.put(topicName, topic);
          });
    }

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
    Stream<Topic> topics =
        topology.getProjects().stream()
            .flatMap(project -> project.getTopics().stream())
            .filter(this::matchesPrefixList);

    Stream<Topic> specialTopics =
        topology.getSpecialTopics().stream().filter(this::matchesPrefixList);

    return Stream.concat(topics, specialTopics)
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

    if (!config.shouldVerifyRemoteState()) {
      OnceOnlyWarningLogger.getInstance().logRemoteStateVerificationDisabledWarning();
    }

    if (config.shouldVerifyRemoteState() && !config.fetchStateFromTheCluster()) {
      // verify that the remote state does not contain different topics than the local state
      detectDivergencesInTheRemoteCluster(plan);
    }

    return listOfTopics;
  }

  private void detectDivergencesInTheRemoteCluster(ExecutionPlan plan) throws IOException {
    if (!config.isAllowDeleteTopics()) {
      /* Assume topics are cleaned up by mechanisms outside JulieOps, and do not fail. */
      return;
    }
    Set<String> remoteTopics = adminClient.listApplicationTopics();
    List<String> delta =
        plan.getTopics().stream()
            .filter(localTopic -> !remoteTopics.contains(localTopic))
            .collect(Collectors.toList());

    if (delta.size() > 0) {
      String errorMessage =
          "Your remote state has changed since the last execution, this topics: "
              + StringUtils.join(delta, ",")
              + " are in your local state, but not in the cluster, please investigate!";
      LOGGER.error(errorMessage);
      throw new RemoteValidationException(errorMessage);
    }
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

  @Override
  public void printCurrentState(PrintStream os) throws IOException {
    os.println("List of Topics:");
    adminClient.listTopics().forEach(os::println);
  }

  public void close() {
    adminClient.close();
  }
}
