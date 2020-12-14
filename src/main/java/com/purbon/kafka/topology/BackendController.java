package com.purbon.kafka.topology;

import com.purbon.kafka.topology.backend.Backend;
import com.purbon.kafka.topology.backend.FileBackend;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BackendController {

  public enum Mode {
    TRUNCATE,
    APPEND
  }

  private static final Logger LOGGER = LogManager.getLogger(BackendController.class);

  private static final String STORE_TYPE = "acls";

  private final Backend backend;
  private Set<TopologyAclBinding> bindings;
  private Set<ServiceAccount> serviceAccounts;

  public BackendController() {
    this(new FileBackend());
  }

  public BackendController(Backend backend) {
    this.backend = backend;
    this.bindings = new HashSet<>();
    this.serviceAccounts = new HashSet<>();
  }

  public void add(List<TopologyAclBinding> bindings) {
    LOGGER.debug(String.format("Adding bindings %s to the backend", bindings));
    this.bindings.addAll(bindings);
  }

  public void addServiceAccounts(Set<ServiceAccount> serviceAccounts) {
    LOGGER.debug(String.format("Adding Service Accounts %s to the backend", serviceAccounts));
    this.serviceAccounts.addAll(serviceAccounts);
  }

  public void add(TopologyAclBinding binding) {
    LOGGER.debug(String.format("Adding binding %s to the backend", binding));
    this.bindings.add(binding);
  }

  public Set<ServiceAccount> getServiceAccounts() {
    return new HashSet<>(serviceAccounts);
  }

  public Set<TopologyAclBinding> getBindings() {
    return bindings;
  }

  public void flushAndClose() {
    LOGGER.debug(String.format("Flushing the current state of %s, %s", STORE_TYPE, bindings));
    backend.createOrOpen(Mode.TRUNCATE);
    backend.saveType(STORE_TYPE);
    backend.saveBindings(bindings);
    backend.saveType("ServiceAccounts");
    backend.saveAccounts(serviceAccounts);
    backend.close();
  }

  public void load() throws IOException {
    LOGGER.debug(String.format("Loading data from the backend at %s", backend.getClass()));
    backend.createOrOpen();
    bindings.addAll(backend.loadBindings());
    serviceAccounts.addAll(backend.loadServiceAccounts());
  }

  public void reset() {
    LOGGER.debug("Reset the bindings cache");
    bindings.clear();
    serviceAccounts.clear();
  }

  public int size() {
    return bindings.size() + serviceAccounts.size();
  }
}
