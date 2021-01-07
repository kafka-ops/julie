package com.purbon.kafka.topology.roles.acls;

import static java.util.Arrays.asList;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.api.adminclient.AclBuilder;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import com.purbon.kafka.topology.roles.TopologyAclBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AclsBindingsBuilder implements BindingsBuilderProvider {

  private static final Logger LOGGER = LogManager.getLogger(AclsBindingsBuilder.class);

  private final TopologyBuilderConfig config;

  public AclsBindingsBuilder(TopologyBuilderConfig config) {
    this.config = config;
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForConnect(
      Connector connector, String topicPrefixNotInUse) {

    String principal = translate(connector.getPrincipal());
    List<String> readTopics = connector.getTopics().get("read");
    List<String> writeTopics = connector.getTopics().get("write");

    List<AclBinding> acls = new ArrayList<>();

    List<String> topics =
        asList(
            connector.statusTopicString(),
            connector.offsetTopicString(),
            connector.configsTopicString());

    for (String topic : topics) {
      acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.READ));
      acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.WRITE));
    }

    if (config.enabledConnectorTopicCreateAcl()) {
      ResourcePattern resourcePattern =
          new ResourcePattern(ResourceType.CLUSTER, "kafka-cluster", PatternType.LITERAL);
      AccessControlEntry entry =
          new AccessControlEntry(principal, "*", AclOperation.CREATE, AclPermissionType.ALLOW);
      acls.add(new AclBinding(resourcePattern, entry));
    }

    ResourcePattern resourcePattern =
        new ResourcePattern(ResourceType.GROUP, connector.groupString(), PatternType.LITERAL);
    AccessControlEntry entry =
        new AccessControlEntry(principal, "*", AclOperation.READ, AclPermissionType.ALLOW);
    acls.add(new AclBinding(resourcePattern, entry));

    if (readTopics != null) {
      readTopics.forEach(
          topic ->
              acls.add(
                  buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.READ)));
    }

    if (writeTopics != null) {
      writeTopics.forEach(
          topic ->
              acls.add(
                  buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.WRITE)));
    }

    return toList(acls.stream());
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForStreamsApp(
      String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {
    return toList(streamsAppStream(translate(principal), topicPrefix, readTopics, writeTopics));
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForConsumers(
      Collection<Consumer> consumers, String resource, boolean prefixed) {
    return toList(
        consumers.stream().flatMap(consumer -> consumerAclsStream(consumer, resource, prefixed)));
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForProducers(
      Collection<Producer> producers, String resource, boolean prefixed) {
    return toList(
        producers.stream().flatMap(producer -> producerAclsStream(producer, resource, prefixed)));
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForSchemaRegistry(
      SchemaRegistryInstance schemaRegistry) {
    return toList(schemaRegistryAclsStream(schemaRegistry));
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForControlCenter(String principal, String appId) {
    return toList(controlCenterStream(translate(principal), appId));
  }

  private List<TopologyAclBinding> toList(Stream<AclBinding> bindingStream) {
    return bindingStream.map(TopologyAclBinding::new).collect(Collectors.toList());
  }

  private Stream<AclBinding> producerAclsStream(Producer producer, String topic, boolean prefixed) {
    PatternType patternType = prefixed ? PatternType.PREFIXED : PatternType.LITERAL;

    List<AclBinding> bindings = new ArrayList<>();
    String principal = translate(producer.getPrincipal());
    bindings.addAll(
        Arrays.asList(
            buildTopicLevelAcl(principal, topic, patternType, AclOperation.DESCRIBE),
            buildTopicLevelAcl(principal, topic, patternType, AclOperation.WRITE)));

    producer
        .getTransactionId()
        .ifPresent(
            transactionId -> {
              bindings.add(
                  buildTransactionIdLevelAcl(
                      producer.getPrincipal(),
                      transactionId,
                      PatternType.LITERAL,
                      AclOperation.DESCRIBE));
              bindings.add(
                  buildTransactionIdLevelAcl(
                      producer.getPrincipal(),
                      transactionId,
                      PatternType.LITERAL,
                      AclOperation.WRITE));
            });

    if (producer.getTransactionId().isPresent() || producer.getIdempotence().isPresent()) {
      ResourcePattern resourcePattern =
          new ResourcePattern(ResourceType.CLUSTER, "kafka-cluster", PatternType.LITERAL);
      AccessControlEntry entry =
          new AccessControlEntry(
              producer.getPrincipal(), "*", AclOperation.IDEMPOTENT_WRITE, AclPermissionType.ALLOW);
      bindings.add(new AclBinding(resourcePattern, entry));
    }

    return bindings.stream();
  }

  private Stream<AclBinding> consumerAclsStream(Consumer consumer, String topic, boolean prefixed) {
    PatternType patternType = prefixed ? PatternType.PREFIXED : PatternType.LITERAL;
    String principal = translate(consumer.getPrincipal());
    return Stream.of(
        buildTopicLevelAcl(principal, topic, patternType, AclOperation.DESCRIBE),
        buildTopicLevelAcl(principal, topic, patternType, AclOperation.READ),
        buildGroupLevelAcl(principal, consumer.groupString(), patternType, AclOperation.READ));
  }

  private Stream<AclBinding> streamsAppStream(
      String principal, String prefix, List<String> readTopics, List<String> writeTopics) {

    List<AclBinding> acls = new ArrayList<>();

    readTopics.forEach(
        topic ->
            acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.READ)));

    writeTopics.forEach(
        topic ->
            acls.add(
                buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.WRITE)));

    acls.add(buildTopicLevelAcl(principal, prefix, PatternType.PREFIXED, AclOperation.ALL));

    acls.add(buildGroupLevelAcl(principal, prefix, PatternType.PREFIXED, AclOperation.READ));

    return acls.stream();
  }

  private Stream<AclBinding> schemaRegistryAclsStream(SchemaRegistryInstance schemaRegistry) {
    String principal = translate(schemaRegistry.getPrincipal());
    List<AclBinding> bindings =
        Stream.of(AclOperation.DESCRIBE_CONFIGS, AclOperation.WRITE, AclOperation.READ)
            .map(
                aclOperation ->
                    buildTopicLevelAcl(
                        principal, schemaRegistry.topicString(), PatternType.LITERAL, aclOperation))
            .collect(Collectors.toList());
    return bindings.stream();
  }

  private Stream<AclBinding> controlCenterStream(String principal, String appId) {
    List<AclBinding> bindings = new ArrayList<>();

    bindings.add(buildGroupLevelAcl(principal, appId, PatternType.PREFIXED, AclOperation.READ));
    bindings.add(
        buildGroupLevelAcl(principal, appId + "-command", PatternType.PREFIXED, AclOperation.READ));

    asList(
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
    return bindings.stream();
  }

  private String translate(String namedPrincipal) {
      return namedPrincipal;
  }

  private AclBinding buildTopicLevelAcl(
      String principal, String topic, PatternType patternType, AclOperation op) {
    return new AclBuilder(principal)
        .addResource(ResourceType.TOPIC, topic, patternType)
        .addControlEntry("*", op, AclPermissionType.ALLOW)
        .build();
  }

  private AclBinding buildTransactionIdLevelAcl(
      String principal, String transactionId, PatternType patternType, AclOperation op) {
    return new AclBuilder(principal)
        .addResource(ResourceType.TRANSACTIONAL_ID, transactionId, patternType)
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
}
