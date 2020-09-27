package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@JsonDeserialize(as = TopicImpl.class)
public interface Topic {

  String getName();

  void setName(String name);

  TopicSchemas getSchemas();

  void setSchemas(TopicSchemas schemas);

  HashMap<String, String> getConfig();

  void setConfig(HashMap<String, String> config);

  Map<String, String> rawConfig();

  Optional<String> getDataType();

  void setProjectPrefix(String projectPrefix);

  void setPrefixProperties(Map<String, Object> properties);

  void addAppConfig(TopologyBuilderConfig appConfig);

  String getProjectPrefix();

  int partitionsCount();
}
