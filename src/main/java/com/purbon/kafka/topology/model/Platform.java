package com.purbon.kafka.topology.model;

import com.purbon.kafka.topology.model.users.SchemaRegistry;
import java.util.ArrayList;
import java.util.List;

public class Platform {

  private List<SchemaRegistry> schemaRegistry;
  private Topology topology;

  public Platform() {
    this.schemaRegistry = new ArrayList<>();
  }

  public List<SchemaRegistry> getSchemaRegistry() {
    return schemaRegistry;
  }

  public void setSchemaRegistry(List<SchemaRegistry> schemaRegistryPrinciples) {
    this.schemaRegistry = schemaRegistryPrinciples;
  }

  public void addSchemaRegistry(SchemaRegistry schemaRegistry) {
    this.schemaRegistry.add(schemaRegistry);
  }

  public void setTopology(Topology topology) {
    this.topology = topology;
  }
}
