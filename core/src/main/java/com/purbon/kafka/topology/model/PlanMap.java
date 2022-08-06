package com.purbon.kafka.topology.model;

import java.util.HashMap;
import java.util.Map;

public class PlanMap {

  Map<String, Plan> plans;

  public PlanMap() {
    this.plans = new HashMap<>();
  }

  public PlanMap(Map<String, Plan> plans) {
    this.plans = plans;
  }

  public boolean containsKey(String key) {
    return plans.containsKey(key);
  }

  public Plan get(String key) {
    return plans.get(key);
  }

  public int size() {
    return plans.size();
  }
}
