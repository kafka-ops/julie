package com.purbon.kafka.topology.model.users.platform;

import com.purbon.kafka.topology.model.User;
import java.util.ArrayList;
import java.util.List;

public class ControlCenter extends User {

  private List<ControlCenterInstance> instances;

  public ControlCenter() {
    this.instances = new ArrayList<>();
  }

  public List<ControlCenterInstance> getInstances() {
    return instances;
  }

  public void setInstances(List<ControlCenterInstance> instances) {
    this.instances = instances;
  }
}
