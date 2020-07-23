package server.api.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

@Singleton
public class TopologyCatalog {

  public List<TopologyDeco> topologies;
  public Map<String, TopologyDeco> index;

  public TopologyCatalog() {
    this.topologies = new ArrayList<>();
    this.index = new HashMap<>();
  }

  public List<TopologyDeco> getTopologies() {
    return topologies;
  }

  public void setTopologies(List<TopologyDeco> topologies) {
    this.topologies = topologies;
    topologies.forEach(topology -> addIndex(topology));
  }

  public void addIndex(TopologyDeco topology) {
    this.index.put(topology.getTeam(), topology);
  }

  public void addTopology(TopologyDeco topology) {
    topologies.add(topology);
    addIndex(topology);
  }

  public boolean exist(String team) {
    return index.keySet().contains(team);
  }

  public TopologyDeco getByTeam(String team) {
    return index.get(team);
  }
}
