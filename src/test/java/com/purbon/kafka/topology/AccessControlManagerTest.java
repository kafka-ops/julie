package com.purbon.kafka.topology;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.purbon.kafka.topology.actions.Action;
import com.purbon.kafka.topology.adminclient.AclBuilder;
import com.purbon.kafka.topology.model.Component;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Platform;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.platform.ControlCenter;
import com.purbon.kafka.topology.model.users.platform.ControlCenterInstance;
import com.purbon.kafka.topology.model.users.platform.Kafka;
import com.purbon.kafka.topology.model.users.platform.KafkaConnect;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistry;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import com.purbon.kafka.topology.roles.SimpleAclsProvider;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class AccessControlManagerTest {

  @Mock SimpleAclsProvider aclsProvider;

  @Mock ClusterState clusterState;

  @Mock PrintStream mockPrintStream;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private AccessControlManager accessControlManager;

  @Before
  public void setup() {
    accessControlManager = new AccessControlManager(aclsProvider, clusterState);
    doNothing().when(clusterState).add(Matchers.anyList());
    doNothing().when(clusterState).flushAndClose();
  }

  @Test
  public void newConsumerACLsCreation() throws IOException {

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));
    Project project = new ProjectImpl();
    project.setConsumers(consumers);

    Topic topicA = new TopicImpl("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    List<Consumer> users = asList(new Consumer("User:app1"));

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsProvider)
        .setAclsForConsumers(users, topicA.toString());
    accessControlManager.sync(topology);
    verify(aclsProvider, times(1)).setAclsForConsumers(eq(users), eq(topicA.toString()));
  }

  @Test
  public void newProducerACLsCreation() throws IOException {

    List<Producer> producers = new ArrayList<>();
    producers.add(new Producer("User:app1"));
    Project project = new ProjectImpl();
    project.setProducers(producers);

    Topic topicA = new TopicImpl("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    List<String> users = asList(new String[] {"User:app1"});

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsProvider)
        .setAclsForProducers(users, topicA.toString());
    accessControlManager.sync(topology);
    verify(aclsProvider, times(1)).setAclsForProducers(eq(users), eq(topicA.toString()));
  }

  @Test
  public void newKafkaStreamsAppACLsCreation() throws IOException {

    Project project = new ProjectImpl();

    KStream app = new KStream();
    app.setPrincipal("User:App0");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, asList("topicA", "topicB"));
    topics.put(KStream.WRITE_TOPICS, asList("topicC", "topicD"));
    app.setTopics(topics);
    project.setStreams(Collections.singletonList(app));

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    accessControlManager.sync(topology);
    String topicPrefix = project.buildTopicPrefix(topology.buildNamePrefix());

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsProvider)
        .setAclsForStreamsApp(
            "User:App0",
            topicPrefix,
            topics.get(KStream.READ_TOPICS),
            topics.get(KStream.WRITE_TOPICS));
    verify(aclsProvider, times(1))
        .setAclsForStreamsApp(
            eq("User:App0"),
            eq(topicPrefix),
            eq(topics.get(KStream.READ_TOPICS)),
            eq(topics.get(KStream.WRITE_TOPICS)));
  }

  @Test
  public void newSchemaRegistryACLCreation() throws IOException {

    Project project = new ProjectImpl();
    Topology topology = new TopologyImpl();
    topology.addProject(project);

    Platform platform = new Platform();
    SchemaRegistry sr = new SchemaRegistry();

    SchemaRegistryInstance instance = new SchemaRegistryInstance();
    instance.setPrincipal("User:foo");
    sr.setInstances(Collections.singletonList(instance));

    Map<String, List<User>> rbac = new HashMap<>();
    rbac.put("SecurityAdmin", Collections.singletonList(new User("User:foo")));
    rbac.put("ClusterAdmin", Collections.singletonList(new User("User:bar")));
    sr.setRbac(Optional.of(rbac));

    platform.setSchemaRegistry(sr);
    topology.setPlatform(platform);

    accessControlManager.sync(topology);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsProvider)
        .setAclsForSchemaRegistry(instance);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsProvider)
        .setClusterLevelRole(anyString(), anyString(), eq(Component.SCHEMA_REGISTRY));

    verify(aclsProvider, times(1)).setAclsForSchemaRegistry(instance);
    verify(aclsProvider, times(1))
        .setClusterLevelRole("SecurityAdmin", "User:foo", Component.SCHEMA_REGISTRY);
    verify(aclsProvider, times(1))
        .setClusterLevelRole("ClusterAdmin", "User:bar", Component.SCHEMA_REGISTRY);
  }

  @Test
  public void newControlCenterACLCreation() throws IOException {

    Project project = new ProjectImpl();
    Topology topology = new TopologyImpl();
    topology.addProject(project);

    Platform platform = new Platform();
    ControlCenter c3 = new ControlCenter();
    ControlCenterInstance instance = new ControlCenterInstance();
    instance.setPrincipal("User:foo");
    instance.setAppId("appid");
    c3.setInstances(Collections.singletonList(instance));
    platform.setControlCenter(c3);
    topology.setPlatform(platform);

    accessControlManager.sync(topology);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsProvider)
        .setAclsForControlCenter("User:foo", "appid");

    verify(aclsProvider, times(1)).setAclsForControlCenter("User:foo", "appid");
  }

  @Test
  public void newKafkaClusterRBACCreation() throws IOException {
    Project project = new ProjectImpl();
    Topology topology = new TopologyImpl();
    topology.addProject(project);

    Platform platform = new Platform();
    Kafka kafka = new Kafka();
    Map<String, List<User>> rbac = new HashMap<>();
    rbac.put("Operator", Collections.singletonList(new User("User:foo")));
    rbac.put("ClusterAdmin", Collections.singletonList(new User("User:bar")));
    kafka.setRbac(Optional.of(rbac));
    platform.setKafka(kafka);
    topology.setPlatform(platform);

    accessControlManager.sync(topology);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsProvider)
        .setClusterLevelRole(anyString(), anyString(), eq(Component.KAFKA));

    verify(aclsProvider, times(1)).setClusterLevelRole("Operator", "User:foo", Component.KAFKA);
    verify(aclsProvider, times(1)).setClusterLevelRole("ClusterAdmin", "User:bar", Component.KAFKA);
  }

  @Test
  public void newKafkaConnectClusterRBACCreation() throws IOException {
    Project project = new ProjectImpl();
    Topology topology = new TopologyImpl();
    topology.addProject(project);

    Platform platform = new Platform();
    KafkaConnect connect = new KafkaConnect();
    Map<String, List<User>> rbac = new HashMap<>();
    rbac.put("Operator", Collections.singletonList(new User("User:foo")));
    rbac.put("ClusterAdmin", Collections.singletonList(new User("User:bar")));
    connect.setRbac(Optional.of(rbac));
    platform.setKafkaConnect(connect);
    topology.setPlatform(platform);

    accessControlManager.sync(topology);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsProvider)
        .setClusterLevelRole(anyString(), anyString(), eq(Component.KAFKA_CONNECT));

    verify(aclsProvider, times(1))
        .setClusterLevelRole("Operator", "User:foo", Component.KAFKA_CONNECT);
    verify(aclsProvider, times(1))
        .setClusterLevelRole("ClusterAdmin", "User:bar", Component.KAFKA_CONNECT);
  }

  @Test
  public void newKafkaConnectACLsCreation() throws IOException {

    Project project = new ProjectImpl();

    Connector connector1 = new Connector();
    connector1.setPrincipal("User:Connect1");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(Connector.READ_TOPICS, asList("topicA", "topicB"));
    connector1.setTopics(topics);

    project.setConnectors(asList(connector1));

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    accessControlManager.sync(topology);
    String topicPrefix = project.buildTopicPrefix(topology.buildNamePrefix());

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsProvider)
        .setAclsForConnect(connector1, topicPrefix);

    verify(aclsProvider, times(1)).setAclsForConnect(eq(connector1), eq(topicPrefix));
  }

  @Test
  public void testDryRunMode() throws IOException {

    accessControlManager.setDryRun(true);
    accessControlManager.setOutputStream(mockPrintStream);

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));
    Project project = new ProjectImpl();
    project.setConsumers(consumers);

    Topic topicA = new TopicImpl("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    List<Consumer> users = asList(new Consumer("User:app1"));

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsProvider)
        .setAclsForConsumers(users, topicA.toString());
    accessControlManager.sync(topology);

    verify(mockPrintStream, times(3)).println(any(Action.class));
  }

  @Test
  public void testAclDeleteLogic() throws IOException {

    accessControlManager = new AccessControlManager(aclsProvider);

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));
    consumers.add(new Consumer("User:app2"));

    Topic topicA = new TopicImpl("topicA");

    Topology topology = buildTopology(consumers, asList(topicA));

    List<TopologyAclBinding> bindings = returnAclsForConsumers(consumers, topicA.getName());
    doReturn(bindings).when(aclsProvider).setAclsForConsumers(any(), eq(topicA.toString()));

    accessControlManager.sync(topology);

    verify(aclsProvider, times(1)).setAclsForConsumers(asList(consumers.get(0)), topicA.toString());
    verify(aclsProvider, times(1)).setAclsForConsumers(asList(consumers.get(1)), topicA.toString());

    consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));

    bindings = returnAclsForConsumers(consumers, topicA.getName());
    doReturn(bindings).when(aclsProvider).setAclsForConsumers(eq(consumers), eq(topicA.toString()));

    Topology newTopology = buildTopology(consumers, asList(topicA));

    accessControlManager.sync(newTopology);

    List<TopologyAclBinding> bindingsToDelete =
        returnAclsForConsumers(asList(new Consumer("User:app2")), topicA.getName());

    verify(aclsProvider, times(1)).clearAcls(new HashSet<>(bindingsToDelete));
  }

  private Topology buildTopology(List<Consumer> consumers, List<Topic> topics) {

    Project project = new ProjectImpl();
    project.setConsumers(consumers);

    for (Topic topic : topics) {
      project.addTopic(topic);
    }

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    return topology;
  }

  private List<TopologyAclBinding> returnAclsForConsumers(List<Consumer> consumers, String topic) {

    List<AclBinding> acls = new ArrayList<>();

    for (Consumer consumer : consumers) {
      acls.add(
          buildTopicLevelAcl(
              consumer.getPrincipal(), topic, PatternType.LITERAL, AclOperation.DESCRIBE));
      acls.add(
          buildTopicLevelAcl(
              consumer.getPrincipal(), topic, PatternType.LITERAL, AclOperation.READ));
      acls.add(
          buildGroupLevelAcl(
              consumer.getPrincipal(),
              consumer.groupString(),
              consumer.groupString().equals("*") ? PatternType.PREFIXED : PatternType.LITERAL,
              AclOperation.READ));
    }

    return acls.stream()
        .map(aclBinding -> new TopologyAclBinding(aclBinding))
        .collect(Collectors.toList());
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
