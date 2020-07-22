package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@JsonDeserialize(as = TopicImpl.class)
public interface Topic {

  String getName();

  void setName(String name);

  HashMap<String, String> getConfig();

  void setConfig(HashMap<String, String> config);

  Map<String, String> rawConfig();

  Optional<String> getDataType();

  void setProjectPrefix(String projectPrefix);

  String getProjectPrefix();
}
