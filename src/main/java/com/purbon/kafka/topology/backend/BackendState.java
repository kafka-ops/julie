package com.purbon.kafka.topology.backend;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.utils.JSON;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class BackendState {

  private final Set<TopologyAclBinding> bindings;
  private final Set<ServiceAccount> accounts;
  private final Set<String> topics;

  public BackendState() {
    this.accounts = new HashSet<>();
    this.bindings = new HashSet<>();
    this.topics = new HashSet<>();
  }

  public void addAccounts(Collection<ServiceAccount> accounts) {
    this.accounts.addAll(accounts);
  }

  public void addBindings(Collection<TopologyAclBinding> bindings) {
    this.bindings.addAll(bindings);
  }

  public void addTopics(Collection<String> topics) {
    this.topics.addAll(topics);
  }

  public Set<TopologyAclBinding> getBindings() {
    return bindings;
  }

  public Set<ServiceAccount> getAccounts() {
    return accounts;
  }

  public Set<String> getTopics() {
    return topics;
  }

  @JsonIgnore
  public String asJson() throws JsonProcessingException {
    return JSON.asString(this);
  }

  public void clear() {
    bindings.clear();
    accounts.clear();
    topics.clear();
  }

  public int size() {
    return bindings.size() + accounts.size() + topics.size();
  }
}
