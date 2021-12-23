package com.purbon.kafka.topology.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.util.List;
import java.util.Map;

public class JSON {

  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.findAndRegisterModules();
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
  }

  public static Map<String, Object> toMap(String jsonString) throws JsonProcessingException {
    return mapper.readValue(jsonString, Map.class);
  }

  public static String asString(Map map) throws JsonProcessingException {
    return mapper.writeValueAsString(map);
  }

  public static String asPrettyString(Map map) throws JsonProcessingException {
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
  }

  public static List<String> toArray(String jsonString) throws JsonProcessingException {
    return mapper.readValue(jsonString, List.class);
  }

  public static String asString(Object object) throws JsonProcessingException {
    return mapper.writeValueAsString(object);
  }

  public static String asPrettyString(Object object) throws JsonProcessingException {
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
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
}
