package com.purbon.kafka.topology.model.users.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.purbon.kafka.topology.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SchemaRegistry {

  private List<SchemaRegistryInstance> instances;

  @JsonInclude(Include.NON_EMPTY)
  private Optional<Map<String, List<User>>> rbac;

  public SchemaRegistry() {
    instances = new ArrayList<>();
    rbac = Optional.empty();
  }

  public List<SchemaRegistryInstance> getInstances() {
    return instances;
  }

  public void setInstances(List<SchemaRegistryInstance> instances) {
    this.instances = instances;
  }

  public Optional<Map<String, List<User>>> getRbac() {
    return rbac;
  }

  public void setRbac(Optional<Map<String, List<User>>> rbac) {
    this.rbac = rbac;
  }

  public boolean isEmpty() {
    return getInstances().isEmpty();
  }
}
