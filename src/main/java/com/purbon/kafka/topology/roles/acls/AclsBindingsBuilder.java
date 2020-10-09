package com.purbon.kafka.topology.roles.acls;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.api.adminclient.AclBuilder;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
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

public class AclsBindingsBuilder implements BindingsBuilderProvider {

  private final TopologyBuilderConfig config;

  public AclsBindingsBuilder(TopologyBuilderConfig config) {
    this.config = config;
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForConnect(Connector connector, String topicPrefix) {
    return setAclsForConnect(connector).stream()
        .map(TopologyAclBinding::new)
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForStreamsApp(
      String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {
    return setAclsForStreamsApp(principal, topicPrefix, readTopics, writeTopics).stream()
        .map(TopologyAclBinding::new)
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForConsumers(
      Collection<Consumer> consumers, String topic) {
    return consumers.stream()
        .flatMap(
            consumer -> setAclsForConsumer(consumer, topic).stream().map(TopologyAclBinding::new))
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForProducers(
      Collection<String> principals, String topic) {
    return principals.stream()
        .flatMap(
            principal -> setAclsForProducer(principal, topic).stream().map(TopologyAclBinding::new))
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForSchemaRegistry(
      SchemaRegistryInstance schemaRegistry) {
    return setAclForSchemaRegistry(schemaRegistry).stream()
        .map(TopologyAclBinding::new)
        .collect(Collectors.toList());
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForControlCenter(String principal, String appId) {
    return setAclsForControlCenter(principal, appId).stream()
        .map(TopologyAclBinding::new)
        .collect(Collectors.toList());
  }

  private List<AclBinding> setAclsForProducer(String principal, String topic) {
    List<AclBinding> acls = new ArrayList<>();
    acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.DESCRIBE));
    acls.add(buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.WRITE));
    return acls;
  }

  private List<AclBinding> setAclsForConsumer(Consumer consumer, String topic) {

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

  private List<AclBinding> setAclsForConnect(Connector connector) {

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

  private List<AclBinding> setAclsForStreamsApp(
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

  private List<AclBinding> setAclForSchemaRegistry(SchemaRegistryInstance schemaRegistry) {
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
}
