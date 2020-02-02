package com.purbon.kafka.topology;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topology;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;

public class TopologyCustomDeserializer extends StdDeserializer<Topology> {

  protected TopologyCustomDeserializer() {
    this(null);
  }

  protected TopologyCustomDeserializer(Class<?> clazz) {
    super(clazz);
  }

  @Override
  public Topology deserialize(JsonParser parser, DeserializationContext context) throws IOException {

    JsonNode rootNode = parser.getCodec().readTree(parser);

    JsonNode projects = rootNode.get("projects");
    Topology topology = new Topology();

    for(int i=0; i < projects.size(); i++) {
      JsonNode node = projects.get(i);
      Project project = parser.getCodec().treeToValue(node, Project.class);
      topology.addProject(project);
    }

    ArrayList<String> excludeAttributes = new ArrayList<>();
    excludeAttributes.add("projects");
    excludeAttributes.add("team");
    excludeAttributes.add("source");

    Iterator<String> fieldNames = rootNode.fieldNames();
    while(fieldNames.hasNext()) {
        String fieldName = fieldNames.next();
        if (!excludeAttributes.contains(fieldName)) {
          topology.addOther(fieldName, rootNode.get(fieldName).asText());
        }
    }
    topology.setTeam(rootNode.get("team").asText());
    topology.setSource(rootNode.get("source").asText());
    return topology;
  }
}
