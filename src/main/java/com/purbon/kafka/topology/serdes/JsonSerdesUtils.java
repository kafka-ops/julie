package com.purbon.kafka.topology.serdes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.util.ArrayList;
import java.util.List;

public class JsonSerdesUtils<T> {

  public List<T> parseApplicationUser(JsonParser parser, JsonNode userNode, Class<T> clazz)
      throws JsonProcessingException {
    List<T> usersList = new ArrayList<>();
    for (int i = 0; i < userNode.size(); i++) {
      JsonNode node = userNode.get(i);
      T user = parser.getCodec().treeToValue(node, clazz);
      usersList.add(user);
    }
    return usersList;
  }

  public static void addProject2Topology(JsonParser parser, Topology topology, JsonNode projects)
      throws JsonProcessingException {
    new JsonSerdesUtils<Project>()
        .parseApplicationUser(parser, projects, Project.class)
        .forEach(project -> topology.addProject(project));
  }

  public static void addTopics2Project(JsonParser parser, Project project, JsonNode topics)
      throws JsonProcessingException {
    new JsonSerdesUtils<Topic>()
        .parseApplicationUser(parser, topics, Topic.class)
        .forEach(topic -> project.addTopic(topic));
  }
}
