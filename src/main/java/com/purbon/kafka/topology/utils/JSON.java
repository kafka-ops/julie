package com.purbon.kafka.topology.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.util.List;
import java.util.Map;

public class JSON {

  private static final ObjectMapper mapper;
  private static final ObjectWriter prettyWriter;

  static {
    mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.findAndRegisterModules();
    DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
    prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
    prettyWriter = mapper.writer(prettyPrinter);
  }

  public static Map<String, Object> toMap(String jsonString) throws JsonProcessingException {
    return mapper.readValue(jsonString, Map.class);
  }

  public static String asString(Map map) throws JsonProcessingException {
    return mapper.writeValueAsString(map);
  }

  public static String asPrettyString(Map map) throws JsonProcessingException {
    return prettyWriter.writeValueAsString(map);
  }

  public static List<String> toArray(String jsonString) throws JsonProcessingException {
    return mapper.readValue(jsonString, List.class);
  }

  public static String asString(Object object) throws JsonProcessingException {
    return mapper.writeValueAsString(object);
  }

  public static String asPrettyString(Object object) throws JsonProcessingException {
    return prettyWriter.writeValueAsString(object);
  }

  public static Object toObjectList(String jsonString, Class objectClazz)
      throws JsonProcessingException {
    CollectionType collectionType =
        mapper.getTypeFactory().constructCollectionType(List.class, objectClazz);
    return mapper.readValue(jsonString, collectionType);
  }

  public static Object toObject(String jsonString, Class objectClazz)
      throws JsonProcessingException {
    return mapper.readValue(jsonString, objectClazz);
  }

  public static JsonNode toNode(String jsonString) throws JsonProcessingException {
    return mapper.readTree(jsonString);
  }
}
