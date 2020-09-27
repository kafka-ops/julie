package com.purbon.kafka.topology.serdes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

  public static List<Project> parseProjects(JsonParser parser, JsonNode projects)
      throws JsonProcessingException {
    return new JsonSerdesUtils<Project>().parseApplicationUser(parser, projects, Project.class);
  }

  public static void addTopics2Project(
      JsonParser parser, Project project, JsonNode topics, TopologyBuilderConfig config)
      throws JsonProcessingException {
    new JsonSerdesUtils<Topic>()
        .parseApplicationUser(parser, topics, Topic.class)
        .forEach(
            new Consumer<Topic>() {
              @Override
              public void accept(Topic topic) {
                topic.addAppConfig(config);
                project.addTopic(topic);
              }
            });
  }
}
