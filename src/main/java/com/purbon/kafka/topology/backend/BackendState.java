package com.purbon.kafka.topology.backend;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.model.artefact.KafkaConnectArtefact;
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
  private final Set<KafkaConnectArtefact> connectors;

  public BackendState() {
    this.accounts = new HashSet<>();
    this.bindings = new HashSet<>();
    this.topics = new HashSet<>();
    this.connectors = new HashSet<>();
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

  public void addConnectors(Collection<KafkaConnectArtefact> connectors) {
    this.connectors.addAll(connectors);
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

  public Set<KafkaConnectArtefact> getConnectors() {
    return connectors;
  }

  @JsonIgnore
  public String asJson() throws JsonProcessingException {
    return JSON.asString(this);
  }

  public void clear() {
    bindings.clear();
    accounts.clear();
    topics.clear();
    connectors.clear();
  }

  public int size() {
    return bindings.size() + accounts.size() + topics.size() + connectors.size();
  }
}
