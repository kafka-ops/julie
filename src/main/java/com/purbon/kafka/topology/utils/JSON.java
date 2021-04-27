package com.purbon.kafka.topology.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JSON {

  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.findAndRegisterModules();
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

  public static String asString(Object account) throws JsonProcessingException {
    return mapper.writeValueAsString(account);
  }

  public static Set<ServiceAccount> toSASet(String json) throws JsonProcessingException {
    return mapper.readValue(json, new TypeReference<Set<ServiceAccount>>() {});
  }

  public static Set<TopologyAclBinding> toBindingsSet(String json) throws JsonProcessingException {
    return mapper.readValue(json, new TypeReference<Set<TopologyAclBinding>>() {});
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

  public static Set<String> toStringsSet(String json) throws JsonProcessingException {
    return mapper.readValue(json, new TypeReference<Set<String>>() {});
  }
}
