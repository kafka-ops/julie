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

  public Map<String, Plan> getPlans() {
    return plans;
  }
}
