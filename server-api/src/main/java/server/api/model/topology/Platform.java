package server.api.model.topology;

import java.util.ArrayList;
import java.util.List;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonProperty;
import server.api.model.topology.users.ControlCenter;
import server.api.model.topology.users.SchemaRegistry;

@BsonDiscriminator
public class Platform {

  @BsonProperty(value = "schema-registry")
  private List<SchemaRegistry> schemaRegistry;
  @BsonProperty(value = "control-center")
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
