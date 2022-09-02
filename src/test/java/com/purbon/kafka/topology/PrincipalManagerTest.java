package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.CommandLineInterface.*;
import static com.purbon.kafka.topology.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.purbon.kafka.topology.actions.Action;
import com.purbon.kafka.topology.actions.accounts.ClearAccounts;
import com.purbon.kafka.topology.actions.accounts.CreateAccounts;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.Producer;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrincipalManagerTest {

  private Map<String, String> cliOps;
  private Properties props;

  @Mock
  PrincipalProvider provider;

  ExecutionPlan plan;

  PrincipalUpdateManager principalUpdateManager;
  PrincipalDeleteManager principalDeleteManager;
  Configuration config;

  BackendController backendController;

  @Mock
  PrintStream mockPrintStream;

  @Mock
  ExecutionPlan mockPlan;

  @BeforeEach
  void before() throws IOException {

    Files.deleteIfExists(Paths.get(".cluster-state"));
    backendController = new BackendController();

    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    props = new Properties();

    props.put(JULIE_ENABLE_PRINCIPAL_MANAGEMENT, "true");
    props.put(TOPOLOGY_STATE_FROM_CLUSTER, "false");
    props.put(ALLOW_DELETE_PRINCIPALS, true);

    plan = ExecutionPlan.init(backendController, mockPrintStream);
    config = new Configuration(cliOps, props);
    principalUpdateManager = new PrincipalUpdateManager(provider, config);
    principalDeleteManager = new PrincipalDeleteManager(provider, config);
  }

  @Test
  void freshGeneration() throws IOException {

    Topology topology = new TopologyImpl();
    topology.setContext("context");
    Project project = new ProjectImpl("foo");
    project.setConsumers(
        Arrays.asList(new Consumer("consumer-principal"), new Consumer("consumer-principal")));
    project.setProducers(Collections.singletonList(new Producer("producer-principal")));
    topology.addProject(project);

    doNothing().when(provider).configure();

    principalUpdateManager.updatePlan(topology, plan);
    principalDeleteManager.updatePlan(topology, plan);

    Set<ServiceAccount> accounts =
        new HashSet<>(
            Arrays.asList(
                new ServiceAccount("-1", "consumer-principal", MANAGED_BY),
                new ServiceAccount("-1", "producer-principal", MANAGED_BY)));

    assertThat(plan.getActions()).hasSize(1);
    assertThat(plan.getActions()).containsAnyOf(new CreateAccounts(provider, accounts));
    backendController.flushAndClose();
  }

  @Test
  void freshTopicLevelGeneration() throws IOException {

    Topology topology = new TopologyImpl();
    topology.setContext("context");
    Project project = new ProjectImpl("foo");
    Topic topic = new Topic("baa");
    topic.setConsumers(Collections.singletonList(new Consumer("topicConsumer-principal")));
    topic.setProducers(Collections.singletonList(new Producer("topicProducer-principal")));

    project.addTopic(topic);
    topology.addProject(project);

    doNothing().when(provider).configure();

    principalUpdateManager.updatePlan(topology, plan);
    principalDeleteManager.updatePlan(topology, plan);

    Set<ServiceAccount> accounts =
        new HashSet<>(
            Arrays.asList(
                new ServiceAccount("-1", "topicConsumer-principal", MANAGED_BY),
                new ServiceAccount("-1", "topicProducer-principal", MANAGED_BY)));

    assertThat(plan.getActions()).hasSize(1);
    assertThat(plan.getActions()).containsAnyOf(new CreateAccounts(provider, accounts));
    backendController.flushAndClose();
  }

  @Test
  void deleteAccountsRequired() throws IOException {

    Topology topology = new TopologyImpl();
    topology.setContext("context");
    Project project = new ProjectImpl("foo");
    project.setConsumers(Collections.singletonList(new Consumer("consumer")));
    project.setProducers(Collections.singletonList(new Producer("producer")));
    topology.addProject(project);

    doNothing().when(provider).configure();

    doReturn(new ServiceAccount("123", "consumer", MANAGED_BY))
        .when(provider)
        .createServiceAccount(eq("consumer"), eq(MANAGED_BY));

    doReturn(new ServiceAccount("124", "producer", MANAGED_BY))
        .when(provider)
        .createServiceAccount(eq("producer"), eq(MANAGED_BY));

    principalUpdateManager.updatePlan(topology, plan);
    principalDeleteManager.updatePlan(topology, plan);
    plan.run();
    assertThat(plan.getServiceAccounts()).hasSize(2);

    topology = new TopologyImpl();
    topology.setContext("context");
    project = new ProjectImpl("foo");
    project.setConsumers(Collections.singletonList(new Consumer("consumer")));
    topology.addProject(project);

    backendController = new BackendController();
    plan = ExecutionPlan.init(backendController, mockPrintStream);
    principalUpdateManager.updatePlan(topology, plan);
    principalDeleteManager.updatePlan(topology, plan);

    Collection<ServiceAccount> accounts =
        Arrays.asList(new ServiceAccount("124", "producer", MANAGED_BY));

    assertThat(plan.getActions()).hasSize(1);
    assertThat(plan.getActions()).containsAnyOf(new ClearAccounts(provider, accounts));

    plan.run();

    assertThat(plan.getServiceAccounts()).hasSize(1);
    assertThat(plan.getServiceAccounts())
        .contains(new ServiceAccount("123", "consumer", MANAGED_BY));
  }

  @Test
  void notRunIfConfigNotExperimental() throws IOException {
    props.put(JULIE_ENABLE_PRINCIPAL_MANAGEMENT, "false");

    config = new Configuration(cliOps, props);
    principalUpdateManager = new PrincipalUpdateManager(provider, config);
    principalDeleteManager = new PrincipalDeleteManager(provider, config);

    Topology topology = new TopologyImpl();

    principalUpdateManager.updatePlan(topology, plan);
    principalDeleteManager.updatePlan(topology, plan);

    verify(mockPlan, times(0)).add(any(Action.class));
    backendController.flushAndClose();
  }

  @Test
  void toProcessOnlySelectedPrincipals() throws IOException {

    props.put(SERVICE_ACCOUNT_MANAGED_PREFIXES, Collections.singletonList("pro"));

    config = new Configuration(cliOps, props);
    principalUpdateManager = new PrincipalUpdateManager(provider, config);
    principalDeleteManager = new PrincipalDeleteManager(provider, config);

    Topology topology = new TopologyImpl();
    topology.setContext("context");
    Project project = new ProjectImpl("foo");
    project.setConsumers(Collections.singletonList(new Consumer("consumer")));
    project.setProducers(Collections.singletonList(new Producer("producer")));
    topology.addProject(project);

    doNothing().when(provider).configure();

    doReturn(new ServiceAccount("124", "producer", MANAGED_BY))
        .when(provider)
        .createServiceAccount(eq("producer"), eq(MANAGED_BY));

    principalUpdateManager.updatePlan(topology, plan);
    principalDeleteManager.updatePlan(topology, plan);
    plan.run();

    assertThat(plan.getServiceAccounts()).hasSize(1);
  }
}
