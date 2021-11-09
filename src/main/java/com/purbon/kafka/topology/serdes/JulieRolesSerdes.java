package com.purbon.kafka.topology.serdes;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.purbon.kafka.topology.model.JulieRoles;
import com.purbon.kafka.topology.model.PlanMap;
import java.io.File;
import java.io.IOException;

public class JulieRolesSerdes {

  ObjectMapper mapper;

  public JulieRolesSerdes() {
    mapper = new ObjectMapper(new YAMLFactory());
    mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    mapper.registerModule(new SimpleModule());
    mapper.registerModule(new Jdk8Module());
    mapper.findAndRegisterModules();
  }

  public JulieRoles deserialise(File file) throws IOException {
    return mapper.readValue(file, JulieRoles.class);
  }

  public String serialise(PlanMap planMap) throws JsonProcessingException {
    return mapper.writeValueAsString(planMap);
  }
}
