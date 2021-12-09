package com.purbon.kafka.topology.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.utils.JSON;
import java.util.Map;

public abstract class BaseAction implements Action {

  protected abstract Map<String, Object> props();

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
