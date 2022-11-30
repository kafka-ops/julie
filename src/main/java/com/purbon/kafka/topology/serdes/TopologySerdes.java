package com.purbon.kafka.topology.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.exceptions.TopologyParsingException;
import com.purbon.kafka.topology.model.PlanMap;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TopologySerdes {

  private final ObjectMapper mapper;
  private final SystemPropertySubstitutor systemPropertySubstitutor;

  public enum FileType {
    JSON,
    YAML
  }

  public TopologySerdes() {
    this(new Configuration(), FileType.YAML, new PlanMap());
  }

  public TopologySerdes(Configuration config, PlanMap plans) {
    this(config, config.getTopologyFileType(), plans);
  }

  public TopologySerdes(Configuration config, FileType type, PlanMap plans) {
    mapper = ObjectMapperFactory.build(type, config, plans);
    systemPropertySubstitutor = new SystemPropertySubstitutor();
  }

  public Topology deserialise(File file) {
    try (FileInputStream inputStream = new FileInputStream(file)) {
      String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      return deserialise(content);
    } catch (IOException e) {
      throw new TopologyParsingException(
          "Failed to deserialize topology from " + file.getPath(), e);
    }
  }

  public Topology deserialise(String content) {
    try {
      String substitutedContent = systemPropertySubstitutor.replace(content);
      return mapper.readValue(substitutedContent, Topology.class);
    } catch (IOException e) {
      throw new TopologyParsingException("Failed to deserialize topology from " + content, e);
    }
  }

  public String serialise(Topology topology) throws JsonProcessingException {
    return mapper.writeValueAsString(topology);
  }

  private static class ObjectMapperFactory {

    public static ObjectMapper build(FileType type, Configuration config, PlanMap plans) {
      ObjectMapper mapper;
      if (type.equals(FileType.JSON)) {
        mapper = new ObjectMapper();
      } else {
        mapper = new ObjectMapper(new YAMLFactory());
      }

      SimpleModule module = new SimpleModule();
      module.addDeserializer(Topology.class, new TopologyCustomDeserializer(config));
      module.addDeserializer(Topic.class, new TopicCustomDeserializer(config, plans));
      mapper.registerModule(module);
      mapper.registerModule(new Jdk8Module());
      mapper.findAndRegisterModules();
      return mapper;
    }
  }
}
