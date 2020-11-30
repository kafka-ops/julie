package com.purbon.kafka.topology.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.exceptions.TopologyParsingException;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.PlanMap;
import com.purbon.kafka.topology.model.Topology;
import java.io.File;
import java.io.IOException;

public class TopologySerdes {

  private ObjectMapper mapper;

  public enum FileType {
    JSON,
    YAML
  }

  public TopologySerdes() {
    this(new TopologyBuilderConfig(), FileType.YAML, new PlanMap());
  }

  public TopologySerdes(TopologyBuilderConfig config) {
    this(config, config.getTopologyFileType(), new PlanMap());
  }

  public TopologySerdes(TopologyBuilderConfig config, PlanMap plans) {
    this(config, config.getTopologyFileType(), plans);
  }

  public TopologySerdes(TopologyBuilderConfig config, FileType type) {
    mapper = ObjectMapperFactory.build(type, config, new PlanMap());
  }

  public TopologySerdes(TopologyBuilderConfig config, FileType type, PlanMap plans) {
    mapper = ObjectMapperFactory.build(type, config, plans);
  }

  public Topology deserialise(File file) {
    try {
      return mapper.readValue(file, Topology.class);
    } catch (IOException e) {
      throw new TopologyParsingException(
          "Failed to deserialize topology from " + file.getPath(), e);
    }
  }

  public Topology deserialise(String content) {
    try {
      return mapper.readValue(content, Topology.class);
    } catch (IOException e) {
      throw new TopologyParsingException("Failed to deserialize topology from " + content, e);
    }
  }

  public String serialise(Topology topology) throws JsonProcessingException {
    return mapper.writeValueAsString(topology);
  }

  private static class ObjectMapperFactory {

    public static ObjectMapper build(FileType type, TopologyBuilderConfig config, PlanMap plans) {
      ObjectMapper mapper;
      if (type.equals(FileType.JSON)) {
        mapper = new ObjectMapper();
      } else {
        mapper = new ObjectMapper(new YAMLFactory());
      }

      SimpleModule module = new SimpleModule();
      module.addDeserializer(Topology.class, new TopologyCustomDeserializer(config));
      module.addDeserializer(TopicImpl.class, new TopicCustomDeserializer(config, plans));
      mapper.registerModule(module);
      mapper.registerModule(new Jdk8Module());
      mapper.findAndRegisterModules();
      return mapper;
    }
  }
}
