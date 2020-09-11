package com.purbon.kafka.topology;

import com.purbon.kafka.topology.adminclient.AclBuilder;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConfigOp.OpType;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
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
  private final TopologyBuilderConfig config;

  public TopologyBuilderAdminClient(AdminClient adminClient, TopologyBuilderConfig config) {
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

  public Set<String> listTopics() throws IOException {
    return listTopics(new ListTopicsOptions());
  }

  public Set<String> listApplicationTopics() throws IOException {
    ListTopicsOptions options = new ListTopicsOptions();
    options.listInternal(false);
    return listTopics(options);
  }

  public void updateTopicConfig(Topic topic, String fullTopicName) throws IOException {
    try {
      updateTopicConfigPostAK23(topic, fullTopicName);
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

  public void createTopic(Topic topic, String fullTopicName) throws IOException {

    int numPartitions =
        Integer.parseInt(topic.getConfig().getOrDefault(TopicManager.NUM_PARTITIONS, "3"));
    short replicationFactor =
        Short.parseShort(topic.getConfig().getOrDefault(TopicManager.REPLICATION_FACTOR, "2"));

    NewTopic newTopic =
        new NewTopic(fullTopicName, numPartitions, replicationFactor).configs(topic.rawConfig());
    Collection<NewTopic> newTopics = Collections.singleton(newTopic);
    try {
      createAllTopics(newTopics);
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

  /**
   * Find cluster inter protocol version, used to determine the minimum level of Api compatibility
   *
   * @return String, the current Kafka Protocol version
   */
  private String findKafkaVersion() throws IOException {
    ConfigResource resource = new ConfigResource(Type.BROKER, "inter.broker.protocol.version");
    String kafkaVersion = "";
    try {
      Map<ConfigResource, Config> configs =
          adminClient.describeConfigs(Collections.singletonList(resource)).all().get();
      kafkaVersion =
          configs.get(resource).get("inter.broker.protocol.version").value().split("-")[0];
    } catch (ExecutionException | InterruptedException e) {
      LOGGER.error(e);
      throw new IOException(e);
    }
    return kafkaVersion;
  }

  public List<AclBinding> setAclsForProducer(String principal, String topic) {
    List<AclBinding> acls = new ArrayList<>();
    acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.DESCRIBE));
    acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.WRITE));
    return acls;
  }

  public List<AclBinding> setAclsForConsumer(Consumer consumer, String topic) {

    List<AclBinding> acls = new ArrayList<>();
    acls.add(
        buildTopicLevelAcl(
            consumer.getPrincipal(), topic, PatternType.LITERAL, AclOperation.DESCRIBE));
    acls.add(
        buildTopicLevelAcl(consumer.getPrincipal(), topic, PatternType.LITERAL, AclOperation.READ));
    acls.add(
        buildGroupLevelAcl(
            consumer.getPrincipal(),
            consumer.groupString(),
            consumer.groupString().equals("*") ? PatternType.PREFIXED : PatternType.LITERAL,
            AclOperation.READ));
    return acls;
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

  public List<AclBinding> setAclForSchemaRegistry(SchemaRegistryInstance schemaRegistry) {
    List<AclBinding> bindings =
        Arrays.asList(AclOperation.DESCRIBE_CONFIGS, AclOperation.WRITE, AclOperation.READ).stream()
            .map(
                aclOperation ->
                    buildTopicLevelAcl(
                        schemaRegistry.getPrincipal(),
                        schemaRegistry.topicString(),
                        PatternType.LITERAL,
                        aclOperation))
            .collect(Collectors.toList());
    return bindings;
  }

  public List<AclBinding> setAclsForControlCenter(String principal, String appId) {
    List<AclBinding> bindings = new ArrayList<>();

    bindings.add(buildGroupLevelAcl(principal, appId, PatternType.PREFIXED, AclOperation.READ));
    bindings.add(
        buildGroupLevelAcl(principal, appId + "-command", PatternType.PREFIXED, AclOperation.READ));

    Arrays.asList(
            config.getConfluentMonitoringTopic(),
            config.getConfluentCommandTopic(),
            config.getConfluentMetricsTopic())
        .forEach(
            topic ->
                Stream.of(
                        AclOperation.WRITE,
                        AclOperation.READ,
                        AclOperation.CREATE,
                        AclOperation.DESCRIBE)
                    .map(
                        aclOperation ->
                            buildTopicLevelAcl(principal, topic, PatternType.LITERAL, aclOperation))
                    .forEach(bindings::add));

    Stream.of(AclOperation.WRITE, AclOperation.READ, AclOperation.CREATE, AclOperation.DESCRIBE)
        .map(
            aclOperation ->
                buildTopicLevelAcl(principal, appId, PatternType.PREFIXED, aclOperation))
        .forEach(bindings::add);

    ResourcePattern resourcePattern =
        new ResourcePattern(ResourceType.CLUSTER, "kafka-cluster", PatternType.LITERAL);
    AccessControlEntry entry =
        new AccessControlEntry(principal, "*", AclOperation.DESCRIBE, AclPermissionType.ALLOW);
    bindings.add(new AclBinding(resourcePattern, entry));

    entry =
        new AccessControlEntry(
            principal, "*", AclOperation.DESCRIBE_CONFIGS, AclPermissionType.ALLOW);
    bindings.add(new AclBinding(resourcePattern, entry));
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
    return acls;
  }

  public List<AclBinding> setAclsForConnect(Connector connector) {

    String principal = connector.getPrincipal();
    List<String> readTopics = connector.getTopics().get("read");
    List<String> writeTopics = connector.getTopics().get("write");

    List<AclBinding> acls = new ArrayList<>();

    List<String> topics =
        Arrays.asList(
            connector.statusTopicString(),
            connector.offsetTopicString(),
            connector.configsTopicString());

    for (String topic : topics) {
      acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.READ));
      acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.WRITE));
    }

    ResourcePattern resourcePattern =
        new ResourcePattern(ResourceType.CLUSTER, "kafka-cluster", PatternType.LITERAL);
    AccessControlEntry entry =
        new AccessControlEntry(principal, "*", AclOperation.CREATE, AclPermissionType.ALLOW);
    acls.add(new AclBinding(resourcePattern, entry));

    resourcePattern =
        new ResourcePattern(ResourceType.GROUP, connector.groupString(), PatternType.LITERAL);
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
    return acls;
  }

  public void createAcls(Collection<AclBinding> acls) throws IOException {
    try {
      adminClient.createAcls(acls).all().get();
    } catch (ExecutionException | InterruptedException e) {
      LOGGER.error(e);
      throw new IOException(e);
    }
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

  public void close() {
    adminClient.close();
  }
}
