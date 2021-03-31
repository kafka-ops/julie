package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.Producer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@JsonDeserialize(as = TopicImpl.class)
public interface Topic {

  String getName();

  List<TopicSchemas> getSchemas();

  String getPlan();

  void setSchemas(List<TopicSchemas> schemas);

  Map<String, String> getConfig();

  HashMap<String, String> getRawConfig();

  Optional<String> getDataType();

  void addAppConfig(Configuration appConfig);

  Optional<Integer> partitionsCountOptional();

  Integer partitionsCount();

  void setDefaultProjectPrefix(String projectPrefix);

  void setPrefixContext(Map<String, Object> prefixContext);

  Optional<Short> replicationFactor();

  List<Consumer> getConsumers();

  List<Producer> getProducers();

  void setConsumers(List<Consumer> consumers);

  void setProducers(List<Producer> producers);

  Map<String, String> getMetadata();

  void setMetadata(Map<String, String> metadata);

  SubjectNameStrategy getSubjectNameStrategy();
}
