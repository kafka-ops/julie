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

  TopicSchemas getSchemas();

  void setSchemas(TopicSchemas schemas);

  HashMap<String, String> getConfig();

  void initializeConfig();

  Optional<String> getDataType();

  void addAppConfig(TopologyBuilderConfig appConfig);

  int partitionsCount();

  void setDefaultProjectPrefix(String projectPrefix);

  void setPrefixContext(Map<String, Object> prefixContext);
}
