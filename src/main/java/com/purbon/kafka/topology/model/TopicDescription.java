package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.Objects;

public class TopicDescription {
  String name;
  Map<String, String> configs;

  public String getName() {
    return name;
  }

  public Map<String, String> getConfigs() {
    return configs;
  }

  @JsonCreator
  public TopicDescription(
      @JsonProperty("name") String name, @JsonProperty("configs") Map<String, String> configs) {
    this.name = name;
    this.configs = configs;
  }

  @Override
  public String toString() {
    return "TopicDescription{" + "name='" + name + '\'' + ", configs=" + configs + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TopicDescription)) return false;
    TopicDescription that = (TopicDescription) o;
    return Objects.equals(name, that.name) && Objects.equals(configs, that.configs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, configs);
  }
}
