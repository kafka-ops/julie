package com.purbon.kafka.topology.integration;

import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.DEVELOPER_READ;
import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.DEVELOPER_WRITE;
import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.RESOURCE_OWNER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.purbon.kafka.topology.AccessControlManager;
import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.model.Platform;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.SchemaRegistry;
import com.purbon.kafka.topology.roles.RBACProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class RBACPRoviderRbacIT extends MDSBaseTest {

  private String mdsServer = "http://localhost:8090";
  private String mdsUser = "professor";
  private String mdsPassword = "professor";

  private MDSApiClient apiClient;
  private AccessControlManager accessControlManager;

  @Before
  public void before() throws IOException, InterruptedException {
    super.beforeEach();
    apiClient = new MDSApiClient(mdsServer);
    apiClient.login(mdsUser, mdsPassword);
    apiClient.authenticate();
    apiClient.setKafkaClusterId(getKafkaClusterID());
    apiClient.setSchemaRegistryClusterID(getSchemaRegistryClusterID());

    RBACProvider rbacProvider = new RBACProvider(apiClient);
    accessControlManager = new AccessControlManager(rbacProvider);
  }

  @Test
  public void consumerAclsCreation() {

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));

    Project project = new Project("project");
    project.setConsumers(consumers);
    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("testConsumerAclsCreation");
    topology.addProject(project);

    accessControlManager.sync(topology);

    verifyConsumerAcls(consumers, topicA.toString());
  }

  @Test
  public void producerAclsCreation() {

    List<Producer> producers = new ArrayList<>();
    producers.add(new Producer("User:app2"));

    Project project = new Project("project");
    project.setProducers(producers);
    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("producerAclsCreation");
    topology.addProject(project);

    accessControlManager.sync(topology);

    verifyProducerAcls(producers, topicA.toString());
  }

  @Test
  public void kstreamsAclsCreation() {
    Project project = new Project();

    KStream app = new KStream();
    app.setPrincipal("User:App3");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    topics.put(KStream.WRITE_TOPICS, Arrays.asList("topicC", "topicD"));
    app.setTopics(topics);
    project.setStreams(Collections.singletonList(app));

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("kstreamsAclsCreation");
    topology.addProject(project);

    accessControlManager.sync(topology);

    verifyKStreamsAcls(app);
  }

  @Test
  public void connectAclsCreation() {
    Project project = new Project();

    Connector connector = new Connector();
    connector.setPrincipal("User:Connect");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    connector.setTopics(topics);
    project.setConnectors(Collections.singletonList(connector));

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("connectAclsCreation");
    topology.addProject(project);

    accessControlManager.sync(topology);

    verifyConnectAcls(connector);
  }

  @Test
  public void schemaRegistryAclsCreation() {
    Project project = new Project();

    Topology topology = new Topology();
    topology.setTeam("integration-test");
    topology.setSource("schemaRegistryAclsCreation");
    topology.addProject(project);

    Platform platform = new Platform();
    SchemaRegistry sr = new SchemaRegistry();
    sr.setPrincipal("User:foo");
    platform.addSchemaRegistry(sr);

    SchemaRegistry sr2 = new SchemaRegistry();
    sr2.setPrincipal("User:banana");
    platform.addSchemaRegistry(sr2);

    topology.setPlatform(platform);

    accessControlManager.sync(topology);

    verifySchemaRegistryAcls(platform);
  }

  private void verifySchemaRegistryAcls(Platform platform) {
    SchemaRegistry sr = platform.getSchemaRegistry().get(0);
    List<String> roles = apiClient.lookupRoles(sr.getPrincipal());
    assertTrue(roles.contains(RESOURCE_OWNER));
    // assertTrue(roles.contains(SECURITY_ADMIN));
  }

  private void verifyConnectAcls(Connector app) {
    List<String> roles = apiClient.lookupRoles(app.getPrincipal());
    assertTrue(roles.contains(DEVELOPER_READ));
    // assertTrue(roles.contains(RESOURCE_OWNER));
  }

  private void verifyKStreamsAcls(KStream app) {
    List<String> roles = apiClient.lookupRoles(app.getPrincipal());
    assertTrue(roles.contains(DEVELOPER_READ));
    assertTrue(roles.contains(DEVELOPER_WRITE));
    // assertTrue(roles.contains(RESOURCE_OWNER));
  }

  private void verifyProducerAcls(List<Producer> producers, String topic) {
    producers.forEach(
        producer -> {
          List<String> roles = apiClient.lookupRoles(producer.getPrincipal());
          assertEquals(1, roles.size());
          assertTrue(roles.contains(DEVELOPER_WRITE));
        });
  }

  private void verifyConsumerAcls(List<Consumer> consumers, String topic) {
    consumers.forEach(
        consumer -> {
          List<String> roles = apiClient.lookupRoles(consumer.getPrincipal());
          assertEquals(1, roles.size());
          assertTrue(roles.contains(DEVELOPER_READ));
        });
  }
}
