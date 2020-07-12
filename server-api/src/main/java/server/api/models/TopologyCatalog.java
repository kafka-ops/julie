package server.api.models;

import server.api.model.topology.Topology;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopologyCatalog {

  public List<Topology> topologies;
  public Map<String, Topology> index;

  public TopologyCatalog() {
    this.topologies = new ArrayList<>();
    this.index = new HashMap<>();
  }

  public List<Topology> getTopologies() {
    return topologies;
  }

  public void setTopologies(List<Topology> topologies) {
    this.topologies = topologies;
    topologies.forEach(topology -> addIndex(topology));
  }

  public void addIndex(Topology topology) {
    this.index.put(topology.getTeam(), topology);
  }

  public void addTopology(Topology topology) {
    topologies.add(topology);
    addIndex(topology);
  }

  public boolean exist(String team) {
    return index.keySet().contains(team);
  }

  public Topology getByTeam(String team) {
    return index.get(team);
  }
}
