package com.purbon.kafka.topology;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.purbon.kafka.topology.model.Topology;
import java.io.File;
import java.io.IOException;

public class TopologySerdes {

  ObjectMapper mapper;

  public TopologySerdes() {
    mapper = new ObjectMapper(new YAMLFactory());
    mapper.findAndRegisterModules();
  }

  public Topology deserialise(File file) throws IOException {
    Topology topology = mapper.readValue(file, Topology.class);
    return topology;
  }

  public Topology deserialise(String content) throws IOException {
    Topology topology = mapper.readValue(content, Topology.class);
    return topology;
  }

  public String serialise(Topology topology) throws JsonProcessingException {
    return mapper.writeValueAsString(topology);
  }

}
