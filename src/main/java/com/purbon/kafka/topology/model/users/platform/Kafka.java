package com.purbon.kafka.topology.model.users.platform;

import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.users.Quota;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Kafka {

  private Optional<List<User>> instances;
  private Optional<Map<String, List<User>>> rbac;
  private Optional<List<Quota>> quotas;

  public Kafka() {
    instances = Optional.empty();
    rbac = Optional.empty();
    quotas = Optional.empty();
  }

  public Optional<List<User>> getInstances() {
    return instances;
  }

  public void setInstances(Optional<List<User>> instances) {
    this.instances = instances;
  }

  public Optional<Map<String, List<User>>> getRbac() {
    return rbac;
  }

  public void setRbac(Optional<Map<String, List<User>>> rbac) {
    this.rbac = rbac;
  }

  public Optional<List<Quota>> getQuotas() {
    return quotas;
  }

  public void setQuotas(Optional<List<Quota>> quotas) {
    this.quotas = quotas;
  }
}
