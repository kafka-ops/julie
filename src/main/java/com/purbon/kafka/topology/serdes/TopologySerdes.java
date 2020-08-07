package com.purbon.kafka.topology.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class TopologySerdes {

  ObjectMapper mapper;

  public TopologySerdes() {
    mapper = new ObjectMapper(new YAMLFactory());
    SimpleModule module = new SimpleModule();
    module.addDeserializer(Topology.class, new TopologyCustomDeserializer());
    module.addDeserializer(ProjectImpl.class, new ProjectCustomDeserializer());
    mapper.registerModule(module);
    mapper.registerModule(new Jdk8Module());
    mapper.findAndRegisterModules();
  }

  public Topology deserialise(File file) throws IOException {
    Topology topology = mapper.readValue(file, Topology.class);
    return updateTopology(topology);
  }

  public Topology deserialise(String content) throws IOException {
    Topology topology = mapper.readValue(content, Topology.class);
    return updateTopology(topology);
  }

  public String serialise(Topology topology) throws JsonProcessingException {
    return mapper.writeValueAsString(topology);
  }

  private Topology updateTopology(Topology topology) {
    topology
        .getProjects()
        .forEach(
            new Consumer<Project>() {
              @Override
              public void accept(Project project) {
                project.setTopologyPrefix(topology.buildNamePrefix());
                project
                    .getTopics()
                    .forEach(
                        new Consumer<Topic>() {
                          @Override
                          public void accept(Topic topic) {
                            topic.setProjectPrefix(project.buildTopicPrefix());
                          }
                        });
              }
            });
    return topology;
  }
}
