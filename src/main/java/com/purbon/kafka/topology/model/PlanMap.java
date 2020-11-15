package com.purbon.kafka.topology.model;

import java.util.ArrayList;
import java.util.List;

public class PlanMap {

  List<Plan> plans;

  public PlanMap() {
    this.plans = new ArrayList<>();
  }

  public PlanMap(List<Plan> plans) {
    this.plans = plans;
  }

  public List<Plan> getPlans() {
    return plans;
  }

}
