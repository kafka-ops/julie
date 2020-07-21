package server.api.models;

import com.purbon.kafka.topology.model.Topology;

public class TopologyDecorator {

  private final Topology topology;

  public TopologyDecorator(Topology topology) {
    this.topology = topology;
  }


}
