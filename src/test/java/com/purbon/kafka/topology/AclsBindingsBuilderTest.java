package com.purbon.kafka.topology;

import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.api.adminclient.AclBuilder;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.roles.acls.AclsBindingsBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Before;
import org.junit.Test;

public class AclsBindingsBuilderTest {

  TopologyBuilderConfig config;
  AclsBindingsBuilder builder;

  @Before
  public void before() {
    config = new TopologyBuilderConfig();
    builder = new AclsBindingsBuilder(config);
  }

  @Test
  public void testConsumerAclsBuilder() {

    Consumer consumer = new Consumer("User:foo");

    List<TopologyAclBinding> aclBindings =
        builder.buildBindingsForConsumers(Collections.singleton(consumer), "bar", false);
    assertThat(aclBindings.size()).isEqualTo(3);
    assertThat(aclBindings)
        .contains(buildTopicLevelAcl("User:foo", "bar", PatternType.LITERAL, AclOperation.READ));
    assertThat(aclBindings)
        .contains(
            buildTopicLevelAcl("User:foo", "bar", PatternType.LITERAL, AclOperation.DESCRIBE));
    assertThat(aclBindings)
        .contains(buildGroupLevelAcl("User:foo", "*", PatternType.LITERAL, AclOperation.READ));
  }

  @Test
  public void testProducerAclsBuilder() {
    Producer producer = new Producer("User:foo");
    List<TopologyAclBinding> aclBindings =
        builder.buildBindingsForProducers(Collections.singleton(producer), "bar", false);
    assertThat(aclBindings.size()).isEqualTo(2);
    assertThat(aclBindings)
        .contains(buildTopicLevelAcl("User:foo", "bar", PatternType.LITERAL, AclOperation.WRITE));
    assertThat(aclBindings)
        .contains(
            buildTopicLevelAcl("User:foo", "bar", PatternType.LITERAL, AclOperation.DESCRIBE));
  }

  @Test
  public void testProducerWithTxIdAclsBuilder() {
    Producer producer = new Producer("User:foo");
    producer.setTransactionId(Optional.of("1234"));
    List<TopologyAclBinding> aclBindings =
        builder.buildBindingsForProducers(Collections.singleton(producer), "bar", false);
    assertThat(aclBindings.size()).isEqualTo(5);

    assertThat(aclBindings)
        .contains(buildTopicLevelAcl("User:foo", "bar", PatternType.LITERAL, AclOperation.WRITE));
    assertThat(aclBindings)
        .contains(
            buildTopicLevelAcl("User:foo", "bar", PatternType.LITERAL, AclOperation.DESCRIBE));

    assertThat(aclBindings)
        .contains(
            buildTransactionIdLevelAcl(
                producer.getPrincipal(),
                producer.getTransactionId().get(),
                PatternType.LITERAL,
                AclOperation.DESCRIBE));

    assertThat(aclBindings)
        .contains(
            buildTransactionIdLevelAcl(
                producer.getPrincipal(),
                producer.getTransactionId().get(),
                PatternType.LITERAL,
                AclOperation.WRITE));

    assertThat(aclBindings)
        .contains(buildClusterLevelAcl(producer.getPrincipal(), AclOperation.IDEMPOTENT_WRITE));
  }

  @Test
  public void testIdempotenceProducerAclsBuilder() {
    Producer producer = new Producer("User:foo");
    producer.setIdempotence(Optional.of(true));
    List<TopologyAclBinding> aclBindings =
        builder.buildBindingsForProducers(Collections.singleton(producer), "bar", false);
    assertThat(aclBindings.size()).isEqualTo(3);

    assertThat(aclBindings)
        .contains(buildTopicLevelAcl("User:foo", "bar", PatternType.LITERAL, AclOperation.WRITE));
    assertThat(aclBindings)
        .contains(
            buildTopicLevelAcl("User:foo", "bar", PatternType.LITERAL, AclOperation.DESCRIBE));

    assertThat(aclBindings)
        .contains(buildClusterLevelAcl(producer.getPrincipal(), AclOperation.IDEMPOTENT_WRITE));
  }

  @Test
  public void testConnectorAclsBuilder() {
    Connector connector = new Connector("User:foo");

    List<TopologyAclBinding> bindings = builder.buildBindingsForConnect(connector, "prefix");
    assertThat(bindings.size()).isEqualTo(8);

    List<TopologyAclBinding> shouldContainBindings = new ArrayList<>();
    shouldContainBindings.add(
        buildTopicLevelAcl(
            connector.getPrincipal(),
            connector.statusTopicString(),
            PatternType.LITERAL,
            AclOperation.READ));
    shouldContainBindings.add(
        buildTopicLevelAcl(
            connector.getPrincipal(),
            connector.offsetTopicString(),
            PatternType.LITERAL,
            AclOperation.READ));
    shouldContainBindings.add(
        buildTopicLevelAcl(
            connector.getPrincipal(),
            connector.configsTopicString(),
            PatternType.LITERAL,
            AclOperation.READ));
    shouldContainBindings.add(
        buildTopicLevelAcl(
            connector.getPrincipal(),
            connector.statusTopicString(),
            PatternType.LITERAL,
            AclOperation.WRITE));
    shouldContainBindings.add(
        buildTopicLevelAcl(
            connector.getPrincipal(),
            connector.offsetTopicString(),
            PatternType.LITERAL,
            AclOperation.WRITE));
    shouldContainBindings.add(
        buildTopicLevelAcl(
            connector.getPrincipal(),
            connector.configsTopicString(),
            PatternType.LITERAL,
            AclOperation.WRITE));
    shouldContainBindings.add(
        buildGroupLevelAcl(
            connector.getPrincipal(),
            connector.groupString(),
            PatternType.LITERAL,
            AclOperation.READ));
    shouldContainBindings.add(buildClusterLevelAcl(connector.getPrincipal(), AclOperation.CREATE));

    assertThat(shouldContainBindings.size()).isEqualTo(8);
    assertThat(bindings).containsAll(shouldContainBindings);
  }

  @Test
  public void testStreamsAclsBuilder() {
    KStream stream = new KStream("User:foo", new HashMap<>());

    List<String> readTopics = Collections.singletonList("foo");
    List<String> writeTopics = Collections.singletonList("bar");

    List<TopologyAclBinding> bindings =
        builder.buildBindingsForStreamsApp(
            stream.getPrincipal(), "prefix", readTopics, writeTopics);

    assertThat(bindings.size()).isEqualTo(4);
    assertThat(bindings)
        .contains(
            buildTopicLevelAcl(
                stream.getPrincipal(), "foo", PatternType.LITERAL, AclOperation.READ));
    assertThat(bindings)
        .contains(
            buildTopicLevelAcl(
                stream.getPrincipal(), "bar", PatternType.LITERAL, AclOperation.WRITE));
    assertThat(bindings)
        .contains(
            buildGroupLevelAcl(
                stream.getPrincipal(), "prefix", PatternType.PREFIXED, AclOperation.READ));
    assertThat(bindings)
        .contains(
            buildTopicLevelAcl(
                stream.getPrincipal(), "prefix", PatternType.PREFIXED, AclOperation.ALL));
  }

  private TopologyAclBinding buildTopicLevelAcl(
      String principal, String topic, PatternType patternType, AclOperation op) {
    return new TopologyAclBinding(
        new AclBuilder(principal)
            .addResource(ResourceType.TOPIC, topic, patternType)
            .addControlEntry("*", op, AclPermissionType.ALLOW)
            .build());
  }

  private TopologyAclBinding buildTransactionIdLevelAcl(
      String principal, String transactionId, PatternType patternType, AclOperation op) {
    return new TopologyAclBinding(
        new AclBuilder(principal)
            .addResource(ResourceType.TRANSACTIONAL_ID, transactionId, patternType)
            .addControlEntry("*", op, AclPermissionType.ALLOW)
            .build());
  }

  private TopologyAclBinding buildClusterLevelAcl(String principal, AclOperation op) {
    ResourcePattern resourcePattern =
        new ResourcePattern(ResourceType.CLUSTER, "kafka-cluster", PatternType.LITERAL);
    AccessControlEntry entry = new AccessControlEntry(principal, "*", op, AclPermissionType.ALLOW);
    return new TopologyAclBinding(new AclBinding(resourcePattern, entry));
  }

  private TopologyAclBinding buildGroupLevelAcl(
      String principal, String group, PatternType patternType, AclOperation op) {
    return new TopologyAclBinding(
        new AclBuilder(principal)
            .addResource(ResourceType.GROUP, group, patternType)
            .addControlEntry("*", op, AclPermissionType.ALLOW)
            .build());
  }
}
