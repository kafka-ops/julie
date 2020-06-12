package com.purbon.kafka.topology.serdes;

import static com.purbon.kafka.topology.serdes.JsonSerdesUtils.addProject2Topology;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.purbon.kafka.topology.model.Platform;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.users.ControlCenter;
import com.purbon.kafka.topology.model.users.SchemaRegistry;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TopologyCustomDeserializer extends StdDeserializer<Topology> {

  public static final String PROJECTS_KEY = "projects";
  public static final String TEAM_KEY = "team";
  public static final String SOURCE_KEY = "source";

  public static final String PLATFORM_KEY = "platform";
  public static final String SCHEMA_REGISTRY_KEY = "schema_registry";
  public static final String CONTROL_CENTER_KEY = "control_center";

  protected TopologyCustomDeserializer() {
    this(null);
  }

  protected TopologyCustomDeserializer(Class<?> clazz) {
    super(clazz);
  }

  @Override
  public Topology deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {

    JsonNode rootNode = parser.getCodec().readTree(parser);

    JsonNode projects = rootNode.get(PROJECTS_KEY);
    Topology topology = new Topology();

    addProject2Topology(parser, topology, projects);

    List<String> excludeAttributes =
        Arrays.asList(PROJECTS_KEY, TEAM_KEY, SOURCE_KEY, PLATFORM_KEY);

    Iterator<String> fieldNames = rootNode.fieldNames();
    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      if (!excludeAttributes.contains(fieldName)) {
        topology.addOther(fieldName, rootNode.get(fieldName).asText());
      }
    }
    topology.setTeam(rootNode.get(TEAM_KEY).asText());
    topology.setSource(rootNode.get(SOURCE_KEY).asText());

    JsonNode platformNode = rootNode.get(PLATFORM_KEY);
    Platform platform = new Platform();
    if (platformNode != null && platformNode.size() > 0) {
      JsonNode schemaRegistryNode = platformNode.get(SCHEMA_REGISTRY_KEY);
      if (schemaRegistryNode != null) {
        for (int i = 0; i < schemaRegistryNode.size(); i++) {
          JsonNode node = schemaRegistryNode.get(i);
          SchemaRegistry schemaRegistry = parser.getCodec().treeToValue(node, SchemaRegistry.class);
          platform.addSchemaRegistry(schemaRegistry);
        }
      }
      JsonNode controlCenterNode = platformNode.get(CONTROL_CENTER_KEY);
      if (controlCenterNode != null) {
        for (int i = 0; i < controlCenterNode.size(); i++) {
          JsonNode node = controlCenterNode.get(i);
          ControlCenter controlCenter = parser.getCodec().treeToValue(node, ControlCenter.class);
          platform.addControlCenter(controlCenter);
        }
      }
    }
    topology.setPlatform(platform);

    return topology;
  }
}
