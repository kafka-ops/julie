package com.purbon.kafka.topology.model.users.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.purbon.kafka.topology.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Confluent Schema Registry clusters definition.
 *
 * List of Schema Registry clusters to configure their access to a secure Kafka cluster.
 *
 * YAML:
 * <pre>
 * {@code
 *   schema_registry:
 *     instances:
 *       - principal: "User:SchemaRegistry01"
 *         topic: "foo"
 *         group: "bar"
 *       - principal: "User:SchemaRegistry02"
 *         topic: "zet"
 * }
 * </pre>
 */
public class SchemaRegistry {

  /**
   * Schema Registry clusters definition.
   */
  private List<SchemaRegistryInstance> instances;

  /**
   *
   */
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
