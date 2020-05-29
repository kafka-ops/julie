package com.purbon.kafka.topology;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.purbon.kafka.topology.model.Platform;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.SchemaRegistry;
import com.purbon.kafka.topology.roles.SimpleAclsProvider;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private AccessControlManager accessControlManager;

  @Before
  public void setup() {
    accessControlManager = new AccessControlManager(aclsProvider, clusterState);
    doNothing().when(clusterState).update(Matchers.anyList());
    doNothing().when(clusterState).flushAndClose();
  }

  @Test
  public void newConsumerACLsCreation() {

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));
    Project project = new Project();
    project.setConsumers(consumers);

    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    Topology topology = new Topology();
    topology.addProject(project);

    List<String> users = Arrays.asList(new String[] {"User:app1"});

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsProvider)
        .setAclsForConsumers(users, topicA.toString());
    accessControlManager.sync(topology);
    verify(aclsProvider, times(1)).setAclsForConsumers(eq(users), eq(topicA.toString()));
  }

  @Test
  public void newProducerACLsCreation() {

    List<Producer> producers = new ArrayList<>();
    producers.add(new Producer("User:app1"));
    Project project = new Project();
    project.setProducers(producers);

    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    Topology topology = new Topology();
    topology.addProject(project);

    List<String> users = Arrays.asList(new String[] {"User:app1"});

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsProvider)
        .setAclsForProducers(users, topicA.toString());
    accessControlManager.sync(topology);
    verify(aclsProvider, times(1)).setAclsForProducers(eq(users), eq(topicA.toString()));
  }

  @Test
  public void newKafkaStreamsAppACLsCreation() {

    Project project = new Project();

    KStream app = new KStream();
    app.setPrincipal("User:App0");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    topics.put(KStream.WRITE_TOPICS, Arrays.asList("topicC", "topicD"));
    app.setTopics(topics);
    project.setStreams(Collections.singletonList(app));

    Topology topology = new Topology();
    topology.addProject(project);

    accessControlManager.sync(topology);
    String topicPrefix = project.buildTopicPrefix(topology);

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
  public void newSchemaRegistryACLCreation() {

    Project project = new Project();
    Topology topology = new Topology();
    topology.addProject(project);

    Platform platform = new Platform();
    SchemaRegistry sr = new SchemaRegistry();
    sr.setPrincipal("User:foo");
    platform.addSchemaRegistry(sr);
    topology.setPlatform(platform);

    accessControlManager.sync(topology);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsProvider)
        .setAclsForSchemaRegistry("User:foo");

    verify(aclsProvider, times(1)).setAclsForSchemaRegistry("User:foo");
  }

  @Test
  public void newKafkaConnectACLsCreation() {

    Project project = new Project();

    Connector connector1 = new Connector();
    connector1.setPrincipal("User:Connect1");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(Connector.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    connector1.setTopics(topics);

    project.setConnectors(Arrays.asList(connector1));

    Topology topology = new Topology();
    topology.addProject(project);

    accessControlManager.sync(topology);
    String topicPrefix = project.buildTopicPrefix(topology);

    doReturn(new ArrayList<TopologyAclBinding>())
        .when(aclsProvider)
        .setAclsForConnect(
            "User:Connect1",
            topicPrefix,
            topics.get(KStream.READ_TOPICS),
            topics.get(KStream.WRITE_TOPICS));
    verify(aclsProvider, times(1))
        .setAclsForConnect(
            eq("User:Connect1"),
            eq(topicPrefix),
            eq(topics.get(KStream.READ_TOPICS)),
            eq(topics.get(KStream.WRITE_TOPICS)));
  }
}
