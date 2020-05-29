package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.ConfigResource.Type;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopologyBuilderAdminClient {

  private static final Logger LOGGER = LogManager.getLogger(TopologyBuilderAdminClient.class);

  private final AdminClient adminClient;

  public TopologyBuilderAdminClient(AdminClient adminClient) {
    this.adminClient = adminClient;
  }

  public Set<String> listTopics() {
    Set<String> listOfTopics = new HashSet<>();
    try {
      listOfTopics = adminClient.listTopics().names().get();
    } catch (InterruptedException e) {
      LOGGER.error(e);
    } catch (ExecutionException e) {
      LOGGER.error(e);
    }
    return listOfTopics;
  }

  public void updateTopicConfig(Topic topic, String fullTopicName) {

    try {
      updateTopicConfigPostAK23(topic, fullTopicName);
    } catch (ExecutionException ex) {
      LOGGER.error(ex);
    } catch (InterruptedException ex) {
      LOGGER.error(ex);
    }
  }

  public void clearAcls() {
    Collection<AclBindingFilter> filters = new ArrayList<>();
    filters.add(AclBindingFilter.ANY);
    clearAcls(filters);
  }

  public void clearAcls(TopologyAclBinding aclBinding) {
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

  private void clearAcls(Collection<AclBindingFilter> filters) {
    try {
      adminClient.deleteAcls(filters).all().get();
    } catch (Exception e) {
      LOGGER.error(e);
    }
  }

  private void updateTopicConfigPostAK23(Topic topic, String fullTopicName)
      throws ExecutionException, InterruptedException {

    Config currentConfigs = getActualTopicConfig(fullTopicName);

    Map<ConfigResource, Collection<AlterConfigOp>> configs = new HashMap<>();
    ArrayList<AlterConfigOp> listOfValues = new ArrayList<>();

    topic
        .rawConfig()
        .forEach(
            (configKey, configValue) -> {
              listOfValues.add(
                  new AlterConfigOp(new ConfigEntry(configKey, configValue), OpType.SET));
            });
    Set<String> newEntryKeys = topic.rawConfig().keySet();

    currentConfigs
        .entries()
        .forEach(
            entry -> {
              if (!newEntryKeys.contains(entry.name())) {
                listOfValues.add(new AlterConfigOp(entry, OpType.DELETE));
              }
            });

    configs.put(new ConfigResource(Type.TOPIC, fullTopicName), listOfValues);

    adminClient.incrementalAlterConfigs(configs).all().get();
  }

  private Config getActualTopicConfig(String topic)
      throws ExecutionException, InterruptedException {
    ConfigResource resource = new ConfigResource(Type.TOPIC, topic);
    Collection<ConfigResource> resources = Collections.singletonList(resource);

    Map<ConfigResource, Config> configs = adminClient.describeConfigs(resources).all().get();

    return configs.get(resource);
  }

  public void createTopic(Topic topic, String fullTopicName) {

    int numPartitions =
        Integer.parseInt(topic.getConfig().getOrDefault(TopicManager.NUM_PARTITIONS, "3"));
    short replicationFactor =
        Short.parseShort(topic.getConfig().getOrDefault(TopicManager.REPLICATION_FACTOR, "2"));

    NewTopic newTopic =
        new NewTopic(fullTopicName, numPartitions, replicationFactor).configs(topic.rawConfig());
    Collection<NewTopic> newTopics = Collections.singleton(newTopic);
    try {
      createAllTopics(newTopics);
    } catch (InterruptedException e) {
      LOGGER.error(e);
    } catch (ExecutionException e) {
      LOGGER.error(e);
    }
  }

  private void createAllTopics(Collection<NewTopic> newTopics)
      throws ExecutionException, InterruptedException {
    adminClient.createTopics(newTopics).all().get();
  }

  public void deleteTopic(String topic) {
    deleteTopics(Collections.singletonList(topic));
  }

  public void deleteTopics(Collection<String> topics) {
    try {
      adminClient.deleteTopics(topics).all().get();
    } catch (InterruptedException e) {
      LOGGER.error(e);
    } catch (ExecutionException e) {
      LOGGER.error(e);
    }
  }

  /**
   * Find cluster inter protocol version, used to determine the minimum level of Api compatibility
   *
   * @return String, the current Kafka Protocol version
   */
  private String findKafkaVersion() {
    ConfigResource resource = new ConfigResource(Type.BROKER, "inter.broker.protocol.version");
    String kafkaVersion = "";
    try {
      Map<ConfigResource, Config> configs =
          adminClient.describeConfigs(Collections.singletonList(resource)).all().get();
      kafkaVersion =
          configs.get(resource).get("inter.broker.protocol.version").value().split("-")[0];
    } catch (InterruptedException e) {
      LOGGER.error(e);
    } catch (ExecutionException e) {
      LOGGER.error(e);
    }
    return kafkaVersion;
  }

  public List<AclBinding> setAclsForProducer(String principal, String topic) {
    List<AclBinding> acls = new ArrayList<>();
    acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.DESCRIBE));
    acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.WRITE));
    createAcls(acls);
    return acls;
  }

  public List<AclBinding> setAclsForConsumer(String principal, String topic) {

    List<AclBinding> acls = new ArrayList<>();
    acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.DESCRIBE));
    acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.READ));
    acls.add(buildGroupLevelAcl(principal, "*", PatternType.LITERAL, AclOperation.READ));
    createAcls(acls);
    return acls;
  }

  private void createAcls(Collection<AclBinding> acls) {
    try {
      adminClient.createAcls(acls).all().get();
    } catch (InterruptedException e) {
      LOGGER.error(e);
    } catch (ExecutionException e) {
      LOGGER.error(e);
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

  public Collection<AclBinding> fetchAclsList(String principal, String topic) {
    Collection<AclBinding> aclsList = null;

    ResourcePatternFilter resourceFilter =
        new ResourcePatternFilter(ResourceType.TOPIC, topic, PatternType.ANY);
    AccessControlEntryFilter accessControlEntryFilter =
        new AccessControlEntryFilter(principal, "*", AclOperation.ALL, AclPermissionType.ANY);
    AclBindingFilter filter = new AclBindingFilter(resourceFilter, accessControlEntryFilter);

    try {
      aclsList = adminClient.describeAcls(filter).values().get();
    } catch (Exception e) {
      return new ArrayList<>();
    }
    return aclsList;
  }

  public List<AclBinding> setAclForSchemaRegistry(String principal) {
    List<AclBinding> bindings =
        Arrays.asList(AclOperation.DESCRIBE_CONFIGS, AclOperation.WRITE, AclOperation.READ).stream()
            .map(
                aclOperation -> {
                  return buildTopicLevelAcl(
                      principal, "_schemas", PatternType.LITERAL, aclOperation);
                })
            .collect(Collectors.toList());
    createAcls(bindings);
    return bindings;
  }

  public List<AclBinding> setAclsForStreamsApp(
      String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {

    List<AclBinding> acls = new ArrayList<>();

    readTopics.forEach(
        topic -> {
          acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.READ));
        });

    writeTopics.forEach(
        topic -> {
          acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.WRITE));
        });

    acls.add(buildTopicLevelAcl(principal, topicPrefix, PatternType.PREFIXED, AclOperation.ALL));
    createAcls(acls);
    return acls;
  }

  public List<AclBinding> setAclsForConnect(
      String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {

    List<AclBinding> acls = new ArrayList<>();

    List<String> topics = Arrays.asList("connect-status", "connect-offsets", "connect-configs");
    for (String topic : topics) {
      acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.READ));
      acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.WRITE));
    }

    ResourcePattern resourcePattern =
        new ResourcePattern(ResourceType.CLUSTER, "kafka-cluster", PatternType.LITERAL);
    AccessControlEntry entry =
        new AccessControlEntry(principal, "*", AclOperation.CREATE, AclPermissionType.ALLOW);
    acls.add(new AclBinding(resourcePattern, entry));

    resourcePattern = new ResourcePattern(ResourceType.GROUP, "*", PatternType.LITERAL);
    entry = new AccessControlEntry(principal, "*", AclOperation.READ, AclPermissionType.ALLOW);
    acls.add(new AclBinding(resourcePattern, entry));

    if (readTopics != null) {
      readTopics.forEach(
          topic -> {
            acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.READ));
          });
    }

    if (writeTopics != null) {
      writeTopics.forEach(
          topic -> {
            acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.WRITE));
          });
    }

    createAcls(acls);
    return acls;
  }

  private AclBinding buildTopicLevelAcl(
      String principal, String topic, PatternType patternType, AclOperation op) {
    return new AclBuilder(principal)
        .addResource(ResourceType.TOPIC, topic, patternType)
        .addControlEntry("*", op, AclPermissionType.ALLOW)
        .build();
  }

  private AclBinding buildGroupLevelAcl(
      String principal, String group, PatternType patternType, AclOperation op) {
    return new AclBuilder(principal)
        .addResource(ResourceType.GROUP, group, patternType)
        .addControlEntry("*", op, AclPermissionType.ALLOW)
        .build();
  }

  private class AclBuilder {

    private ResourcePattern resourcePattern;
    private AccessControlEntry entry;
    private String principal;

    public AclBuilder(String principal) {
      this.principal = principal;
    }

    public AclBuilder addResource(ResourceType resourceType, String name, PatternType patternType) {
      resourcePattern = new ResourcePattern(resourceType, name, patternType);
      return this;
    }

    public AclBuilder addControlEntry(
        String host, AclOperation op, AclPermissionType permissionType) {
      entry = new AccessControlEntry(principal, host, op, permissionType);
      return this;
    }

    public AclBinding build() {
      return new AclBinding(resourcePattern, entry);
    }
  }
}
