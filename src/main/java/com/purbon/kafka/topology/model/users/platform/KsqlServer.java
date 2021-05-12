package com.purbon.kafka.topology.model.users.platform;

import java.util.ArrayList;
import java.util.List;

public class KsqlServer {

  private List<KsqlServerInstance> instances;

  public KsqlServer() {
    this.instances = new ArrayList<>();
  }

  public List<KsqlServerInstance> getInstances() {
    return instances;
  }
}
