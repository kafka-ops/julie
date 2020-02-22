package com.purbon.kafka.topology.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

public class JSON {

  private static final ObjectMapper mapper = new ObjectMapper();

  public static Map<String, Object> toMap(String jsonString) throws JsonProcessingException {
      return mapper.readValue(jsonString, Map.class);
  }

  public static String asString(Map map) throws JsonProcessingException {
    return mapper.writeValueAsString(map);
  }

  public static List<String> toArray(String jsonString) throws JsonProcessingException {
    return mapper.readValue(jsonString, List.class);
  }
}
