package com.purbon.kafka.topology.serdes;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.purbon.kafka.topology.model.PlanMap;
import java.io.File;
import java.io.IOException;

public class PlanMapSerdes {

  ObjectMapper mapper;

  public PlanMapSerdes() {
    mapper = new ObjectMapper(new YAMLFactory());
    mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    mapper.registerModule(new SimpleModule());
    mapper.registerModule(new Jdk8Module());
    mapper.findAndRegisterModules();
  }

  public PlanMap deserialise(File file) throws IOException {
    return mapper.readValue(file, PlanMap.class);
  }

  public PlanMap deserialise(String content) throws IOException {
    return mapper.readValue(content, PlanMap.class);
  }

  public String serialise(PlanMap planMap) throws JsonProcessingException {
    return mapper.writeValueAsString(planMap);
  }
}
