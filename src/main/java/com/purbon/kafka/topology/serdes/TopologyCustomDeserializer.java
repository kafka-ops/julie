package com.purbon.kafka.topology.serdes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topology;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class TopologyCustomDeserializer extends StdDeserializer<Topology> {

  public static final String PROJECTS_KEY = "projects";
  public static final String TEAM_KEY = "team";
  public static final String SOURCE_KEY = "source";

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

    for (int i = 0; i < projects.size(); i++) {
      JsonNode node = projects.get(i);
      Project project = parser.getCodec().treeToValue(node, Project.class);
      topology.addProject(project);
    }

    ArrayList<String> excludeAttributes = new ArrayList<>();
    excludeAttributes.add(PROJECTS_KEY);
    excludeAttributes.add(TEAM_KEY);
    excludeAttributes.add(SOURCE_KEY);

    Iterator<String> fieldNames = rootNode.fieldNames();
    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      if (!excludeAttributes.contains(fieldName)) {
        topology.addOther(fieldName, rootNode.get(fieldName).asText());
      }
    }
    topology.setTeam(rootNode.get(TEAM_KEY).asText());
    topology.setSource(rootNode.get(SOURCE_KEY).asText());
    return topology;
  }
}
