package com.purbon.kafka.topology.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Topology;
import java.io.File;
import java.io.IOException;

public class TopologySerdes {

  private ObjectMapper mapper;

  public enum FileType {
    JSON,
    YAML,
    XML
  }

  public TopologySerdes() {
    this(new TopologyBuilderConfig(), FileType.YAML);
  }

  public TopologySerdes(TopologyBuilderConfig config) {
    this(config, config.getTopologyFileType());
  }

  public TopologySerdes(TopologyBuilderConfig config, FileType type) {
    mapper = ObjectMapperFactory.build(type, config);
  }

  public Topology deserialise(File file) throws IOException {
    return mapper.readValue(file, Topology.class);
  }

  public Topology deserialise(String content) throws IOException {
    return mapper.readValue(content, Topology.class);
  }

  public String serialise(Topology topology) throws JsonProcessingException {
    return mapper.writeValueAsString(topology);
  }

  private static class ObjectMapperFactory {

    public static ObjectMapper build(FileType type, TopologyBuilderConfig config) {
      ObjectMapper mapper;
      if (type.equals(FileType.JSON)) {
        mapper = buildAJsonObjectMapper();
      } else if (type.equals(FileType.XML)) {
        mapper = buildAnXMLObjectMapper();
      } else {
        mapper = buildAYamlObjectMapper();
      }

      SimpleModule module = new SimpleModule();
      module.addDeserializer(Topology.class, new TopologyCustomDeserializer(config));
      module.addDeserializer(TopicImpl.class, new TopicCustomDeserializer(config));
      mapper.registerModule(module);
      mapper.registerModule(new Jdk8Module());
      mapper.findAndRegisterModules();
      return mapper;
    }

    private static ObjectMapper buildAnXMLObjectMapper() {
      return new XmlMapper();
    }

    private static ObjectMapper buildAYamlObjectMapper() {
      return new ObjectMapper(new YAMLFactory());
    }

    private static ObjectMapper buildAJsonObjectMapper() {
      return new ObjectMapper();
    }
  }
}
