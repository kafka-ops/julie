package com.purbon.kafka.topology.api.adminclient;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConfigOp.OpType;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.ConfigResource.Type;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopologyBuilderAdminClient {

  private static final Logger LOGGER = LogManager.getLogger(TopologyBuilderAdminClient.class);

  private final AdminClient adminClient;
  private final Configuration config;

  public TopologyBuilderAdminClient(AdminClient adminClient, Configuration config) {
    this.adminClient = adminClient;
    this.config = config;
  }

  public Set<String> listTopics(ListTopicsOptions options) throws IOException {
    Set<String> listOfTopics;
    try {
      listOfTopics = adminClient.listTopics(options).names().get();
    } catch (InterruptedException | ExecutionException e) {
      LOGGER.error(e);
      throw new IOException(e);
    }
    return listOfTopics;
  }

  public void healthCheck() throws IOException {

    try {
      adminClient.describeCluster().nodes().get();
    } catch (Exception ex) {
      throw new IOException("Problem during the health-check operation", ex);
    }
  }

  public Set<String> listTopics() throws IOException {
    return listTopics(new ListTopicsOptions());
  }

  public Set<String> listApplicationTopics() throws IOException {
    ListTopicsOptions options = new ListTopicsOptions();
    options.listInternal(false);
    return listTopics(options);
  }

  public Map<String, String> updateTopicConfig(Topic topic, String fullTopicName, boolean dryRun)
      throws IOException {
    try {
      return updateTopicConfigPostAK23(topic, fullTopicName, dryRun);
    } catch (InterruptedException | ExecutionException ex) {
      LOGGER.error(ex);
      throw new IOException(ex);
    }
  }

  public int getPartitionCount(String topic) throws IOException {
    try {
      Map<String, TopicDescription> results =
          adminClient.describeTopics(Collections.singletonList(topic)).all().get();
      return results.get(topic).partitions().size();
    } catch (InterruptedException | ExecutionException e) {
      LOGGER.error(e);
      throw new IOException(e);
    }
  }

  public void updatePartitionCount(Topic topic, String topicName) throws IOException {
    Map<String, NewPartitions> map = new HashMap<>();
    map.put(topicName, NewPartitions.increaseTo(topic.partitionsCount()));
    try {
      adminClient.createPartitions(map).all().get();
    } catch (InterruptedException | ExecutionException e) {
      LOGGER.error(e);
      throw new IOException(e);
    }
  }

  public void clearAcls() throws IOException {
    Collection<AclBindingFilter> filters = new ArrayList<>();
    filters.add(AclBindingFilter.ANY);
    clearAcls(filters);
  }

  public void clearAcls(TopologyAclBinding aclBinding) throws IOException {
    Collection<AclBindingFilter> filters = new ArrayList<>();

    LOGGER.debug("clearAcl = " + aclBinding);
    ResourcePatternFilter resourceFilter =
        new ResourcePatternFilter(
            aclBinding.getResourceType(),
            aclBinding.getResourceName(),
            PatternType.valueOf(aclBinding.getPattern()));

    AccessControlEntryFilter accessControlEntryFilter =
        new AccessControlEntryFilter(
            aclBinding.getPrincipal(),
            aclBinding.getHost(),
            AclOperation.valueOf(aclBinding.getOperation()),
            AclPermissionType.ANY);

    AclBindingFilter filter = new AclBindingFilter(resourceFilter, accessControlEntryFilter);
    filters.add(filter);
    clearAcls(filters);
  }

  private void clearAcls(Collection<AclBindingFilter> filters) throws IOException {
    try {
      adminClient.deleteAcls(filters).all().get();
    } catch (ExecutionException | InterruptedException e) {
      LOGGER.error(e);
      throw new IOException(e);
    }
  }

  private Map<String, String> updateTopicConfigPostAK23(
      Topic topic, String fullTopicName, boolean dryRun)
      throws ExecutionException, InterruptedException {

    Config actualTopicConfig = getActualTopicConfig(fullTopicName);
    List<AlterConfigOp> toBeSetConfigs =
        topic.getRawConfig().entrySet().stream()
            .map(
                entry ->
                    new AlterConfigOp(
                        new ConfigEntry(entry.getKey(), entry.getValue()), OpType.SET))
            .collect(Collectors.toList());

    Set<String> newEntryKeys = topic.getRawConfig().keySet();
    Map<String, String> entryToOldValueMap = new HashMap<>();

    List<AlterConfigOp> deleteConfigOps =
        resolveDeleteEntries(fullTopicName, actualTopicConfig, newEntryKeys, entryToOldValueMap);

    if (!config.shouldOverwriteTopicsInSync() && !actualTopicConfig.entries().isEmpty()) {
      LOGGER.debug(
          "Existing topics with exact same configuration will not be overwritten/set again");

      List<AlterConfigOp> syncConfigOps = new ArrayList<>(toBeSetConfigs);
      // removing all same values again
      toBeSetConfigs.forEach(
          entry -> {
            String configEntryName = entry.configEntry().name();
            ConfigEntry configEntry = actualTopicConfig.get(configEntryName);
            if (configEntry != null) {
              String currentValue = configEntry.value();
              String newValue = entry.configEntry().value();
              if (newEntryKeys.contains(configEntryName) && currentValue.equals(newValue)) {
                LOGGER.debug(
                    "Topic {} and config key {} has the same value, removing config pair.",
                    fullTopicName,
                    configEntryName);
                syncConfigOps.remove(entry);

              } else {
                entryToOldValueMap.put(configEntryName, currentValue);
                LOGGER.debug(
                    "Topic {} and config key {} updating value: {} -> {}",
                    fullTopicName,
                    configEntryName,
                    currentValue,
                    newValue);
              }
            }
          });

      if (syncConfigOps.isEmpty() && deleteConfigOps.isEmpty()) {
        LOGGER.debug("Topic {} will not be updated, because it has no changes", fullTopicName);

        return Collections.emptyMap();
      }
    }

    // merge lists
    toBeSetConfigs.addAll(deleteConfigOps);

    Map<ConfigResource, Collection<AlterConfigOp>> configs = new HashMap<>();
    configs.put(new ConfigResource(Type.TOPIC, fullTopicName), toBeSetConfigs);

    if (!dryRun) {
      adminClient.incrementalAlterConfigs(configs).all().get();
    }
    return parseChangesBasedOnConfigs(toBeSetConfigs, entryToOldValueMap);
  }

  private List<AlterConfigOp> resolveDeleteEntries(
      String fullTopicName,
      Config currentConfigs,
      Set<String> newEntryKeys,
      Map<String, String> entryToOldValueMap) {
    return currentConfigs.entries().stream()
        // filter default values and static broker configs as they don't need to be defined in the
        // julie
        // config
        .filter(
            (entry ->
                !entry.isDefault()
                    && !entry.source().equals((ConfigEntry.ConfigSource.STATIC_BROKER_CONFIG))
                    && !newEntryKeys.contains(entry.name())))
        .map(
            entry -> {
              LOGGER.debug(
                  "Topic {} and config key {} deleting value", fullTopicName, entry.name());
              entryToOldValueMap.put(entry.name(), entry.value());
              return new AlterConfigOp(entry, OpType.DELETE);
            })
        .collect(Collectors.toList());
  }

  private Map<String, String> parseChangesBasedOnConfigs(
      List<AlterConfigOp> alterConfigOps, Map<String, String> entryToOldValueMap) {
    return alterConfigOps.stream()
        .filter(
            // (option to overwrite on and is changed value or the config is new) OR option to
            // overwrite is off
            c ->
                (!config.shouldOverwriteTopicsInSync()
                            && entryToOldValueMap.containsKey(c.configEntry().name())
                        || entryToOldValueMap.isEmpty())
                    || config.shouldOverwriteTopicsInSync())
        .collect(
            Collectors.toMap(
                entry -> entry.configEntry().name(),
                config -> {
                  String previousValue = entryToOldValueMap.get(config.configEntry().name());
                  if (config.opType().equals(OpType.SET)) {
                    return previousValue != null
                        ? "[Update] " + previousValue + " -> " + config.configEntry().value()
                        : "[Set] " + config.configEntry().value();
                  } else if (config.opType().equals(OpType.DELETE)) {
                    return "[Unset] " + previousValue + " ->  ";
                  }
                  // fallback case for other OPs
                  LOGGER.warn("Unsupported Operation used {}", config.opType());
                  return "[Unsupported Op] - " + config.opType();
                }));
  }

  private Config getActualTopicConfig(String topic) throws InterruptedException {
    ConfigResource resource = new ConfigResource(Type.TOPIC, topic);
    Collection<ConfigResource> resources = Collections.singletonList(resource);

    try {
      Map<ConfigResource, Config> configs = adminClient.describeConfigs(resources).all().get();

      return configs.get(resource);

    } catch (ExecutionException | UnknownTopicOrPartitionException e) {
      LOGGER.debug("Topic {} not found on the cluster - might be a new topic - continuing", topic);

      return new Config(Collections.emptyList());
    }
  }

  public void createTopic(Topic topic, String fullTopicName) throws IOException {
    NewTopic newTopic =
        new NewTopic(fullTopicName, topic.partitionsCount(), topic.replicationFactor())
            .configs(topic.getRawConfig());
    Collection<NewTopic> newTopics = Collections.singleton(newTopic);
    try {
      createAllTopics(newTopics);
    } catch (TopicExistsException ex) {
      LOGGER.info(ex);
    } catch (ExecutionException | InterruptedException e) {
      LOGGER.error(e);
      throw new IOException(e);
    }
  }

  private void createAllTopics(Collection<NewTopic> newTopics)
      throws ExecutionException, InterruptedException {
    adminClient.createTopics(newTopics).all().get();
  }

  public void deleteTopics(Collection<String> topics) throws IOException {
    try {
      adminClient.deleteTopics(topics).all().get();
    } catch (ExecutionException | InterruptedException e) {
      LOGGER.error(e);
      throw new IOException(e);
    }
  }

  public Map<String, Collection<AclBinding>> fetchAclsList() {
    Map<String, Collection<AclBinding>> acls = new HashMap<>();

    try {
      Collection<AclBinding> list = adminClient.describeAcls(AclBindingFilter.ANY).values().get();
      list.forEach(
          aclBinding -> {
            String name = aclBinding.pattern().name();
            if (acls.get(name) == null) {
              acls.put(name, new ArrayList<>());
            }
            Collection<AclBinding> updatedList = acls.get(name);
            updatedList.add(aclBinding);
            acls.put(name, updatedList);
          });
    } catch (Exception e) {
      return new HashMap<>();
    }
    return acls;
  }

  public void createAcls(Collection<AclBinding> acls) throws IOException {
    try {
      adminClient.createAcls(acls).all().get();
    } catch (InvalidConfigurationException ex) {
      LOGGER.error(ex);
      throw ex;
    } catch (ExecutionException | InterruptedException e) {
      LOGGER.error(e);
    }
  }

  public void close() {
    adminClient.close();
  }
}
