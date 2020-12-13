package com.purbon.kafka.topology.integration.backend;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;

import com.purbon.kafka.topology.BackendController;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Before;
import org.junit.Test;

public class DefaultBackendIT {

  BackendController backend;

  @Before
  public void before() throws IOException {
    Files.deleteIfExists(Paths.get(".cluster-state"));
    backend = new BackendController();
  }

  @Test
  public void saveAndRestoreBindingsAndAccountsTest() throws IOException {

    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(), "Topic", "host", "op", "principal", "LITERAL");

    ServiceAccount serviceAccount = new ServiceAccount(1, "name", "description");
    ServiceAccount serviceAccount2 = new ServiceAccount(2, "name2", "description2");

    Set<ServiceAccount> accounts = new HashSet<>(Arrays.asList(serviceAccount, serviceAccount2));

    backend.add(Collections.singletonList(binding));
    backend.addServiceAccounts(accounts);
    backend.flushAndClose();

    // reopen a new connection
    backend = new BackendController();
    backend.load();

    assertThat(backend.getBindings()).isNotNull();
    assertThat(backend.getBindings()).isEqualTo(Collections.singleton(binding));
    assertThat(backend.getServiceAccounts()).isNotNull();
    assertThat(backend.getServiceAccounts()).isEqualTo(accounts);
  }

  @Test
  public void saveAndRestoreBindingsWithNoAccounts() throws IOException {

    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(), "Topic", "host", "op", "principal", "LITERAL");

    backend.add(Collections.singletonList(binding));
    backend.flushAndClose();

    // reopen a new connection
    backend = new BackendController();
    backend.load();

    assertThat(backend.getBindings()).isNotNull();
    assertThat(backend.getBindings()).isEqualTo(Collections.singleton(binding));
    assertThat(backend.getServiceAccounts()).isNotNull();
    assertEquals(0, backend.getServiceAccounts().size());
  }
}
