package com.purbon.kafka.topology.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.utils.JSON;
import java.util.Map;

public abstract class BaseAction implements Action {

  protected abstract Map<String, Object> props();

  @Override
  public String toString() {
    try {
      Map<String, Object> props = props();
      if (props != null) {
        return JSON.asPrettyString(props);
      }
      return "";
    } catch (JsonProcessingException e) {
      return "";
    }
  }
}
