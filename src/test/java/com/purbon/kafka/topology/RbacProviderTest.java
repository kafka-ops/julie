package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.Constants.OPTIMIZED_ACLS_CONFIG;
import static com.purbon.kafka.topology.roles.rbac.RBACBindingsBuilder.LITERAL;
import static com.purbon.kafka.topology.roles.rbac.RBACBindingsBuilder.PREFIX;
import static com.purbon.kafka.topology.roles.rbac.RBACPredefinedRoles.DEVELOPER_READ;
import static com.purbon.kafka.topology.roles.rbac.RBACPredefinedRoles.DEVELOPER_WRITE;
import static org.mockito.Mockito.*;

import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.api.mds.RequestScope;
import com.purbon.kafka.topology.model.*;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.platform.ControlCenter;
import com.purbon.kafka.topology.model.users.platform.ControlCenterInstance;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistry;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import com.purbon.kafka.topology.roles.RBACProvider;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.roles.rbac.ClusterLevelRoleBuilder;
import com.purbon.kafka.topology.roles.rbac.RBACBindingsBuilder;
import java.io.IOException;
import java.util.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class RbacProviderTest {

  @Mock MDSApiClient apiClient;

  @Mock ExecutionPlan plan;

  @Mock ClusterLevelRoleBuilder runner;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private AccessControlManager accessControlManager;
  private RBACProvider aclsProvider;
  private RBACBindingsBuilder bindingsBuilder;

  @Before
  public void setup() {
    apiClient.setConnectClusterID("kc");
    apiClient.setSchemaRegistryClusterID("sr");
    apiClient.setKafkaClusterId("ak");

    aclsProvider = new RBACProvider(apiClient);
    bindingsBuilder = new RBACBindingsBuilder(apiClient);
    accessControlManager = new AccessControlManager(aclsProvider, bindingsBuilder);
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

    doReturn(new TopologyAclBinding())
        .when(apiClient)
        .bind("User:app1", DEVELOPER_READ, topicA.toString(), LITERAL);

    accessControlManager.updatePlan(plan, topology);

    verify(apiClient, times(1))
        .bind(eq("User:app1"), anyString(), eq(topicA.toString()), anyString());
  }

  @Test
  public void newConsumerOptimisedACLsCreation() throws IOException {

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    Properties props = new Properties();
    props.put(OPTIMIZED_ACLS_CONFIG, true);

    Configuration config = new Configuration(cliOps, props);
    accessControlManager = new AccessControlManager(aclsProvider, bindingsBuilder, config);

    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("User:app1"));
    Project project = new ProjectImpl();
    project.setConsumers(consumers);

    Topic topicA = new TopicImpl("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    doReturn(new TopologyAclBinding())
        .when(apiClient)
        .bind("User:app1", DEVELOPER_READ, project.namePrefix(), PREFIX);

    accessControlManager.updatePlan(plan, topology);

    verify(apiClient, times(1))
        .bind(eq("User:app1"), eq(DEVELOPER_READ), eq(project.namePrefix()), eq(PREFIX));
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

    doReturn(new TopologyAclBinding())
        .when(apiClient)
        .bind("User:app1", DEVELOPER_WRITE, topicA.toString(), LITERAL);

    accessControlManager.updatePlan(plan, topology);

    verify(apiClient, times(1))
        .bind(eq("User:app1"), anyString(), eq(topicA.toString()), anyString());
  }

  @Test
  public void newProducerOptimizedACLsCreation() throws IOException {

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    Properties props = new Properties();
    props.put(OPTIMIZED_ACLS_CONFIG, true);

    Configuration config = new Configuration(cliOps, props);
    accessControlManager = new AccessControlManager(aclsProvider, bindingsBuilder, config);
    List<Producer> producers = new ArrayList<>();
    producers.add(new Producer("User:app1"));
    Project project = new ProjectImpl();
    project.setProducers(producers);

    Topic topicA = new TopicImpl("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    doReturn(new TopologyAclBinding())
        .when(apiClient)
        .bind("User:app1", DEVELOPER_WRITE, project.namePrefix(), PREFIX);

    accessControlManager.updatePlan(plan, topology);

    verify(apiClient, times(1))
        .bind(eq("User:app1"), eq(DEVELOPER_WRITE), eq(project.namePrefix()), eq(PREFIX));
  }

  @Test
  public void newKafkaStreamsAppACLsCreation() throws IOException {

    Project project = new ProjectImpl();

    KStream app = new KStream();
    app.setPrincipal("User:App0");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    topics.put(KStream.WRITE_TOPICS, Arrays.asList("topicC", "topicD"));
    app.setTopics(topics);
    project.setStreams(Collections.singletonList(app));

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    doReturn(new TopologyAclBinding())
        .when(apiClient)
        .bind(anyString(), anyString(), anyString(), anyString());

    accessControlManager.updatePlan(plan, topology);

    verify(apiClient, times(6)).bind(eq("User:App0"), anyString(), anyString(), anyString());
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
    rbac.put("ClusterAdmin", Collections.singletonList(new User("User:foo")));
    sr.setRbac(Optional.of(rbac));

    platform.setSchemaRegistry(sr);
    topology.setPlatform(platform);

    doReturn(runner).when(apiClient).bind(eq("User:foo"), anyString());

    doReturn(runner).when(runner).forSchemaRegistry();
    doReturn(new TopologyAclBinding())
        .when(apiClient)
        .bindClusterRole(anyString(), anyString(), any(RequestScope.class));

    accessControlManager.updatePlan(plan, topology);

    verify(apiClient, times(1))
        .bind(anyString(), anyString(), anyString(), anyString(), anyString());
    verify(apiClient, times(3)).bind(anyString(), anyString());
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

    doReturn(runner).when(apiClient).bind(eq("User:foo"), anyString());

    doReturn(runner).when(runner).forControlCenter();
    doReturn(new TopologyAclBinding())
        .when(apiClient)
        .bindClusterRole(anyString(), anyString(), any(RequestScope.class));

    accessControlManager.updatePlan(plan, topology);

    verify(apiClient, times(1)).bind(anyString(), anyString());
  }

  @Test
  public void newKafkaConnectACLsCreation() throws IOException {

    Project project = new ProjectImpl();

    Connector connector1 = new Connector();
    connector1.setPrincipal("User:Connect1");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(Connector.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    connector1.setTopics(topics);

    project.setConnectors(Collections.singletonList(connector1));

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    doReturn(runner).when(apiClient).bind(eq("User:Connect1"), anyString());

    doReturn(runner).when(runner).forKafkaConnect(any());

    doReturn(new TopologyAclBinding())
        .when(apiClient)
        .bindClusterRole(anyString(), anyString(), any(RequestScope.class));

    accessControlManager.updatePlan(plan, topology);

    verify(apiClient, times(1)).bind(anyString(), anyString());
    verify(apiClient, times(6))
        .bind(anyString(), anyString(), anyString(), anyString(), anyString());
  }
}
