package com.purbon.kafka.topology.integration;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.purbon.kafka.topology.*;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.integration.containerutils.*;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.Quota;
import com.purbon.kafka.topology.quotas.QuotasManager;
import com.purbon.kafka.topology.roles.SimpleAclsProvider;
import com.purbon.kafka.topology.roles.acls.AclsBindingsBuilder;
import com.purbon.kafka.topology.utils.TestUtils;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.quota.ClientQuotaEntity;
import org.apache.kafka.common.quota.ClientQuotaFilter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class QuotasManagerIT {

  private static SaslPlaintextKafkaContainer container;
  private static AdminClient kafkaAdminClient;

  private TopologyBuilderAdminClient topologyAdminClient;
  private TopicManager topicManager;
  private AccessControlManager accessControlManager;
  private SimpleAclsProvider aclsProvider;

  private ExecutionPlan plan;
  private BackendController cs;

  private QuotasManager quotasManager;
  private AclsBindingsBuilder bindingsBuilder;

  @BeforeAll
  public static void setup() {
    container =
        ContainerFactory.fetchSaslKafkaContainer(System.getProperty("cp.version"))
            .withUser("user1")
            .withUser("user2")
            .withUser("user3");
    container.start();
  }

  @AfterAll
  public static void teardown() {
    container.stop();
  }

  @BeforeEach
  void before() throws IOException, ExecutionException, InterruptedException {
    kafkaAdminClient = ContainerTestUtils.getSaslAdminClient(container);
    topologyAdminClient = new TopologyBuilderAdminClient(kafkaAdminClient);
    topologyAdminClient.clearAcls();
    TestUtils.deleteStateFile();

    Properties props = new Properties();
    props.put(TOPOLOGY_TOPIC_STATE_FROM_CLUSTER, "false");
    props.put(ALLOW_DELETE_TOPICS, true);
    props.put(ALLOW_DELETE_BINDINGS, true);

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(CCLOUD_ENV_CONFIG, "");

    Configuration config = new Configuration(cliOps, props);

    this.cs = new BackendController();
    this.plan = ExecutionPlan.init(cs, System.out);

    this.topicManager = new TopicManager(topologyAdminClient, null, config);
    bindingsBuilder = new AclsBindingsBuilder(config);
    quotasManager = new QuotasManager(kafkaAdminClient, config);
    aclsProvider = new SimpleAclsProvider(topologyAdminClient);

    accessControlManager = new AccessControlManager(aclsProvider, bindingsBuilder, config);
  }

  private Topology woldMSpecPattern() throws ExecutionException, InterruptedException, IOException {
    List<Producer> producers = new ArrayList<>();
    Producer producer = new Producer("User:user1");
    producers.add(producer);
    Producer producer2 = new Producer("User:user2");
    producers.add(producer2);

    List<Consumer> consumers = new ArrayList<>();
    Consumer consumer = new Consumer("User:user1");
    consumers.add(consumer);
    Consumer consumer2 = new Consumer("User:user2");
    consumers.add(consumer2);

    Project project = new ProjectImpl("project");
    project.setProducers(producers);
    project.setConsumers(consumers);

    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.setContext("integration-test");
    topology.addOther("source", "producerAclsCreation");
    topology.addProject(project);

    return topology;
  }

  @Test
  void quotaForUserCreation() throws ExecutionException, InterruptedException, IOException {

    Topology topology = woldMSpecPattern();
    accessControlManager.updatePlan(topology, plan);
    plan.run();

    List<Quota> quotas = new ArrayList<>();
    Quota quota = new Quota("user1", Optional.empty(), Optional.of(20.0));
    quotas.add(quota);

    quotasManager.assignQuotasPrincipal(quotas);

    // Verify Quotas
    verifyQuotasOnlyUser(quotas);
  }

  @Test
  void quotaForUserRemove() throws ExecutionException, InterruptedException, IOException {

    Topology topology = woldMSpecPattern();
    accessControlManager.updatePlan(topology, plan);
    plan.run();

    List<Quota> quotas = new ArrayList<>();
    Quota quota = new Quota("user3", Optional.empty(), Optional.of(20.0));
    quotas.add(quota);

    quotasManager.assignQuotasPrincipal(quotas);
    // Verify Quotas
    assertTrue(verifyQuotasOnlyUser(quotas).stream().allMatch(f -> f.equals(true)));

    // Remove quota
    quotasManager.removeQuotasPrincipal(List.of(new User("user3")));
    assertTrue(verifyQuotasOnlyUser(quotas).stream().allMatch(f -> f.equals(false)));
  }

  @Test
  void quotaForUserChangeValues() throws ExecutionException, InterruptedException, IOException {

    Topology topology = woldMSpecPattern();
    accessControlManager.updatePlan(topology, plan);
    plan.run();

    List<Quota> quotas = new ArrayList<>();
    Quota quota = new Quota("user1", Optional.empty(), Optional.of(20.0));
    quotas.add(quota);
    quotasManager.assignQuotasPrincipal(quotas);
    // Verify Quotas
    assertTrue(verifyQuotasOnlyUser(quotas).stream().allMatch(f -> f.equals(true)));

    // change value
    Quota quotaUpdate = new Quota("user1", Optional.of(150.0), Optional.of(250.0));
    quotas.clear();
    quotas.add(quotaUpdate);
    quotasManager.assignQuotasPrincipal(quotas);
    assertTrue(verifyQuotasOnlyUser(quotas).stream().allMatch(f -> f.equals(true)));
  }

  @Test
  void quotaOnlyRemoveOneUser() throws ExecutionException, InterruptedException, IOException {

    Topology topology = woldMSpecPattern();
    accessControlManager.updatePlan(topology, plan);
    plan.run();

    List<Quota> quotas = new ArrayList<>();
    Quota quota = new Quota("user1", Optional.empty(), Optional.of(20.0));
    quotas.add(quota);
    Quota quota2 = new Quota("user2", Optional.of(300.0), Optional.of(100.0), Optional.of(50.0));
    quotas.add(quota2);
    quotasManager.assignQuotasPrincipal(quotas);
    // Verify Quotas
    assertTrue(verifyQuotasOnlyUser(quotas).stream().allMatch(f -> f.equals(true)));

    quotasManager.removeQuotasPrincipal(List.of(new User("user2")));
    assertTrue(verifyQuotasOnlyUser(List.of(quota)).stream().allMatch(f -> f.equals(true)));

    assertTrue(verifyQuotasOnlyUser(List.of(quota2)).stream().allMatch(f -> f.equals(false)));
  }

  private List<Boolean> verifyQuotasOnlyUser(List<Quota> quotas)
      throws ExecutionException, InterruptedException, IOException {
    Map<ClientQuotaEntity, Map<String, Double>> cqsresult =
        kafkaAdminClient.describeClientQuotas(ClientQuotaFilter.all()).entities().get();
    return quotas.stream()
        .map(
            q -> {
              ClientQuotaEntity cqe =
                  new ClientQuotaEntity(
                      Collections.singletonMap(ClientQuotaEntity.USER, q.getPrincipal()));
              if (cqsresult.containsKey(cqe)) {
                verifyQuotaAssigment(q, cqsresult.get(cqe));
                return true;
              } else {
                return false;
              }
            })
        .collect(Collectors.toList());
  }

  private void verifyQuotaAssigment(Quota q, Map<String, Double> map) {
    if (q.getProducer_byte_rate().isPresent()) {
      assertEquals(map.get("producer_byte_rate"), q.getProducer_byte_rate().get());
    }
    if (q.getConsumer_byte_rate().isPresent()) {
      assertEquals(map.get("consumer_byte_rate"), q.getConsumer_byte_rate().get());
    }
    if (q.getRequest_percentage().isPresent()) {
      assertEquals(map.get("request_percentage"), q.getRequest_percentage().get());
    }
  }
}
