package com.purbon.kafka.topology;

import com.purbon.kafka.topology.backend.Backend;
import com.purbon.kafka.topology.backend.BackendState;
import com.purbon.kafka.topology.backend.FileBackend;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
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
  private BackendState state;

  public BackendController() {
    this(new FileBackend());
  }

  public BackendController(Backend backend) {
    this.backend = backend;
    this.state = new BackendState();
  }

  public void addBindings(List<TopologyAclBinding> bindings) {
    LOGGER.debug(String.format("Adding bindings %s to the backend", bindings));
    state.addBindings(bindings);
  }

  public void addTopics(Set<String> topics) {
    LOGGER.debug(String.format("Adding topics %s to the backend", topics));
    state.addTopics(topics);
  }

  public void addServiceAccounts(Set<ServiceAccount> serviceAccounts) {
    LOGGER.debug(String.format("Adding Service Accounts %s to the backend", serviceAccounts));
    state.addAccounts(serviceAccounts);
  }

  public Set<ServiceAccount> getServiceAccounts() {
    return state.getAccounts();
  }

  public Set<TopologyAclBinding> getBindings() {
    return state.getBindings();
  }

  public Set<String> getTopics() {
    return state.getTopics();
  }

  public void flushAndClose() throws IOException {
    LOGGER.debug(String.format("Flushing the current state of %s", STORE_TYPE));
    backend.createOrOpen(Mode.TRUNCATE);
    backend.saveType(STORE_TYPE);
    backend.save(state);
    backend.close();
  }

  public void load() throws IOException {
    LOGGER.debug(String.format("Loading data from the backend at %s", backend.getClass()));
    backend.createOrOpen();
    state = backend.load();
  }

  public void reset() {
    LOGGER.debug("Reset the bindings cache");
    state.clear();
  }

  public int size() {
    return state.size();
  }
}
