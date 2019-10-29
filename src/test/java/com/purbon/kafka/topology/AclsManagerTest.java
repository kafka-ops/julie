package com.purbon.kafka.topology;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class AclsManagerTest {


  @Mock
  TopologyBuilderAdminClient adminClient;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();


  private AclsManager aclsManager;

  @Before
  public void setup() {
    aclsManager = new AclsManager(adminClient);
  }

  @Test
  public void newConsumerACLsCreation() {

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("app1"));
    Project project = new Project();
    project.setConsumers(consumers);

    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    Topology topology = new Topology();
    topology.addProject(project);

    doNothing()
        .when(adminClient)
        .setAclsForConsumer("User:app1", topicA.composeTopicName(topology, project.getName()));
    aclsManager.syncAcls(topology);
    verify(adminClient, times(1))
        .setAclsForConsumer(eq("User:app1"), eq(topicA.composeTopicName(topology, project.getName())));
  }

  @Test
  public void newProducerACLsCreation() {

    List<Producer> producers = new ArrayList<>();
    producers.add(new Producer("app1"));
    Project project = new Project();
    project.setProducers(producers);

    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    Topology topology = new Topology();
    topology.addProject(project);

    doNothing().when(adminClient)
        .setAclsForProducer("User:app1", topicA.composeTopicName(topology, project.getName()));
    aclsManager.syncAcls(topology);
    verify(adminClient, times(1))
        .setAclsForProducer(eq("User:app1"), eq(topicA.composeTopicName(topology, project.getName())));
  }

  @Test
  public void newKafkaStreamsAppACLsCreation() {

    Project project = new Project();

    KStream app = new KStream();
    app.setPrincipal("App0");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    topics.put(KStream.WRITE_TOPICS, Arrays.asList("topicC", "topicD"));
    app.setTopics(topics);
    project.setStreams(Collections.singletonList(app));

    Topology topology = new Topology();
    topology.addProject(project);

    aclsManager.syncAcls(topology);
    String topicPrefix = project.buildTopicPrefix(topology);

    doNothing()
        .when(adminClient)
        .setAclsForStreamsApp("User:App0", topicPrefix, topics.get(KStream.READ_TOPICS), topics.get(KStream.WRITE_TOPICS));
    verify(adminClient, times(1))
        .setAclsForStreamsApp(eq("User:App0"), eq(topicPrefix), eq(topics.get(KStream.READ_TOPICS)), eq(topics.get(KStream.WRITE_TOPICS)) );
  }
}

