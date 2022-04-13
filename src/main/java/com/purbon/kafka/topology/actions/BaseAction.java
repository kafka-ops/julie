package com.purbon.kafka.topology.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.utils.JSON;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class BaseAction implements Action {

  protected abstract Map<String, Object> props();

  protected abstract List<Map<String, Object>> detailedProps();

  @Override
  public List<String> refs() {
    return detailedProps().stream()
        .map(
            map -> {
              try {
                return JSON.asPrettyString(map);
              } catch (JsonProcessingException ex) {
                return "";
              }
            })
        .collect(Collectors.toList());
  }

  @Override
  public String toString() {
    try {
      final Map<String, Object> props = props();
      return props.isEmpty() ? "" : JSON.asPrettyString(props);
    } catch (JsonProcessingException e) {
      return "";
    }
  }
}
