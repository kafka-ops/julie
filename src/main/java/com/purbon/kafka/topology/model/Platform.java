package com.purbon.kafka.topology.model;

import com.purbon.kafka.topology.model.users.ControlCenter;
import com.purbon.kafka.topology.model.users.SchemaRegistry;
import java.util.ArrayList;
import java.util.List;

public class Platform {

  private List<SchemaRegistry> schemaRegistry;
  private List<ControlCenter> controlCenter;

  public Platform() {
    this.schemaRegistry = new ArrayList<>();
    this.controlCenter = new ArrayList<>();
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

  public List<ControlCenter> getControlCenter() {
    return controlCenter;
  }

  public void setControlCenter(List<ControlCenter> controlCenter) {
    this.controlCenter = controlCenter;
  }

  public void addControlCenter(ControlCenter controlCenter) {
    this.controlCenter.add(controlCenter);
  }
}
