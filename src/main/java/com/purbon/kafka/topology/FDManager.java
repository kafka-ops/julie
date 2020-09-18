package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.FlatDescription;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.TopicDescription;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourceType;

public class FDManager {

  interface TopicConfigUpdatePolicy {
    List<AbstractAction> calcChanges(
        String topicName, Map<String, String> currentConfig, Map<String, String> desiredConfig);
  }

  static class DoNothingPolicy implements TopicConfigUpdatePolicy {
    @Override
    public List<AbstractAction> calcChanges(
        String topicName, Map<String, String> currentConfig, Map<String, String> desiredConfig) {
      return Collections.EMPTY_LIST;
    }
  }

  static class IncrementalUpdateNoCheck implements TopicConfigUpdatePolicy {
    /**
     * Incrementally applies the new settings from 'desiredConfig'.
     *
     * <p>Settings which are part of currentConfig but not of desiredConfig will not be changed.
     *
     * @param topicName
     * @param currentConfig
     * @param desiredConfig
     * @return
     */
    @Override
    public List<AbstractAction> calcChanges(
        String topicName, Map<String, String> currentConfig, Map<String, String> desiredConfig) {
      Map<String, String> updatedConfigs = new HashMap<>();
      desiredConfig.forEach(
          (dKey, dValue) -> {
            if (!currentConfig.containsKey(dKey) || !currentConfig.get(dKey).equals(dValue)) {
              updatedConfigs.put(dKey, dValue);
            }
          });
      final IncrementallyUpdateTopicAction action =
          new IncrementallyUpdateTopicAction(topicName, updatedConfigs);
      return Collections.singletonList(action);
    }
  }

  // called AbstractAction to prevent naming conflict
  interface AbstractAction {
    boolean run(AdminClient adminClient);
  }

  static class IncrementallyUpdateTopicAction implements AbstractAction {

    final String topicName;
    final Map<String, String> topicConfig;

    IncrementallyUpdateTopicAction(String topicName, Map<String, String> topicConfig) {
      this.topicName = topicName;
      this.topicConfig = topicConfig;
    }

    @Override
    public boolean run(AdminClient adminClient) {

      Collection<AlterConfigOp> ops = new ArrayList<>();

      topicConfig.forEach(
          (k, v) -> {
            ConfigEntry entry = new ConfigEntry(k, v);
            AlterConfigOp op = new AlterConfigOp(entry, AlterConfigOp.OpType.SET);
            ops.add(op);
          });
      Map<ConfigResource, Collection<AlterConfigOp>> stuff =
          Collections.singletonMap(new ConfigResource(ConfigResource.Type.TOPIC, topicName), ops);

      try {
        adminClient.incrementalAlterConfigs(stuff).all().get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return "Action: update settings for topic  " + topicName + " " + topicConfig;
    }
  }

  // better replace this with an action which can delete a collection of topics?
  static class DeleteTopicAction implements AbstractAction {

    final String topicName;

    public DeleteTopicAction(String topicName) {
      this.topicName = topicName;
    }

    @Override
    public boolean run(AdminClient adminClient) {
      try {
        adminClient.deleteTopics(Collections.singleton(topicName)).all().get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return "Will delete topic " + topicName;
    }
  }

  static class CreateAclsActions implements AbstractAction {

    final Collection<TopologyAclBinding> aclsToCreate;

    CreateAclsActions(Collection<TopologyAclBinding> aclsToCreate) {
      this.aclsToCreate = aclsToCreate;
    }

    @Override
    public boolean run(AdminClient adminClient) {
      final Set<AclBinding> collect =
          aclsToCreate.stream().map(TopologyAclBinding::toAclBinding).collect(Collectors.toSet());
      try {
        adminClient.createAcls(collect).all().get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return "Action: create ACls " + aclsToCreate;
    }
  }

  static class DeleteAclsActions implements AbstractAction {

    final Collection<TopologyAclBinding> aclsToDelete;

    DeleteAclsActions(Collection<TopologyAclBinding> aclsToDelete) {
      this.aclsToDelete = aclsToDelete;
    }

    @Override
    public boolean run(AdminClient adminClient) {
      final Set<AclBindingFilter> collect =
          aclsToDelete.stream()
              .map(TopologyAclBinding::toAclBinding)
              .map(AclBinding::toFilter)
              .collect(Collectors.toSet());
      try {
        adminClient.deleteAcls(collect).all().get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return "Action: delete ACls " + aclsToDelete;
    }
  }

  static class CreateTopicsAction implements AbstractAction {

    // TODO: clean up the logic for default replication.factor and num.partitions
    String defaultNumPartitions = "6";
    String defaultReplicationFactor = "3";

    final Collection<TopicDescription> topicDescriptions;

    CreateTopicsAction(Collection<TopicDescription> topicDescriptions) {
      this.topicDescriptions = topicDescriptions;
    }

    @Override
    public boolean run(AdminClient adminClient) {
      final Set<NewTopic> newTopicConfigs =
          topicDescriptions.stream()
              .map(
                  td -> {
                    final Map<String, String> configs = td.getConfigs();
                    Short replicationFactor =
                        Short.valueOf(
                            configs.getOrDefault(
                                "default.replication.factor", defaultReplicationFactor));
                    Integer numPartitions =
                        Integer.valueOf(
                            configs.getOrDefault("num.partitions", defaultNumPartitions));
                    final NewTopic nt =
                        new NewTopic(td.getName(), numPartitions, replicationFactor);
                    nt.configs(configs);
                    return nt;
                  })
              .collect(Collectors.toSet());
      try {
        adminClient.createTopics(newTopicConfigs).all().get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
        return true;
      }
      return false;
    }

    @Override
    public String toString() {
      final Set<String> topicNames =
          topicDescriptions.stream().map(TopicDescription::getName).collect(Collectors.toSet());
      return "Action: create topics " + topicNames;
    }
  }

  public List<AbstractAction> generatePlan(
      FlatDescription currentState,
      FlatDescription desiredState,
      boolean allowDeletes,
      TopicConfigUpdatePolicy updatePolicy) {

    List<AbstractAction> actions = new ArrayList<>();

    System.out.println("removed: " + calculateRemovedTopics(currentState, desiredState));
    System.out.println("added:" + calculateNewTopics(currentState, desiredState));
    System.out.println("changed: " + calculateTopicConfigDiffs(currentState, desiredState));
    if (allowDeletes) {
      calculateRemovedTopics(currentState, desiredState)
          .forEach(
              topicName -> {
                actions.add(new DeleteTopicAction(topicName));
              });
      final Set<TopologyAclBinding> aclBindingsToRemove =
          calculateRemovedAcls(currentState, desiredState);
      if (!aclBindingsToRemove.isEmpty()) {
        actions.add(new DeleteAclsActions(aclBindingsToRemove));
      }
    }
    final Set<TopicDescription> newTopicDescriptions =
        calculateNewTopics(currentState, desiredState).stream()
            .map(topicName -> desiredState.getTopics().get(topicName))
            .collect(Collectors.toSet());
    actions.add(new CreateTopicsAction(newTopicDescriptions));

    calculateTopicConfigDiffs(currentState, desiredState)
        .forEach(
            topicName -> {
              final List<AbstractAction> actionsForTopic =
                  updatePolicy.calcChanges(
                      topicName,
                      currentState.getTopics().get(topicName).getConfigs(),
                      desiredState.getTopics().get(topicName).getConfigs());
              actions.addAll(actionsForTopic);
            });

    final Set<TopologyAclBinding> aclBindingsToAdd = calculateNewAcls(currentState, desiredState);
    if (!aclBindingsToAdd.isEmpty()) {
      actions.add(new CreateAclsActions(aclBindingsToAdd));
    }
    return actions;
  }

  /**
   * Get all topicNames which are part of the currentState but are no longer part of the
   * desiredState
   *
   * @param currentState
   * @param desiredState
   * @return
   */
  public Set<String> calculateRemovedTopics(
      FlatDescription currentState, FlatDescription desiredState) {
    final Set<String> currentTopicNames = extractTopicNames(currentState);
    final Set<String> desiredTopicNames = extractTopicNames(desiredState);
    currentTopicNames.removeAll(desiredTopicNames);
    return currentTopicNames;
  }

  /**
   * Get all topic names which are part of desiredState but are not currently in currentState.
   *
   * @param currentState
   * @param desiredState
   * @return
   */
  public Set<String> calculateNewTopics(
      FlatDescription currentState, FlatDescription desiredState) {
    final Set<String> currentTopicNames = extractTopicNames(currentState);
    final Set<String> desiredTopicNames = extractTopicNames(desiredState);
    desiredTopicNames.removeAll(currentTopicNames);
    return desiredTopicNames;
  }

  /** @return */
  public Set<String> calculateTopicConfigDiffs(
      FlatDescription currentState, FlatDescription desiredState) {
    final Set<String> currentTopicNames = extractTopicNames(currentState);
    final Set<String> desiredTopicNames = extractTopicNames(desiredState);

    desiredTopicNames.retainAll(
        currentTopicNames); // now contains topic names which have not changed
    final Set<String> topicNamesWithConfigChanges = new HashSet<>();
    desiredTopicNames.forEach(
        topicName -> {
          final Map<String, String> currentConfig =
              currentState.getTopics().get(topicName).getConfigs();
          final Map<String, String> desiredConfig =
              desiredState.getTopics().get(topicName).getConfigs();
          if (!currentConfig.equals(desiredConfig)) {
            topicNamesWithConfigChanges.add(topicName);
          }
        });
    return topicNamesWithConfigChanges;
  }

  public Set<TopologyAclBinding> calculateNewAcls(
      FlatDescription currentState, FlatDescription desiredState) {
    final Set<TopologyAclBinding> currentACLs = extractAcls(currentState);
    final Set<TopologyAclBinding> desiredACls = extractAcls(desiredState);
    desiredACls.removeAll(currentACLs);
    return desiredACls;
  }

  public Set<TopologyAclBinding> calculateRemovedAcls(
      FlatDescription currentState, FlatDescription desiredState) {
    final Set<TopologyAclBinding> currentAcls = extractAcls(currentState);
    final Set<TopologyAclBinding> desiredAcls = extractAcls(desiredState);
    currentAcls.removeAll(desiredAcls);
    return currentAcls;
  }

  // returns a new Set containing the names of the topics in fd,
  private Set<String> extractTopicNames(FlatDescription fd) {
    return new HashSet<>(fd.getTopics().keySet());
  }

  // returns a new Set containing the ACL bindings from fd.
  private Set<TopologyAclBinding> extractAcls(FlatDescription fd) {
    return new HashSet<>(fd.getAclsBindings());
  }

  /**
   * Converts a topology into the a FlatDescription
   *
   * @param topology
   * @return
   */
  public FlatDescription compileTopology(Topology topology) {

    final Map<String, TopicDescription> topicDescriptions = new HashMap<>();

    topology.getProjects().stream()
        .forEach(
            project ->
                project.getTopics().stream()
                    .forEach(
                        topic -> {
                          final HashMap config = topic.getConfig();
                          final String topicName = topic.toString();
                          topicDescriptions.put(topicName, new TopicDescription(topicName, config));
                        }));

    Set<TopologyAclBinding> aclBindings = new HashSet<>();

    // for each project in the topology, make ACLs
    topology
        .getProjects()
        .forEach(
            project -> {
              aclBindings.addAll(getAclBindingForProject(project));
            });

    return new FlatDescription(aclBindings, topicDescriptions);
  }

  private Set<TopologyAclBinding> getAclBindingForProject(Project project) {

    final Set<String> topicsInProject =
        project.getTopics().stream().map(topic -> topic.toString()).collect(Collectors.toSet());
    final Set<TopologyAclBinding> aclsForProject = new HashSet<>();

    // deal with Producers: every producer in the project gets access to every topic
    project
        .getProducers()
        .forEach(
            producer -> {
              final String principal = producer.getPrincipal();
              final List<String> operations = Arrays.asList("WRITE", "DESCRIBE");
              final Set<TopologyAclBinding> producerBindings =
                  getLiteralBindingsFor(principal, ResourceType.TOPIC, operations, topicsInProject);
              aclsForProject.addAll(producerBindings);
            });

    // deal with consumers: every consumer in the project gets access to every topic in the project:
    project
        .getConsumers()
        .forEach(
            consumer -> {
              final String principal = consumer.getPrincipal();
              final List<String> topicOperations = Arrays.asList("READ", "DESCRIBE");
              final Set<TopologyAclBinding> consumerBindings =
                  getLiteralBindingsFor(
                      principal, ResourceType.TOPIC, topicOperations, topicsInProject);
              aclsForProject.addAll(consumerBindings);

              final String groupString = consumer.groupString();
              final PatternType patternType =
                  groupString.equals("*") ? PatternType.PREFIXED : PatternType.LITERAL;
              aclsForProject.add(
                  new TopologyAclBinding(
                      ResourceType.GROUP,
                      groupString,
                      "*",
                      "READ",
                      principal,
                      patternType.toString()));
            });

    // TODO: deal with connectors

    project
        .getStreams()
        .forEach(
            streamsApp -> {
              final String principal = streamsApp.getPrincipal();
              final List<String> readTopics = streamsApp.getTopics().get("read");
              final List<String> writeTopics = streamsApp.getTopics().get("write");

              aclsForProject.addAll(
                  getLiteralBindingsFor(
                      principal, ResourceType.TOPIC, Collections.singleton("READ"), readTopics));
              aclsForProject.addAll(
                  getLiteralBindingsFor(
                      principal, ResourceType.TOPIC, Collections.singleton("WRITE"), writeTopics));
              // lot's of stuff seems to be missing, for example consumer group
              // TODO: add application_id to streams config
              aclsForProject.add(
                  new TopologyAclBinding(
                      ResourceType.TOPIC,
                      project.buildTopicPrefix(),
                      "*",
                      "ALL",
                      principal,
                      "PREFIXED"));
            });
    return aclsForProject;
  }

  private Set<TopologyAclBinding> getLiteralBindingsFor(
      String principal,
      ResourceType resourceType,
      Collection<String> operations,
      Collection<String> resourceNames) {
    final Set<TopologyAclBinding> aclBindings = new HashSet<>();
    resourceNames.forEach(
        resourceName -> {
          operations.forEach(
              operation -> {
                aclBindings.add(
                    new TopologyAclBinding(
                        resourceType, resourceName, "*", operation, principal, "LITERAL"));
              });
        });
    return aclBindings;
  }
}
