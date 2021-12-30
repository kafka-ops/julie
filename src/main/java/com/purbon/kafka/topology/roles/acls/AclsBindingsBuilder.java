package com.purbon.kafka.topology.roles.acls;

import static java.util.Arrays.asList;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.api.adminclient.AclBuilder;
import com.purbon.kafka.topology.model.JulieRoleAcl;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KSqlApp;
import com.purbon.kafka.topology.model.users.Other;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.platform.KsqlServerInstance;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.utils.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
  private static final String KAFKA_CLUSTER_NAME = "kafka-cluster";

  private final Configuration config;

  public AclsBindingsBuilder(Configuration config) {
    this.config = config;
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForConnect(
      Connector connector, String topicPrefixNotInUse) {

    String principal = connector.getPrincipal();
    Stream<String> readTopics = Utils.asNullableStream(connector.getTopics().get("read"));
    Stream<String> writeTopics = Utils.asNullableStream(connector.getTopics().get("write"));

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
          new ResourcePattern(ResourceType.CLUSTER, KAFKA_CLUSTER_NAME, PatternType.LITERAL);
      AccessControlEntry entry =
          new AccessControlEntry(principal, "*", AclOperation.CREATE, AclPermissionType.ALLOW);
      acls.add(new AclBinding(resourcePattern, entry));
    }

    ResourcePattern resourcePattern =
        new ResourcePattern(ResourceType.GROUP, connector.groupString(), PatternType.LITERAL);
    AccessControlEntry entry =
        new AccessControlEntry(principal, "*", AclOperation.READ, AclPermissionType.ALLOW);
    acls.add(new AclBinding(resourcePattern, entry));

    readTopics
        .map(topic -> buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.READ))
        .forEach(acls::add);

    writeTopics
        .map(topic -> buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.WRITE))
        .forEach(acls::add);

    return toList(acls.stream());
  }

  @Override
  public List<TopologyAclBinding> buildBindingsForStreamsApp(
      String principal,
      String topicPrefix,
      List<String> readTopics,
      List<String> writeTopics,
      boolean eos) {
    return toList(streamsAppStream(principal, topicPrefix, readTopics, writeTopics, eos));
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
    return toList(controlCenterStream(principal, appId));
  }

  @Override
  public Collection<TopologyAclBinding> buildBindingsForKSqlServer(KsqlServerInstance ksqlServer) {
    return toList(ksqlServerStream(ksqlServer));
  }

  @Override
  public Collection<TopologyAclBinding> buildBindingsForKSqlApp(KSqlApp app, String prefix) {
    return toList(ksqlAppStream(app, prefix));
  }

  @Override
  public Collection<TopologyAclBinding> buildBindingsForJulieRole(
      Other other, String name, List<JulieRoleAcl> acls) throws IOException {

    List<TopologyAclBinding> bindings = new ArrayList<>();
    for (JulieRoleAcl acl : acls) {
      var resourceType = ResourceType.fromString(acl.getResourceType());
      var patternType = PatternType.fromString(acl.getPatternType());
      var aclOperation = AclOperation.fromString(acl.getOperation());
      if (resourceType.isUnknown() || patternType.isUnknown() || aclOperation.isUnknown()) {
        throw new IOException(
            "Unknown ACL setting being used resourceType="
                + acl.getResourceType()
                + " ("
                + resourceType
                + ")"
                + ", patternType="
                + acl.getPatternType()
                + " ("
                + patternType
                + ")"
                + ", aclOperation="
                + acl.getOperation()
                + " ("
                + aclOperation
                + ")");
      }
      var binding =
          new AclBuilder(other.getPrincipal())
              .addResource(resourceType, acl.getResourceName(), patternType)
              .addControlEntry(acl.getHost(), aclOperation, AclPermissionType.ALLOW)
              .build();
      bindings.add(new TopologyAclBinding(binding));
    }
    return bindings;
  }

  private List<TopologyAclBinding> toList(Stream<AclBinding> bindingStream) {
    return bindingStream.map(TopologyAclBinding::new).collect(Collectors.toList());
  }

  private Stream<AclBinding> producerAclsStream(Producer producer, String topic, boolean prefixed) {
    PatternType patternType = prefixed ? PatternType.PREFIXED : PatternType.LITERAL;

    List<AclBinding> bindings = new ArrayList<>();
    String principal = producer.getPrincipal();
    Stream.of(AclOperation.DESCRIBE, AclOperation.WRITE)
        .map(aclOperation -> buildTopicLevelAcl(principal, topic, patternType, aclOperation))
        .forEach(bindings::add);

    producer
        .getTransactionId()
        .ifPresent(
            transactionId -> {
              bindings.add(
                  buildTransactionIdLevelAcl(
                      producer.getPrincipal(),
                      evaluateResourcePattern(transactionId),
                      evaluateResourcePatternType(transactionId),
                      AclOperation.DESCRIBE));
              bindings.add(
                  buildTransactionIdLevelAcl(
                      producer.getPrincipal(),
                      evaluateResourcePattern(transactionId),
                      evaluateResourcePatternType(transactionId),
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
    String principal = consumer.getPrincipal();
    return Stream.of(
        buildTopicLevelAcl(principal, topic, patternType, AclOperation.DESCRIBE),
        buildTopicLevelAcl(principal, topic, patternType, AclOperation.READ),
        buildGroupLevelAcl(
            principal,
            evaluateResourcePattern(consumer.groupString()),
            evaluateResourcePatternType(consumer.groupString()),
            AclOperation.READ));
  }

  private Stream<AclBinding> streamsAppStream(
      String principal,
      String prefix,
      List<String> readTopics,
      List<String> writeTopics,
      boolean eos) {

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

    if (eos) {
      acls.add(
          buildTransactionIdLevelAcl(principal, prefix, PatternType.PREFIXED, AclOperation.WRITE));
      acls.add(
          buildTransactionIdLevelAcl(
              principal, prefix, PatternType.PREFIXED, AclOperation.DESCRIBE));
    }

    return acls.stream();
  }

  private Stream<AclBinding> schemaRegistryAclsStream(SchemaRegistryInstance schemaRegistry) {
    String principal = schemaRegistry.getPrincipal();
    List<AclBinding> bindings =
        Stream.of(
                AclOperation.CREATE,
                AclOperation.DESCRIBE_CONFIGS,
                AclOperation.DESCRIBE,
                AclOperation.WRITE,
                AclOperation.READ)
            .map(
                aclOperation ->
                    buildTopicLevelAcl(
                        principal, schemaRegistry.topicString(), PatternType.LITERAL, aclOperation))
            .collect(Collectors.toList());

    bindings.add(
        buildTopicLevelAcl(
            principal,
            schemaRegistry.consumerOffsetsTopicString(),
            PatternType.LITERAL,
            AclOperation.DESCRIBE));

    bindings.add(
        buildGroupLevelAcl(
            principal, schemaRegistry.groupString(), PatternType.LITERAL, AclOperation.READ));

    return bindings.stream();
  }

  private Stream<AclBinding> controlCenterStream(String principal, String appId) {
    List<AclBinding> bindings = new ArrayList<>();

    bindings.add(buildGroupLevelAcl(principal, appId, PatternType.PREFIXED, AclOperation.READ));
    bindings.add(
        buildGroupLevelAcl(principal, appId + "-command", PatternType.PREFIXED, AclOperation.READ));
    bindings.add(buildGroupLevelAcl(principal, "*", PatternType.LITERAL, AclOperation.DESCRIBE));

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
                buildTopicLevelAcl(
                    principal, "_confluent-controlcenter", PatternType.PREFIXED, aclOperation))
        .forEach(bindings::add);

    bindings.add(buildTopicLevelAcl(principal, "*", PatternType.LITERAL, AclOperation.CREATE));

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

  private Stream<AclBinding> ksqlServerStream(KsqlServerInstance ksqlServer) {
    String principal = ksqlServer.getPrincipal();

    List<AclBinding> bindings = new ArrayList<>();

    ResourcePattern resourcePattern =
        new ResourcePattern(ResourceType.CLUSTER, "kafka-cluster", PatternType.LITERAL);
    AccessControlEntry entry =
        new AccessControlEntry(
            principal, "*", AclOperation.DESCRIBE_CONFIGS, AclPermissionType.ALLOW);
    bindings.add(new AclBinding(resourcePattern, entry));

    bindings.add(
        buildTopicLevelAcl(
            principal, ksqlServer.internalTopics(), PatternType.PREFIXED, AclOperation.ALL));
    bindings.add(
        buildTopicLevelAcl(
            principal, ksqlServer.processingLogTopic(), PatternType.LITERAL, AclOperation.ALL));
    bindings.add(
        buildGroupLevelAcl(
            principal, ksqlServer.consumerGroupPrefix(), PatternType.PREFIXED, AclOperation.ALL));

    return bindings.stream();
  }

  private Stream<AclBinding> ksqlAppStream(KSqlApp app, String prefix) {
    String principal = app.getPrincipal();

    List<AclBinding> bindings = new ArrayList<>();

    Optional<List<String>> readTopics = Optional.ofNullable(app.getTopics().get("read"));
    readTopics.ifPresent(
        topics -> {
          for (String topic : topics) {
            bindings.add(
                buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.READ));
          }
        });

    Optional<List<String>> writeTopics = Optional.ofNullable(app.getTopics().get("write"));
    writeTopics.ifPresent(
        topics -> {
          for (String topic : topics) {
            bindings.add(
                buildTopicLevelAcl(principal, topic, PatternType.LITERAL, AclOperation.WRITE));
          }
        });

    bindings.add(buildTopicLevelAcl(principal, prefix, PatternType.PREFIXED, AclOperation.ALL));
    bindings.add(buildGroupLevelAcl(principal, prefix, PatternType.PREFIXED, AclOperation.ALL));

    return bindings.stream();
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

  private boolean isResourcePrefixed(String res) {
    return res.length() > 1 && res.endsWith("*");
  }

  private String evaluateResourcePattern(String res) {
    return isResourcePrefixed(res) ? res.replaceFirst(".$", "") : res;
  }

  private PatternType evaluateResourcePatternType(String res) {
    return isResourcePrefixed(res) ? PatternType.PREFIXED : PatternType.LITERAL;
  }
}
