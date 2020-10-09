package com.purbon.kafka.topology.serdes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
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

  public static void validateRequiresKeys(JsonNode rootNode, String... keys) throws IOException {
    for (String key : keys) {
      if (rootNode.get(key) == null) {
        throw new IOException(key + " is a required field in the topology, please specify.");
      }
    }
  }
}
